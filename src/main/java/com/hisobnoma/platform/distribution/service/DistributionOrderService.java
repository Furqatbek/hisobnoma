package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.dto.*;
import com.hisobnoma.platform.distribution.entity.DistributionOrder;
import com.hisobnoma.platform.distribution.entity.DistributionOrderLine;
import com.hisobnoma.platform.distribution.entity.DistributionOrderStatus;
import com.hisobnoma.platform.distribution.entity.DistributionPaymentMethod;
import com.hisobnoma.platform.distribution.mapper.DistributionOrderMapper;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.distribution.repository.DistributionOrderRepository;
import com.hisobnoma.platform.distribution.repository.DistributionRouteRepository;
import com.hisobnoma.platform.distribution.repository.DistributionVisitRepository;
import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceLineRequest;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.service.StockService;
import com.hisobnoma.platform.pos.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * B2B distribution order lifecycle: server-side pricing, fulfilment status flow,
 * best-effort stock reservation/deduction, and AR invoice conversion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DistributionOrderService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DistributionOrderRepository orderRepository;
    private final DistributionOrderMapper orderMapper;
    private final DistributionAgentRepository agentRepository;
    private final DistributionRouteRepository routeRepository;
    private final DistributionVisitRepository visitRepository;
    private final SecurityContextHelper securityContextHelper;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final LocationRepository locationRepository;
    private final PricingService pricingService;
    private final StockService stockService;
    private final DistributionStockService distributionStockService;
    private final ARInvoiceService arInvoiceService;

    // ---- reads ----

    @Transactional(readOnly = true)
    public PageResponse<DistributionOrderDto> getOrders(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<DistributionOrder> page = orderRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(orderMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<DistributionOrderDto> getOrdersByStatus(DistributionOrderStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return PageResponse.of(orderRepository.findByTenantIdAndStatus(tenantId, status, pageable).map(orderMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<DistributionOrderDto> getOrdersByAgent(Long agentId, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return PageResponse.of(orderRepository.findByTenantIdAndAgentId(tenantId, agentId, pageable).map(orderMapper::toDto));
    }

    @Transactional(readOnly = true)
    public PageResponse<DistributionOrderDto> searchOrders(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return PageResponse.of(orderRepository.searchByTenantId(tenantId, search, pageable).map(orderMapper::toDto));
    }

    @Transactional(readOnly = true)
    public DistributionOrderDto getOrder(Long id) {
        return orderMapper.toDto(loadOrder(id));
    }

    // ---- create / update ----

    @Transactional
    public DistributionOrderDto createOrder(CreateDistributionOrderRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Customer customer = requireCustomer(request.getCustomerId(), tenantId);
        validateAgent(request.getAgentId(), tenantId);
        validateSourceLocation(request.getSourceLocationId(), tenantId);
        validateVisit(request.getVisitId(), tenantId);
        validateRoute(request.getRouteId(), tenantId);

        DistributionOrder order = DistributionOrder.builder()
                .status(DistributionOrderStatus.DRAFT)
                .agentId(request.getAgentId())
                .visitId(request.getVisitId())
                .routeId(request.getRouteId())
                .customerId(customer.getId())
                .customerName(customer.getName())
                .sourceLocationId(request.getSourceLocationId())
                .priceListId(customer.getPriceListId())
                .orderDate(request.getOrderDate() != null ? request.getOrderDate() : LocalDate.now(ZoneOffset.UTC))
                .expectedDeliveryDate(request.getExpectedDeliveryDate())
                .paymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : DistributionPaymentMethod.CREDIT)
                .paymentTermsDays(request.getPaymentTermsDays() != null ? request.getPaymentTermsDays() : customer.getPaymentTermsDays())
                .discountAmount(nz(request.getDiscountAmount()))
                .taxAmount(nz(request.getTaxAmount()))
                .deliveryFee(nz(request.getDeliveryFee()))
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryLat(request.getDeliveryLat())
                .deliveryLng(request.getDeliveryLng())
                .notes(request.getNotes())
                .internalNotes(request.getInternalNotes())
                .currency(customer.getDefaultCurrency() != null ? customer.getDefaultCurrency() : "UZS")
                .tenantId(tenantId)
                .build();

        buildLines(order, request.getLines(), tenantId);
        order.recalculateTotals();
        applyDueDate(order);

        order.setOrderNumber(generateOrderNumber(tenantId));
        order = orderRepository.save(order);
        log.info("Created distribution order {} for customer {}", order.getOrderNumber(), customer.getCode());
        return orderMapper.toDto(order);
    }

    @Transactional
    public DistributionOrderDto updateOrder(Long id, UpdateDistributionOrderRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        DistributionOrder order = loadOrder(id);
        if (order.getStatus() != DistributionOrderStatus.DRAFT) {
            throw new BusinessException("Only draft orders can be edited", "ORDER_NOT_EDITABLE");
        }
        if (request.getAgentId() != null) {
            validateAgent(request.getAgentId(), tenantId);
            order.setAgentId(request.getAgentId());
        }
        if (request.getSourceLocationId() != null) {
            validateSourceLocation(request.getSourceLocationId(), tenantId);
            order.setSourceLocationId(request.getSourceLocationId());
        }
        if (request.getExpectedDeliveryDate() != null) order.setExpectedDeliveryDate(request.getExpectedDeliveryDate());
        if (request.getPaymentMethod() != null) order.setPaymentMethod(request.getPaymentMethod());
        if (request.getPaymentTermsDays() != null) order.setPaymentTermsDays(request.getPaymentTermsDays());
        if (request.getDiscountAmount() != null) order.setDiscountAmount(request.getDiscountAmount());
        if (request.getTaxAmount() != null) order.setTaxAmount(request.getTaxAmount());
        if (request.getDeliveryFee() != null) order.setDeliveryFee(request.getDeliveryFee());
        if (request.getDeliveryAddress() != null) order.setDeliveryAddress(request.getDeliveryAddress());
        if (request.getNotes() != null) order.setNotes(request.getNotes());
        if (request.getInternalNotes() != null) order.setInternalNotes(request.getInternalNotes());

        if (request.getLines() != null) {
            order.getLines().clear();
            buildLines(order, request.getLines(), tenantId);
        }
        order.recalculateTotals();
        applyDueDate(order);

        order = orderRepository.save(order);
        return orderMapper.toDto(order);
    }

    @Transactional
    public void deleteOrder(Long id) {
        DistributionOrder order = loadOrder(id);
        if (order.getStatus() != DistributionOrderStatus.DRAFT) {
            throw new BusinessException("Only draft orders can be deleted", "ORDER_NOT_DELETABLE");
        }
        orderRepository.delete(order);
    }

    // ---- status transitions ----

    @Transactional
    public DistributionOrderDto confirm(Long id) {
        DistributionOrder order = transition(id, DistributionOrderStatus.CONFIRMED);
        resolveSourceLocation(order);
        reserveStock(order);
        return orderMapper.toDto(order);
    }

    @Transactional
    public DistributionOrderDto startPicking(Long id) {
        return orderMapper.toDto(transition(id, DistributionOrderStatus.PICKING));
    }

    @Transactional
    public DistributionOrderDto markLoaded(Long id) {
        return orderMapper.toDto(transition(id, DistributionOrderStatus.LOADED));
    }

    @Transactional
    public DistributionOrderDto markInTransit(Long id) {
        return orderMapper.toDto(transition(id, DistributionOrderStatus.IN_TRANSIT));
    }

    @Transactional
    public DistributionOrderDto deliver(Long id, DeliverDistributionOrderRequest request) {
        DistributionOrder order = transition(id, DistributionOrderStatus.DELIVERED);
        order.setDeliveredAt(Instant.now());
        order.getLines().forEach(l -> l.setFulfilledQuantity(l.getQuantity()));
        applyCashSplit(order, request != null ? request.getCashCollected() : null);
        releaseReservation(order);
        deductStock(order);
        return orderMapper.toDto(order);
    }

    @Transactional
    public DistributionOrderDto invoice(Long id) {
        DistributionOrder order = transition(id, DistributionOrderStatus.INVOICED);

        // Raise a receivable only for the credit (owed) portion. A fully cash-settled order
        // (creditAmount == 0) raises no AR invoice — avoiding a phantom receivable for money
        // already collected. The invoice is created DRAFT, exactly like the web-order bridge;
        // posting it to the GL and recording the cash payment are Finance-module actions.
        if (nz(order.getCreditAmount()).signum() <= 0) {
            log.info("Distribution order {} fully cash-settled; no AR invoice raised", order.getOrderNumber());
            return orderMapper.toDto(order);
        }

        ARInvoiceDto invoice = arInvoiceService.createInvoice(buildInvoiceRequest(order));
        order.setArInvoiceId(invoice.getId());
        order.setArInvoiceNumber(invoice.getInvoiceNumber());
        log.info("Distribution order {} invoiced as {} (credit {})",
                order.getOrderNumber(), invoice.getInvoiceNumber(), order.getCreditAmount());
        return orderMapper.toDto(order);
    }

    @Transactional
    public DistributionOrderDto cancel(Long id, CancelDistributionOrderRequest request) {
        DistributionOrder order = loadOrder(id);
        if (!order.getStatus().canTransitionTo(DistributionOrderStatus.CANCELLED)) {
            throw new BusinessException(
                    "Order in status " + order.getStatus() + " cannot be cancelled", "INVALID_STATUS_TRANSITION");
        }
        boolean held = order.getStatus().holdsReservation();
        order.setStatus(DistributionOrderStatus.CANCELLED);
        order.setCancellationReason(request != null ? request.getReason() : null);
        if (held) {
            releaseReservation(order);
        }
        return orderMapper.toDto(orderRepository.save(order));
    }

    // ---- helpers ----

    private DistributionOrder loadOrder(Long id) {
        return orderRepository.findByIdAndTenantId(id, securityContextHelper.getCurrentTenantId())
                .orElseThrow(() -> new NotFoundException("DistributionOrder", id));
    }

    private DistributionOrder transition(Long id, DistributionOrderStatus target) {
        DistributionOrder order = loadOrder(id);
        if (!order.getStatus().canTransitionTo(target)) {
            throw new BusinessException(
                    "Cannot move order from " + order.getStatus() + " to " + target, "INVALID_STATUS_TRANSITION");
        }
        order.setStatus(target);
        return orderRepository.save(order);
    }

    private Customer requireCustomer(Long customerId, Long tenantId) {
        return customerRepository.findByIdAndTenantId(customerId, tenantId)
                .orElseThrow(() -> new NotFoundException("Customer", customerId));
    }

    private void validateAgent(Long agentId, Long tenantId) {
        if (agentId != null && agentRepository.findByIdAndTenantId(agentId, tenantId).isEmpty()) {
            throw new NotFoundException("DistributionAgent", agentId);
        }
    }

    /** Ensures a client-supplied source location actually belongs to the caller's tenant. */
    private void validateSourceLocation(Long locationId, Long tenantId) {
        if (locationId != null && locationRepository.findByIdAndTenantId(locationId, tenantId).isEmpty()) {
            throw new NotFoundException("Location", locationId);
        }
    }

    private void validateVisit(Long visitId, Long tenantId) {
        if (visitId != null && visitRepository.findByIdAndTenantId(visitId, tenantId).isEmpty()) {
            throw new NotFoundException("DistributionVisit", visitId);
        }
    }

    private void validateRoute(Long routeId, Long tenantId) {
        if (routeId != null && routeRepository.findByIdAndTenantId(routeId, tenantId).isEmpty()) {
            throw new NotFoundException("DistributionRoute", routeId);
        }
    }

    private void buildLines(DistributionOrder order, List<DistributionOrderLineRequest> requests, Long tenantId) {
        if (requests == null) {
            return;
        }
        for (DistributionOrderLineRequest req : requests) {
            Product product = productRepository.findByIdAndTenantId(req.getProductId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Product", req.getProductId()));
            BigDecimal unitPrice = pricingService.getProductPrice(
                    product.getId(), null, req.getQuantity(), order.getCustomerId(), order.getSourceLocationId());

            DistributionOrderLine line = DistributionOrderLine.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .unitName(product.getBaseUom() != null ? product.getBaseUom().getName() : null)
                    .quantity(req.getQuantity())
                    .unitPrice(unitPrice)
                    .discountPercent(nz(req.getDiscountPercent()))
                    .tenantId(tenantId)
                    .build();
            line.recalculate();
            order.addLine(line);
        }
    }

    private void applyDueDate(DistributionOrder order) {
        if (order.getPaymentTermsDays() != null && order.getOrderDate() != null) {
            order.setDueDate(order.getOrderDate().plusDays(order.getPaymentTermsDays()));
        }
    }

    /** Splits the total into cash-on-delivery vs AR credit based on payment method. */
    private void applyCashSplit(DistributionOrder order, BigDecimal cashCollected) {
        BigDecimal total = nz(order.getTotalAmount());
        BigDecimal cash = switch (order.getPaymentMethod()) {
            case CASH -> cashCollected != null ? cashCollected : total;
            case MIXED -> cashCollected != null ? cashCollected : BigDecimal.ZERO;
            case CREDIT -> BigDecimal.ZERO;
        };
        cash = cash.min(total).max(BigDecimal.ZERO);
        order.setCashCollected(cash);
        order.setCreditAmount(total.subtract(cash));
    }

    private CreateARInvoiceRequest buildInvoiceRequest(DistributionOrder order) {
        List<CreateARInvoiceLineRequest> lines = new ArrayList<>();
        for (DistributionOrderLine line : order.getLines()) {
            lines.add(CreateARInvoiceLineRequest.builder()
                    .productId(line.getProductId())
                    .productSku(line.getProductSku())
                    .productName(line.getProductName())
                    .description(line.getProductName() != null ? line.getProductName() : "Item")
                    .quantity(line.getQuantity())
                    .unitOfMeasure(line.getUnitName())
                    .unitPrice(line.getUnitPrice())
                    .discountPercent(nz(line.getDiscountPercent()))
                    .salesOrderLineId(line.getId())
                    .build());
        }
        // Delivery fee and tax are added as explicit lines so the invoice total matches
        // the order total (createInvoice derives the total from lines minus header discount).
        if (order.getDeliveryFee() != null && order.getDeliveryFee().signum() > 0) {
            lines.add(CreateARInvoiceLineRequest.builder()
                    .description("Yetkazib berish")
                    .quantity(BigDecimal.ONE)
                    .unitPrice(order.getDeliveryFee())
                    .build());
        }
        if (order.getTaxAmount() != null && order.getTaxAmount().signum() > 0) {
            lines.add(CreateARInvoiceLineRequest.builder()
                    .description("Soliq")
                    .quantity(BigDecimal.ONE)
                    .unitPrice(order.getTaxAmount())
                    .build());
        }

        LocalDate invoiceDate = LocalDate.now(ZoneOffset.UTC);
        return CreateARInvoiceRequest.builder()
                .customerId(order.getCustomerId())
                .salesOrderId(order.getId())
                .invoiceDate(invoiceDate)
                .dueDate(order.getDueDate() != null ? order.getDueDate() : invoiceDate.plusDays(30))
                .discountAmount(nz(order.getDiscountAmount()))
                .totalAmount(nz(order.getTotalAmount()))
                .currency(order.getCurrency())
                .paymentTerms(order.getPaymentTermsDays())
                .description("Distribution buyurtma " + order.getOrderNumber())
                .lines(lines)
                .build();
    }

    private void resolveSourceLocation(DistributionOrder order) {
        if (order.getSourceLocationId() == null) {
            locationRepository.findByTenantIdAndIsDefaultTrue(securityContextHelper.getCurrentTenantId())
                    .ifPresent(loc -> order.setSourceLocationId(loc.getId()));
        }
    }

    /** Best-effort stock reservation (mirrors the web-order convention). */
    private void reserveStock(DistributionOrder order) {
        Long locationId = order.getSourceLocationId();
        if (locationId == null) {
            log.warn("Order {}: no source location, skipping stock reservation", order.getOrderNumber());
            return;
        }
        for (DistributionOrderLine line : order.getLines()) {
            try {
                stockService.reserveStock(line.getProductId(), locationId, line.getQuantity(),
                        MovementReferenceType.DISTRIBUTION_ORDER, order.getId(), order.getOrderNumber());
            } catch (Exception e) {
                log.warn("Order {}: failed to reserve product {}: {}",
                        order.getOrderNumber(), line.getProductId(), e.getMessage());
            }
        }
    }

    private void releaseReservation(DistributionOrder order) {
        Long locationId = order.getSourceLocationId();
        if (locationId == null) {
            return;
        }
        for (DistributionOrderLine line : order.getLines()) {
            try {
                stockService.releaseReservation(line.getProductId(), locationId,
                        MovementReferenceType.DISTRIBUTION_ORDER, order.getId());
            } catch (Exception e) {
                log.warn("Order {}: failed to release reservation for product {}: {}",
                        order.getOrderNumber(), line.getProductId(), e.getMessage());
            }
        }
    }

    private void deductStock(DistributionOrder order) {
        Long locationId = order.getSourceLocationId();
        if (locationId == null) {
            log.warn("Order {}: no source location, skipping stock deduction", order.getOrderNumber());
            return;
        }
        for (DistributionOrderLine line : order.getLines()) {
            try {
                // Isolated (REQUIRES_NEW) so a deduct failure can't roll back the delivery.
                distributionStockService.deduct(line.getProductId(), locationId, line.getQuantity(),
                        order.getId(), order.getOrderNumber());
            } catch (Exception e) {
                log.warn("Order {}: failed to deduct product {}: {}",
                        order.getOrderNumber(), line.getProductId(), e.getMessage());
            }
        }
    }

    private String generateOrderNumber(Long tenantId) {
        String prefix = "DO" + LocalDate.now(ZoneOffset.UTC).format(DATE) + "-";
        String max = orderRepository.findMaxOrderNumberByPrefix(tenantId, prefix);
        int next = 1;
        if (max != null) {
            next = Integer.parseInt(max.substring(max.lastIndexOf('-') + 1)) + 1;
        }
        String number = String.format("%s%05d", prefix, next);
        while (orderRepository.existsByTenantIdAndOrderNumber(tenantId, number)) {
            number = String.format("%s%05d", prefix, ++next);
        }
        return number;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
