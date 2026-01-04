package com.hisobnoma.platform.pos.controller;

import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.PricingService;
import com.hisobnoma.platform.pos.service.PromotionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/pos/pricing")
@RequiredArgsConstructor
@Tag(name = "Pricing", description = "Pricing calculation and coupon application APIs")
public class PricingController {

    private final PricingService pricingService;
    private final PromotionService promotionService;

    @PostMapping("/calculate")
    @Operation(summary = "Calculate prices",
            description = "Calculates prices for a set of items based on price lists, customer pricing, and promotions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<PriceCalculationResult> calculatePrices(
            @Valid @RequestBody PriceCalculationRequest request) {
        return ResponseEntity.ok(pricingService.calculatePrices(request));
    }

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get product price",
            description = "Gets the best price for a single product based on customer and location")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<BigDecimal> getProductPrice(
            @Parameter(description = "Product ID") @PathVariable Long productId,
            @Parameter(description = "Variant ID (optional)") @RequestParam(required = false) Long variantId,
            @Parameter(description = "Quantity") @RequestParam(defaultValue = "1") BigDecimal quantity,
            @Parameter(description = "Customer ID (optional)") @RequestParam(required = false) Long customerId,
            @Parameter(description = "Location ID (optional)") @RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(
                pricingService.getProductPrice(productId, variantId, quantity, customerId, locationId));
    }

    @PostMapping("/apply-coupon")
    @Operation(summary = "Apply coupon",
            description = "Validates and applies a coupon code to get the discount amount")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<ApplyCouponResponse> applyCoupon(
            @Valid @RequestBody ApplyCouponRequest request) {
        PriceCalculationResult.CouponApplication result = promotionService.applyCoupon(
                request.getCouponCode(), request.getOrderTotal(), request.getCustomerId());

        ApplyCouponResponse response = ApplyCouponResponse.builder()
                .couponCode(request.getCouponCode())
                .valid(result.isValid())
                .discountAmount(result.getDiscountAmount())
                .discountDescription(result.getDiscountDescription())
                .errorMessage(result.getErrorMessage())
                .promotionId(result.getPromotionId())
                .promotionName(result.getPromotionName())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-coupon")
    @Operation(summary = "Validate coupon",
            description = "Validates a coupon code without applying it")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<ApplyCouponResponse> validateCoupon(
            @RequestParam String couponCode,
            @Parameter(description = "Customer ID (optional)") @RequestParam(required = false) Long customerId) {
        PriceCalculationResult.CouponApplication result = promotionService.applyCoupon(
                couponCode, BigDecimal.ZERO, customerId);

        ApplyCouponResponse response = ApplyCouponResponse.builder()
                .couponCode(couponCode)
                .valid(result.isValid())
                .discountAmount(BigDecimal.ZERO)
                .discountDescription(result.getDiscountDescription())
                .errorMessage(result.getErrorMessage())
                .promotionId(result.getPromotionId())
                .promotionName(result.getPromotionName())
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/record-coupon-redemption")
    @Operation(summary = "Record coupon redemption",
            description = "Records a coupon redemption after a successful transaction")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<Void> recordCouponRedemption(
            @RequestParam String couponCode,
            @RequestParam Long customerId,
            @RequestParam Long orderId,
            @RequestParam BigDecimal discountApplied) {
        promotionService.recordCouponRedemption(couponCode, customerId, orderId, discountApplied);
        return ResponseEntity.ok().build();
    }
}
