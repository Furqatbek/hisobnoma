package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import com.hisobnoma.platform.delivery.repository.DeliveryRegionRepository;
import com.hisobnoma.platform.delivery.repository.DeliveryVillageRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.mobile.entity.MobileAlert;
import com.hisobnoma.platform.mobile.push.apns.ApnsPayload;
import com.hisobnoma.platform.mobile.push.service.PushSendService;
import com.hisobnoma.platform.mobile.service.MobileAlertService;
import com.hisobnoma.platform.telegram.service.TelegramNotificationService;
import com.hisobnoma.platform.web.dto.CheckoutRequest;
import com.hisobnoma.platform.web.dto.PublicOrderDto;
import com.hisobnoma.platform.web.dto.PublicRegionDto;
import com.hisobnoma.platform.web.dto.PublicVillageDto;
import com.hisobnoma.platform.web.entity.*;
import com.hisobnoma.platform.web.exception.TooManyRequestsException;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Anonymous checkout and order-status lookup for the online shop.
 * Tenant comes from the X-Tenant-ID header via TenantContext and is required
 * (fail closed) — a request without it is rejected, never attributed to tenant 1.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebOrderPublicService {

    private final WebOrderRepository orderRepository;
    private final WebCatalogItemRepository catalogRepository;
    private final DeliveryRegionRepository regionRepository;
    private final DeliveryVillageRepository villageRepository;
    private final CheckoutRateLimiter rateLimiter;
    private final TelegramNotificationService telegramNotificationService;
    private final PushSendService pushSendService;
    private final MobileAlertService mobileAlertService;
    private final WebPricingService pricingService;
    private final WebCouponService couponService;
    private final WebLoyaltyService loyaltyService;
    private final WebCustomerRepository webCustomerRepository;

    @Transactional
    public PublicOrderDto checkout(CheckoutRequest request, String sourceIp, String userAgent) {
        Long tenantId = resolveTenantId();

        if (!rateLimiter.tryAcquire(sourceIp + "|" + normalizePhone(request.getPhone()))) {
            throw new TooManyRequestsException("Too many checkout attempts, please try again later");
        }

        // Payment is cash-on-delivery only for now. Any payment method the client sends is ignored
        // so no order is stranded in PENDING awaiting an online-payment flow that isn't wired.
        WebOrder order = WebOrder.builder()
                .tenantId(tenantId)
                .orderNumber(generateOrderNumber(tenantId))
                .status(WebOrderStatus.NEW)
                .customerName(request.getCustomerName().trim())
                .phone(request.getPhone().trim())
                .phoneNormalized(normalizePhone(request.getPhone()))
                .customerNote(request.getNote())
                .deliveryAddress(request.getAddress() != null ? request.getAddress().trim() : null)
                .paymentMethod(WebPaymentMethod.CASH)
                .paymentStatus(WebPaymentStatus.NONE)
                .sourceIp(sourceIp)
                .userAgent(userAgent != null && userAgent.length() > 500
                        ? userAgent.substring(0, 500) : userAgent)
                .build();

        applyDelivery(order, request, tenantId);

        List<WebPricingService.PricedLine> pricedLines = new ArrayList<>();
        for (CheckoutRequest.CheckoutLine lineRequest : request.getLines()) {
            WebCatalogItem catalogItem = catalogRepository
                    .findByIdAndTenantId(lineRequest.getCatalogItemId(), tenantId)
                    .filter(i -> i.getStatus() == WebCatalogStatus.LIVE)
                    .filter(i -> i.getProduct().isActive() && i.getProduct().isSellable())
                    .orElseThrow(() -> new ValidationException(
                            "Product is not available: " + lineRequest.getCatalogItemId()));

            Product product = catalogItem.getProduct();
            // Server-side price snapshot — any price sent by the client is ignored.
            BigDecimal unitPrice = catalogItem.getEffectivePrice();
            BigDecimal lineTotal = unitPrice.multiply(lineRequest.getQuantity());

            order.addLine(WebOrderLine.builder()
                    .tenantId(tenantId)
                    .productId(product.getId())
                    .catalogItemId(catalogItem.getId())
                    .productName(catalogItem.getEffectiveName())
                    .unitName(product.getBaseUom() != null ? product.getBaseUom().getName() : null)
                    .quantity(lineRequest.getQuantity())
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .build());
            pricedLines.add(new WebPricingService.PricedLine(catalogItem.getId(), product.getId(),
                    catalogItem.getEffectiveName(), lineRequest.getQuantity(), unitPrice, lineTotal));
        }

        applyPromotions(order, pricedLines, tenantId, request.getPhone());
        applyCoupon(order, request, tenantId);
        order.recalculateTotal();
        WebOrder saved = orderRepository.save(order);
        applyLoyaltyPoints(saved, request, tenantId);
        log.info("Web order {} created for tenant {} ({} lines, total {})",
                saved.getOrderNumber(), tenantId, saved.getLines().size(), saved.getTotalAmount());

        notifyStaff(saved);
        return toPublicDto(saved);
    }

    @Transactional(readOnly = true)
    public PublicOrderDto getOrderStatus(String orderNumber, String phone) {
        Long tenantId = resolveTenantId();
        WebOrder order = orderRepository.findByTenantIdAndOrderNumber(tenantId, orderNumber)
                .filter(o -> phoneMatches(o.getPhone(), phone))
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderNumber));
        return toPublicDto(order);
    }

    @Transactional(readOnly = true)
    public List<PublicRegionDto> getRegions() {
        Long tenantId = resolveTenantId();
        return regionRepository.findActiveByTenantId(tenantId).stream()
                .map(this::toRegionDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PublicVillageDto> getVillages(Long regionId) {
        Long tenantId = resolveTenantId();
        List<DeliveryVillage> villages = regionId != null
                ? villageRepository.findActiveByRegionIdAndTenantId(regionId, tenantId)
                : villageRepository.findActiveByTenantId(tenantId);
        return villages.stream().map(this::toVillageDto).toList();
    }

    // ---- internals ----

    private Long resolveTenantId() {
        Long tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            // Fail closed: never silently write/charge tenant 1. The storefront must identify the
            // tenant via X-Tenant-ID (or an authenticated customer token, from which TenantFilter
            // derives it).
            throw new BusinessException("X-Tenant-ID header is required", "TENANT_REQUIRED");
        }
        return tenantId;
    }

    /**
     * Applies WEB-channel promotions through the shared engine. A failure in
     * the promotion engine must never block checkout — the customer simply
     * pays the undiscounted price and the error is logged.
     */
    private void applyPromotions(WebOrder order, List<WebPricingService.PricedLine> pricedLines,
                                 Long tenantId, String phone) {
        try {
            WebPricingService.CartPrice price = pricingService.priceResolved(pricedLines, tenantId, phone);
            order.setDiscountTotal(price.discountTotal());
            order.setAppliedPromotions(price.joinedPromotionCodes());
        } catch (Exception e) {
            log.warn("Promotion application failed for order {} (tenant {}): {}",
                    order.getOrderNumber(), tenantId, e.getMessage());
            order.setDiscountTotal(BigDecimal.ZERO);
            order.setAppliedPromotions(null);
        }
    }

    /**
     * Validates and snapshots the coupon. Unlike promotion-engine failures,
     * an invalid coupon rejects the checkout — silently dropping a discount
     * the customer typed in would be a nasty surprise on the bill.
     */
    private void applyCoupon(WebOrder order, CheckoutRequest request, Long tenantId) {
        if (request.getCouponCode() == null || request.getCouponCode().isBlank()) {
            return;
        }
        BigDecimal goodsAfterPromotions = order.getLines().stream()
                .map(WebOrderLine::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .subtract(order.getDiscountTotal() != null ? order.getDiscountTotal() : BigDecimal.ZERO)
                .max(BigDecimal.ZERO);

        WebCouponService.CouponOutcome outcome = couponService.validate(
                request.getCouponCode(), goodsAfterPromotions, tenantId, request.getPhone());
        if (!outcome.valid()) {
            throw new ValidationException("Coupon is invalid or expired");
        }
        order.setCouponCode(request.getCouponCode().trim());
        order.setCouponDiscount(outcome.discount());
    }

    private void applyLoyaltyPoints(WebOrder order, CheckoutRequest request, Long tenantId) {
        if (request.getPointsToSpend() == null || request.getPointsToSpend().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        try {
            String phone = normalizePhone(request.getPhone());
            WebCustomer customer = webCustomerRepository.findByTenantIdAndPhone(tenantId, phone)
                    .orElse(null);
            if (customer == null) {
                log.debug("Loyalty: no web customer for phone {} — points skipped", phone);
                return;
            }
            BigDecimal goodsTotal = order.getLines().stream()
                    .map(WebOrderLine::getLineTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .subtract(order.getDiscountTotal() != null ? order.getDiscountTotal() : BigDecimal.ZERO)
                    .subtract(order.getCouponDiscount() != null ? order.getCouponDiscount() : BigDecimal.ZERO)
                    .max(BigDecimal.ZERO);

            BigDecimal spent = loyaltyService.spend(tenantId, customer.getId(), order.getId(),
                    request.getPointsToSpend(), goodsTotal);
            if (spent.compareTo(BigDecimal.ZERO) > 0) {
                order.setPointsSpent(spent);
                order.recalculateTotal();
                orderRepository.save(order);
            }
        } catch (Exception e) {
            log.warn("Loyalty: failed to spend points for order {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    private void applyDelivery(WebOrder order, CheckoutRequest request, Long tenantId) {
        if (request.getRegionId() != null) {
            DeliveryRegion region = regionRepository.findByIdAndTenantId(request.getRegionId(), tenantId)
                    .orElseThrow(() -> new ValidationException("Unknown delivery region: " + request.getRegionId()));
            order.setDeliveryRegionId(region.getId());
            order.setDeliveryRegionName(region.getName());
            order.setDeliveryFee(region.getDeliveryFee() != null
                    ? region.getDeliveryFee() : BigDecimal.ZERO);
        }
        if (request.getVillageId() != null) {
            DeliveryVillage village = villageRepository.findByIdAndTenantId(request.getVillageId(), tenantId)
                    .orElseThrow(() -> new ValidationException("Unknown delivery village: " + request.getVillageId()));
            order.setDeliveryVillageId(village.getId());
            order.setDeliveryVillageName(village.getName());
        }
    }

    private String generateOrderNumber(Long tenantId) {
        long next = orderRepository.countByTenantIdAndCreatedAtAfter(tenantId, java.time.Instant.EPOCH) + 1;
        String number = String.format("WO-%06d", next);
        while (orderRepository.existsByTenantIdAndOrderNumber(tenantId, number)) {
            number = String.format("WO-%06d", ++next);
        }
        return number;
    }

    /**
     * Notifies the tenant's staff of a new online order over three isolated channels: a durable
     * in-app alert for every active staff member (survives a missed push), a Telegram broadcast (to
     * staff who linked Telegram), and an APNs push to every registered admin device. A failure in
     * any one channel must never break checkout or suppress the others.
     */
    private void notifyStaff(WebOrder order) {
        String title = "Янги онлайн буюртма";
        String total = order.getTotalAmount().stripTrailingZeros().toPlainString();
        String message = String.format("%s — %s%nТелефон: %s%nСумма: %s %s",
                order.getOrderNumber(), order.getCustomerName(),
                order.getPhone(), total, order.getCurrency());

        try {
            // Durable feed entry per active staff user, in its own transaction (REQUIRES_NEW).
            mobileAlertService.createStaffBroadcast(order.getTenantId(),
                    MobileAlert.AlertType.ORDER_PLACED, title, message,
                    MobileAlert.AlertPriority.HIGH, "WEB_ORDER", order.getId());
        } catch (Exception e) {
            log.warn("Failed to create in-app new-order alerts for {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }

        try {
            telegramNotificationService.sendBroadcastAlert(order.getTenantId(),
                    MobileAlert.AlertType.ORDER_PLACED, title, message);
        } catch (Exception e) {
            log.warn("Failed to send Telegram new-order alert for {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }

        try {
            String body = String.format("%s · %s · %s %s",
                    order.getOrderNumber(), order.getCustomerName(), total, order.getCurrency());
            // type/id let the admin app deep-link to the order; push is async and self-isolating.
            ApnsPayload payload = new ApnsPayload(title, body, null, "new_order", order.getId(), null);
            pushSendService.notifyTenant(order.getTenantId(), payload);
        } catch (Exception e) {
            log.warn("Failed to send APNs new-order push for {}: {}",
                    order.getOrderNumber(), e.getMessage());
        }
    }

    static String normalizePhone(String phone) {
        return phone == null ? "" : phone.replaceAll("[^0-9]", "");
    }

    static boolean phoneMatches(String stored, String supplied) {
        String a = normalizePhone(stored);
        String b = normalizePhone(supplied);
        if (a.isEmpty() || b.length() < 7) {
            return false;
        }
        return a.equals(b) || a.endsWith(b) || b.endsWith(a);
    }

    private PublicOrderDto toPublicDto(WebOrder order) {
        return PublicOrderDto.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus().name())
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .address(order.getDeliveryAddress())
                .deliveryFee(order.getDeliveryFee())
                .discountTotal(order.getDiscountTotal())
                .couponCode(order.getCouponCode())
                .couponDiscount(order.getCouponDiscount())
                .pointsSpent(order.getPointsSpent())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .createdAt(order.getCreatedAt())
                .lines(order.getLines().stream()
                        .map(l -> PublicOrderDto.Line.builder()
                                .productName(l.getProductName())
                                .quantity(l.getQuantity())
                                .unitPrice(l.getUnitPrice())
                                .lineTotal(l.getLineTotal())
                                .build())
                        .toList())
                .build();
    }

    private PublicRegionDto toRegionDto(DeliveryRegion region) {
        return PublicRegionDto.builder()
                .id(region.getId())
                .name(region.getName())
                .deliveryFee(region.getDeliveryFee())
                .build();
    }

    private PublicVillageDto toVillageDto(DeliveryVillage village) {
        return PublicVillageDto.builder()
                .id(village.getId())
                .name(village.getName())
                .regionId(village.getRegion() != null ? village.getRegion().getId() : null)
                .build();
    }
}
