package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.web.dto.CartPriceRequest;
import com.hisobnoma.platform.web.dto.PublicCartPriceDto;
import com.hisobnoma.platform.web.exception.TooManyRequestsException;
import com.hisobnoma.platform.web.security.WebCustomerTokenService;
import com.hisobnoma.platform.web.service.CheckoutRateLimiter;
import com.hisobnoma.platform.web.service.WebPricingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public cart pricing preview for the shop mobile app. The app calls this
 * (debounced) whenever the cart changes so customers see promotion discounts
 * before checkout. Checkout recomputes everything — this is display only.
 */
@RestController
@RequestMapping("/api/v1/web/cart")
@RequiredArgsConstructor
public class WebCartPublicController {

    private static final Long DEFAULT_TENANT_ID = 1L;

    private final WebPricingService pricingService;
    private final CheckoutRateLimiter rateLimiter;
    private final WebCustomerTokenService tokenService;

    @PostMapping("/price")
    public ResponseEntity<ApiResponse<PublicCartPriceDto>> price(
            @Valid @RequestBody CartPriceRequest request,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            HttpServletRequest httpRequest) {

        // Previews fire on every cart edit; the checkout limiter (5/min) is
        // too tight for that, so the key carries a 10s bucket — effectively
        // 5 calls per 10 seconds per IP.
        String bucket = String.valueOf(System.currentTimeMillis() / 10_000);
        if (!rateLimiter.tryAcquire("cart-price|" + bucket + "|" + clientIp(httpRequest))) {
            throw new TooManyRequestsException("Too many price requests, please try again later");
        }

        Long tenantId = TenantContext.getCurrentTenant() != null
                ? TenantContext.getCurrentTenant() : DEFAULT_TENANT_ID;
        String phone = phoneFromToken(authorization);

        WebPricingService.CartPrice price = pricingService.price(request.getLines(), tenantId, phone);
        return ResponseEntity.ok(ApiResponse.success(toDto(price)));
    }

    private String phoneFromToken(String authorization) {
        if (authorization == null || authorization.isBlank()) {
            return null;
        }
        String token = authorization.startsWith("Bearer ")
                ? authorization.substring(7) : authorization;
        return tokenService.parse(token)
                .map(p -> p.phone())
                .orElse(null);
    }

    private PublicCartPriceDto toDto(WebPricingService.CartPrice price) {
        return PublicCartPriceDto.builder()
                .lines(price.lines().stream()
                        .map(l -> PublicCartPriceDto.Line.builder()
                                .catalogItemId(l.catalogItemId())
                                .productName(l.productName())
                                .quantity(l.quantity())
                                .unitPrice(l.unitPrice())
                                .lineTotal(l.lineTotal())
                                .build())
                        .toList())
                .subtotal(price.subtotal())
                .discountTotal(price.discountTotal())
                .total(price.total())
                .currency("UZS")
                .appliedPromotions(price.appliedPromotions().stream()
                        .map(p -> p.getPromotionName() != null ? p.getPromotionName() : p.getPromotionCode())
                        .toList())
                .build();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
