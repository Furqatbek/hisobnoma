package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceLineRequest;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.dto.CreateCustomerRequest;
import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.dto.CustomerDto;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
import com.hisobnoma.platform.finance.service.CustomerService;
import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
import com.hisobnoma.platform.inventory.entity.Stock;
import com.hisobnoma.platform.inventory.entity.StockReservation;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.inventory.repository.StockReservationRepository;
import com.hisobnoma.platform.inventory.service.StockService;
import com.hisobnoma.platform.web.dto.UpdateOrderStatusRequest;
import com.hisobnoma.platform.web.dto.WebOrderDto;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderLine;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.entity.WebPaymentStatus;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Staff management of online orders: inbox, status transitions and
 * conversion to AR invoices (debt) via the existing finance services.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebOrderService {

    private final WebOrderRepository orderRepository;
    private final SecurityContextHelper securityContextHelper;
    private final CustomerService customerService;
    private final ARInvoiceService arInvoiceService;
    private final com.hisobnoma.platform.web.repository.WebCustomerRepository webCustomerRepository;
    private final StockRepository stockRepository;
    private final StockReservationRepository stockReservationRepository;
    private final StockService stockService;
    private final com.hisobnoma.platform.pos.repository.PromotionRepository promotionRepository;
    private final com.hisobnoma.platform.pos.service.PromotionService promotionService;
    private final WebLoyaltyService loyaltyService;
    private final WebPushService pushService;
    private final WebReferralService referralService;
    private final com.hisobnoma.platform.web.repository.WebPaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Page<WebOrderDto> getOrders(WebOrderStatus status, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<WebOrder> page = status != null
                ? orderRepository.findByTenantAndStatus(tenantId, status, pageable)
                : orderRepository.findAllByTenant(tenantId, pageable);
        return page.map(this::toDto);
    }

    @Transactional(readOnly = true)
    public WebOrderDto getOrder(Long id) {
        return toDto(getOrderEntity(id));
    }

    @Transactional(readOnly = true)
    public long getNewOrderCount() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return orderRepository.countByTenantIdAndStatus(tenantId, WebOrderStatus.NEW);
    }

    @Transactional
    public WebOrderDto updateStatus(Long id, UpdateOrderStatusRequest request) {
        WebOrder order = getOrderEntity(id);
        WebOrderStatus target = request.getStatus();

        if (!order.getStatus().canTransitionTo(target)) {
            throw new ValidationException(String.format(
                    "Cannot change order %s from %s to %s",
                    order.getOrderNumber(), order.getStatus(), target));
        }
        if (target == WebOrderStatus.CANCELLED) {
            if (request.getReason() == null || request.getReason().isBlank()) {
                throw new ValidationException("Cancellation reason is required");
            }
            order.setCancellationReason(request.getReason().trim());
        }

        WebOrderStatus previous = order.getStatus();
        order.setStatus(target);

        if (target == WebOrderStatus.CONFIRMED) {
            reserveStock(order);
            adjustPromotionUsage(order, true);
            recordCouponRedemption(order);
        } else if (previous != WebOrderStatus.NEW
                && (target == WebOrderStatus.CANCELLED || target == WebOrderStatus.COMPLETED)) {
            releaseReservations(order);
            if (target == WebOrderStatus.COMPLETED) {
                earnLoyaltyPoints(order);
                rewardReferral(order);
            }
            if (target == WebOrderStatus.CANCELLED) {
                adjustPromotionUsage(order, false);
                reverseCouponRedemption(order, request.getReason());
                reverseLoyaltyPoints(order);
                // A previously-paid card order that is cancelled must be refunded.
                if (order.getPaymentStatus() == WebPaymentStatus.PAID) {
                    order.setPaymentStatus(WebPaymentStatus.REFUNDED);
                }
            }
        }

        log.info("Web order {} status changed to {}", order.getOrderNumber(), target);
        WebOrderDto result = toDto(orderRepository.save(order));

        notifyStatusChange(order, target);

        return result;
    }

    /**
     * Staff confirmation that an online card payment was received (interim path
     * until provider webhooks are wired). Flags the order — and its latest
     * payment attempt — as PAID. Idempotent.
     */
    @Transactional
    public WebOrderDto confirmPayment(Long id) {
        WebOrder order = getOrderEntity(id);
        if (order.getStatus() == WebOrderStatus.CANCELLED) {
            throw new ValidationException("Cancelled orders cannot be marked paid");
        }
        order.setPaymentStatus(WebPaymentStatus.PAID);
        paymentRepository
                .findTopByWebOrderIdAndTenantIdOrderByCreatedAtDesc(order.getId(), order.getTenantId())
                .filter(p -> p.getStatus() != WebPaymentStatus.PAID)
                .ifPresent(p -> {
                    p.setStatus(WebPaymentStatus.PAID);
                    p.setPaidAt(java.time.Instant.now());
                    paymentRepository.save(p);
                });
        log.info("Web order {} payment confirmed by staff", order.getOrderNumber());
        return toDto(orderRepository.save(order));
    }

    /**
     * Converts the order into an AR invoice (debt). Creates an AR customer
     * from the order's name/phone when the order isn't linked to one yet.
     */
    @Transactional
    public WebOrderDto convertToInvoice(Long id) {
        WebOrder order = getOrderEntity(id);

        if (order.getStatus() == WebOrderStatus.CANCELLED) {
            throw new ValidationException("Cancelled orders cannot be converted to an invoice");
        }
        if (order.getArInvoiceId() != null) {
            throw new ValidationException(
                    "Order is already converted to invoice " + order.getArInvoiceNumber());
        }

        Long customerId = order.getCustomerId();
        if (customerId == null) {
            // A web customer account linked by staff to an AR customer wins
            // over creating a brand-new customer record.
            customerId = findLinkedCustomerId(order);
        }
        if (customerId == null) {
            CustomerDto customer = customerService.createCustomer(CreateCustomerRequest.builder()
                    .name(order.getCustomerName())
                    .phone(order.getPhone())
                    .address(buildAddress(order))
                    .build());
            customerId = customer.getId();
            log.info("Created customer {} from web order {}", customer.getCode(), order.getOrderNumber());
        }
        order.setCustomerId(customerId);

        List<CreateARInvoiceLineRequest> lines = new ArrayList<>(order.getLines().stream()
                .map(this::toInvoiceLine)
                .toList());

        // Delivery fee becomes an explicit invoice line so totals stay equal
        if (order.getDeliveryFee() != null && order.getDeliveryFee().compareTo(BigDecimal.ZERO) > 0) {
            lines.add(CreateARInvoiceLineRequest.builder()
                    .productName("Етказиб бериш")
                    .description("Етказиб бериш (" + (order.getDeliveryRegionName() != null
                            ? order.getDeliveryRegionName() : "-") + ")")
                    .quantity(BigDecimal.ONE)
                    .unitPrice(order.getDeliveryFee())
                    .build());
        }

        ARInvoiceDto invoice = arInvoiceService.createInvoice(CreateARInvoiceRequest.builder()
                .customerId(customerId)
                .invoiceDate(LocalDate.now())
                .dueDate(LocalDate.now().plusDays(30))
                .totalAmount(order.getTotalAmount())
                // Promotion + coupon discounts as a header discount keep line
                // snapshots at full price while invoice and order totals match
                .discountAmount(order.totalDiscounts().compareTo(BigDecimal.ZERO) > 0
                        ? order.totalDiscounts() : null)
                .currency(order.getCurrency())
                .description("Онлайн буюртма " + order.getOrderNumber())
                .lines(lines)
                .build());

        order.setArInvoiceId(invoice.getId());
        order.setArInvoiceNumber(invoice.getInvoiceNumber());
        log.info("Web order {} converted to invoice {}", order.getOrderNumber(), invoice.getInvoiceNumber());
        return toDto(orderRepository.save(order));
    }

    // ---- stock reservation ----

    /**
     * Best-effort reservation: never blocks confirmation. Lines whose product
     * has no stock rows (untracked/services) or not enough available quantity
     * are skipped with a warning — staff sees real stock in the inventory module.
     */
    private void reserveStock(WebOrder order) {
        for (WebOrderLine line : order.getLines()) {
            try {
                List<Stock> stocks = stockRepository.findByProductIdAndTenantId(
                        line.getProductId(), order.getTenantId());
                Stock best = stocks.stream()
                        .filter(s -> s.getQuantityAvailable().compareTo(line.getQuantity()) >= 0)
                        .max(java.util.Comparator.comparing(Stock::getQuantityAvailable))
                        .orElse(null);
                if (best == null) {
                    if (!stocks.isEmpty()) {
                        log.warn("Web order {}: not enough stock to reserve {} x {}",
                                order.getOrderNumber(), line.getQuantity(), line.getProductName());
                    }
                    continue;
                }
                stockService.reserveStock(line.getProductId(), best.getLocation().getId(),
                        line.getQuantity(), MovementReferenceType.WEB_ORDER,
                        order.getId(), order.getOrderNumber());
            } catch (Exception e) {
                log.warn("Web order {}: failed to reserve stock for {}: {}",
                        order.getOrderNumber(), line.getProductName(), e.getMessage());
            }
        }
    }

    private void releaseReservations(WebOrder order) {
        List<StockReservation> reservations = new ArrayList<>();
        try {
            reservations = stockReservationRepository
                    .findByReferenceTypeAndReferenceIdAndTenantId(
                            MovementReferenceType.WEB_ORDER, order.getId(), order.getTenantId());
        } catch (Exception e) {
            log.warn("Web order {}: failed to load reservations: {}",
                    order.getOrderNumber(), e.getMessage());
        }
        for (StockReservation reservation : reservations) {
            if (reservation.getStatus() != StockReservation.ReservationStatus.ACTIVE) {
                continue;
            }
            try {
                stockService.releaseReservation(
                        reservation.getProduct().getId(), reservation.getLocation().getId(),
                        MovementReferenceType.WEB_ORDER, order.getId());
            } catch (Exception e) {
                log.warn("Web order {}: failed to release reservation {}: {}",
                        order.getOrderNumber(), reservation.getId(), e.getMessage());
            }
        }
    }

    // ---- promotion usage ----

    /**
     * Best-effort usage tracking against promotion max-use limits: counted
     * when the order is confirmed, released when a confirmed order is
     * cancelled. A deleted promotion is skipped silently.
     */
    private void adjustPromotionUsage(WebOrder order, boolean increment) {
        if (order.getAppliedPromotions() == null || order.getAppliedPromotions().isBlank()) {
            return;
        }
        for (String code : order.getAppliedPromotions().split(",")) {
            String trimmed = code.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                promotionRepository.findByCodeAndTenantId(trimmed, order.getTenantId())
                        .ifPresent(promotion -> {
                            if (increment) {
                                promotion.incrementUsage();
                            } else {
                                promotion.decrementUsage();
                            }
                            promotionRepository.save(promotion);
                        });
            } catch (Exception e) {
                log.warn("Web order {}: failed to {} usage for promotion {}: {}",
                        order.getOrderNumber(), increment ? "increment" : "release", trimmed, e.getMessage());
            }
        }
    }

    // ---- coupon redemption ----

    /**
     * Records the coupon redemption when the order is confirmed. Best-effort:
     * a failure is logged but never blocks confirmation — the customer was
     * already promised the discount at checkout.
     */
    private void recordCouponRedemption(WebOrder order) {
        if (order.getCouponCode() == null || order.getCouponCode().isBlank()) {
            return;
        }
        try {
            promotionService.recordWebCouponRedemption(
                    order.getCouponCode(),
                    order.getCustomerId() != null ? order.getCustomerId() : findLinkedCustomerId(order),
                    order.getId(),
                    order.getTotalAmount(),
                    order.getCouponDiscount(),
                    order.getTenantId(),
                    securityContextHelper.getCurrentUserId());
        } catch (Exception e) {
            log.warn("Web order {}: failed to record coupon redemption for {}: {}",
                    order.getOrderNumber(), order.getCouponCode(), e.getMessage());
        }
    }

    private void reverseCouponRedemption(WebOrder order, String reason) {
        if (order.getCouponCode() == null || order.getCouponCode().isBlank()) {
            return;
        }
        try {
            promotionService.reverseWebCouponRedemption(
                    order.getId(), order.getTenantId(),
                    securityContextHelper.getCurrentUserId(), reason);
        } catch (Exception e) {
            log.warn("Web order {}: failed to reverse coupon redemption: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    // ---- push notifications ----

    private void notifyStatusChange(WebOrder order, WebOrderStatus status) {
        try {
            pushService.notifyOrderStatusChange(order, status);
        } catch (Exception e) {
            log.warn("Web order {}: push notification failed: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    // ---- referral ----

    private void rewardReferral(WebOrder order) {
        try {
            referralService.rewardIfFirstCompletion(order);
        } catch (Exception e) {
            log.warn("Web order {}: referral reward failed: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    // ---- loyalty ----

    private void earnLoyaltyPoints(WebOrder order) {
        try {
            loyaltyService.earn(order);
        } catch (Exception e) {
            log.warn("Web order {}: failed to earn loyalty points: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    private void reverseLoyaltyPoints(WebOrder order) {
        try {
            loyaltyService.reverseOrder(order);
        } catch (Exception e) {
            log.warn("Web order {}: failed to reverse loyalty points: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    // ---- internals ----

    private Long findLinkedCustomerId(WebOrder order) {
        if (order.getPhoneNormalized() == null || order.getPhoneNormalized().isBlank()) {
            return null;
        }
        return webCustomerRepository
                .findByTenantIdAndPhone(order.getTenantId(), order.getPhoneNormalized())
                .map(com.hisobnoma.platform.web.entity.WebCustomer::getCustomerId)
                .orElse(null);
    }

    private WebOrder getOrderEntity(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return orderRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Web order not found: " + id));
    }

    private CreateARInvoiceLineRequest toInvoiceLine(WebOrderLine line) {
        return CreateARInvoiceLineRequest.builder()
                .productId(line.getProductId())
                .productName(line.getProductName())
                .description(line.getProductName())
                .quantity(line.getQuantity())
                .unitOfMeasure(line.getUnitName())
                .unitPrice(line.getUnitPrice())
                .build();
    }

    private String buildAddress(WebOrder order) {
        StringBuilder sb = new StringBuilder();
        if (order.getDeliveryRegionName() != null) {
            sb.append(order.getDeliveryRegionName());
        }
        if (order.getDeliveryVillageName() != null) {
            if (!sb.isEmpty()) sb.append(", ");
            sb.append(order.getDeliveryVillageName());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private WebOrderDto toDto(WebOrder order) {
        return WebOrderDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .customerName(order.getCustomerName())
                .phone(order.getPhone())
                .deliveryRegionId(order.getDeliveryRegionId())
                .deliveryRegionName(order.getDeliveryRegionName())
                .deliveryVillageId(order.getDeliveryVillageId())
                .deliveryVillageName(order.getDeliveryVillageName())
                .customerNote(order.getCustomerNote())
                .deliveryAddress(order.getDeliveryAddress())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .deliveryFee(order.getDeliveryFee())
                .discountTotal(order.getDiscountTotal())
                .appliedPromotions(order.getAppliedPromotions())
                .couponCode(order.getCouponCode())
                .couponDiscount(order.getCouponDiscount())
                .pointsSpent(order.getPointsSpent())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .customerId(order.getCustomerId())
                .arInvoiceId(order.getArInvoiceId())
                .arInvoiceNumber(order.getArInvoiceNumber())
                .cancellationReason(order.getCancellationReason())
                .createdAt(order.getCreatedAt())
                .lines(order.getLines().stream()
                        .map(l -> WebOrderDto.WebOrderLineDto.builder()
                                .id(l.getId())
                                .productId(l.getProductId())
                                .productName(l.getProductName())
                                .unitName(l.getUnitName())
                                .quantity(l.getQuantity())
                                .unitPrice(l.getUnitPrice())
                                .lineTotal(l.getLineTotal())
                                .build())
                        .toList())
                .build();
    }
}
