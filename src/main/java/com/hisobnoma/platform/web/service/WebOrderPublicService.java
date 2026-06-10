package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import com.hisobnoma.platform.delivery.repository.DeliveryRegionRepository;
import com.hisobnoma.platform.delivery.repository.DeliveryVillageRepository;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.mobile.entity.MobileAlert;
import com.hisobnoma.platform.telegram.service.TelegramNotificationService;
import com.hisobnoma.platform.web.dto.CheckoutRequest;
import com.hisobnoma.platform.web.dto.PublicOrderDto;
import com.hisobnoma.platform.web.dto.PublicRegionDto;
import com.hisobnoma.platform.web.dto.PublicVillageDto;
import com.hisobnoma.platform.web.entity.*;
import com.hisobnoma.platform.web.exception.TooManyRequestsException;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Anonymous checkout and order-status lookup for the online shop.
 * Tenant comes from the X-Tenant-ID header via TenantContext (default 1).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebOrderPublicService {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private final WebOrderRepository orderRepository;
    private final WebCatalogItemRepository catalogRepository;
    private final DeliveryRegionRepository regionRepository;
    private final DeliveryVillageRepository villageRepository;
    private final CheckoutRateLimiter rateLimiter;
    private final TelegramNotificationService telegramNotificationService;

    @Transactional
    public PublicOrderDto checkout(CheckoutRequest request, String sourceIp, String userAgent) {
        Long tenantId = resolveTenantId();

        if (!rateLimiter.tryAcquire(sourceIp + "|" + normalizePhone(request.getPhone()))) {
            throw new TooManyRequestsException("Too many checkout attempts, please try again later");
        }

        WebOrder order = WebOrder.builder()
                .tenantId(tenantId)
                .orderNumber(generateOrderNumber(tenantId))
                .status(WebOrderStatus.NEW)
                .customerName(request.getCustomerName().trim())
                .phone(request.getPhone().trim())
                .customerNote(request.getNote())
                .sourceIp(sourceIp)
                .userAgent(userAgent != null && userAgent.length() > 500
                        ? userAgent.substring(0, 500) : userAgent)
                .build();

        applyDelivery(order, request, tenantId);

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
        }

        order.recalculateTotal();
        WebOrder saved = orderRepository.save(order);
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
        return tenantId != null ? tenantId : DEFAULT_TENANT_ID;
    }

    private void applyDelivery(WebOrder order, CheckoutRequest request, Long tenantId) {
        if (request.getRegionId() != null) {
            DeliveryRegion region = regionRepository.findByIdAndTenantId(request.getRegionId(), tenantId)
                    .orElseThrow(() -> new ValidationException("Unknown delivery region: " + request.getRegionId()));
            order.setDeliveryRegionId(region.getId());
            order.setDeliveryRegionName(region.getName());
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

    private void notifyStaff(WebOrder order) {
        try {
            String message = String.format("%s — %s%nТелефон: %s%nСумма: %s %s",
                    order.getOrderNumber(), order.getCustomerName(),
                    order.getPhone(), order.getTotalAmount().stripTrailingZeros().toPlainString(),
                    order.getCurrency());
            telegramNotificationService.sendBroadcastAlert(order.getTenantId(),
                    MobileAlert.AlertType.ORDER_PLACED, "Янги онлайн буюртма", message);
        } catch (Exception e) {
            // Notification failures must never break checkout.
            log.warn("Failed to send new-order notification for {}: {}",
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
        return PublicRegionDto.builder().id(region.getId()).name(region.getName()).build();
    }

    private PublicVillageDto toVillageDto(DeliveryVillage village) {
        return PublicVillageDto.builder()
                .id(village.getId())
                .name(village.getName())
                .regionId(village.getRegion() != null ? village.getRegion().getId() : null)
                .build();
    }
}
