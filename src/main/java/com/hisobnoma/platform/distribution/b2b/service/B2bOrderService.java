package com.hisobnoma.platform.distribution.b2b.service;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.b2b.dto.B2bPlaceOrderRequest;
import com.hisobnoma.platform.distribution.dto.DistributionOrderDto;
import com.hisobnoma.platform.distribution.entity.DistributionOrder;
import com.hisobnoma.platform.distribution.entity.DistributionOrderLine;
import com.hisobnoma.platform.distribution.entity.DistributionOrderStatus;
import com.hisobnoma.platform.distribution.entity.DistributionPaymentMethod;
import com.hisobnoma.platform.distribution.mapper.DistributionOrderMapper;
import com.hisobnoma.platform.distribution.repository.DistributionOrderRepository;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.pos.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Self-service order placement for B2B buyers. Creates a DRAFT {@link DistributionOrder}
 * (same table/number sequence as staff-created orders) which staff then confirm and fulfil.
 * Prices are resolved server-side against the buyer's price list; the client only sends
 * product + quantity.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class B2bOrderService {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DistributionOrderRepository orderRepository;
    private final DistributionOrderMapper orderMapper;
    private final ProductRepository productRepository;
    private final PricingService pricingService;

    @Transactional
    public DistributionOrderDto placeOrder(Customer buyer, B2bPlaceOrderRequest request) {
        Long tenantId = buyer.getTenantId();
        if (buyer.isCreditHold()) {
            throw new BusinessException("Account is on credit hold; please contact your distributor", "CREDIT_HOLD");
        }

        DistributionOrder order = DistributionOrder.builder()
                .status(DistributionOrderStatus.DRAFT)
                .customerId(buyer.getId())
                .customerName(buyer.getName())
                .priceListId(buyer.getPriceListId())
                .orderDate(LocalDate.now(ZoneOffset.UTC))
                .paymentMethod(DistributionPaymentMethod.CREDIT)
                .paymentTermsDays(buyer.getPaymentTermsDays())
                .deliveryAddress(request.getDeliveryAddress())
                .notes(request.getNotes())
                .currency(buyer.getDefaultCurrency() != null ? buyer.getDefaultCurrency() : "UZS")
                .tenantId(tenantId)
                .build();

        for (B2bPlaceOrderRequest.Line lr : request.getLines()) {
            Product product = productRepository.findByIdAndTenantId(lr.getProductId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Product", lr.getProductId()));
            if (!product.isActive() || !product.isSellable()) {
                throw new BusinessException("Product " + lr.getProductId() + " is not available", "PRODUCT_UNAVAILABLE");
            }
            BigDecimal unitPrice = pricingService.getProductPrice(
                    product.getId(), null, lr.getQuantity(), buyer.getId(), null, tenantId);
            DistributionOrderLine line = DistributionOrderLine.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .unitName(product.getBaseUom() != null ? product.getBaseUom().getName() : null)
                    .quantity(lr.getQuantity())
                    .unitPrice(unitPrice)
                    .tenantId(tenantId)
                    .build();
            line.recalculate();
            order.addLine(line);
        }
        order.recalculateTotals();
        if (order.getPaymentTermsDays() != null) {
            order.setDueDate(order.getOrderDate().plusDays(order.getPaymentTermsDays()));
        }
        order.setOrderNumber(generateOrderNumber(tenantId));

        order = orderRepository.save(order);
        log.info("B2B order {} placed by customer {} ({} lines)",
                order.getOrderNumber(), buyer.getCode(), order.getLines().size());
        return orderMapper.toDto(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<DistributionOrderDto> listOrders(Customer buyer, Pageable pageable) {
        return PageResponse.of(orderRepository
                .findByTenantIdAndCustomerId(buyer.getTenantId(), buyer.getId(), pageable)
                .map(orderMapper::toDto));
    }

    @Transactional(readOnly = true)
    public DistributionOrderDto getOrder(Customer buyer, String orderNumber) {
        DistributionOrder order = orderRepository.findByTenantIdAndOrderNumber(buyer.getTenantId(), orderNumber)
                .filter(o -> buyer.getId().equals(o.getCustomerId()))
                .orElseThrow(() -> new NotFoundException("DistributionOrder", "orderNumber", orderNumber));
        return orderMapper.toDto(order);
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
}
