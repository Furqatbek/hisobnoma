package com.hisobnoma.platform.pos.controller;

import com.hisobnoma.platform.pos.dto.CouponDto;
import com.hisobnoma.platform.pos.dto.CreateCouponRequest;
import com.hisobnoma.platform.pos.entity.CouponRedemption;
import com.hisobnoma.platform.pos.enums.CouponStatus;
import com.hisobnoma.platform.pos.service.CouponService;
import com.hisobnoma.platform.auth.security.RequiresPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/coupons")
@RequiredArgsConstructor
@Tag(name = "Coupons", description = "Coupon management APIs")
public class CouponController {

    private final CouponService couponService;

    @GetMapping
    @Operation(summary = "Get all coupons", description = "Retrieves all coupons with pagination")
    @RequiresPermission("POS_COUPON_READ")
    public ResponseEntity<Page<CouponDto>> getAllCoupons(Pageable pageable) {
        return ResponseEntity.ok(couponService.findAllCoupons(pageable));
    }

    @GetMapping("/promotion/{promotionId}")
    @Operation(summary = "Get coupons by promotion", description = "Retrieves coupons for a specific promotion")
    @RequiresPermission("POS_COUPON_READ")
    public ResponseEntity<Page<CouponDto>> getCouponsByPromotion(
            @Parameter(description = "Promotion ID") @PathVariable Long promotionId,
            Pageable pageable) {
        return ResponseEntity.ok(couponService.findCouponsByPromotion(promotionId, pageable));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get coupons by status", description = "Retrieves coupons with a specific status")
    @RequiresPermission("POS_COUPON_READ")
    public ResponseEntity<Page<CouponDto>> getCouponsByStatus(
            @Parameter(description = "Coupon status") @PathVariable CouponStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(couponService.findCouponsByStatus(status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coupon by ID", description = "Retrieves a coupon by its ID")
    @RequiresPermission("POS_COUPON_READ")
    public ResponseEntity<CouponDto> getCouponById(
            @Parameter(description = "Coupon ID") @PathVariable Long id) {
        return ResponseEntity.ok(couponService.findCouponById(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get coupon by code", description = "Retrieves a coupon by its code")
    @RequiresPermission("POS_COUPON_READ")
    public ResponseEntity<CouponDto> getCouponByCode(
            @Parameter(description = "Coupon code") @PathVariable String code) {
        return ResponseEntity.ok(couponService.findCouponByCode(code));
    }

    @PostMapping
    @Operation(summary = "Create coupon", description = "Creates a new coupon")
    @RequiresPermission("POS_COUPON_CREATE")
    public ResponseEntity<CouponDto> createCoupon(
            @Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(couponService.createCoupon(request));
    }

    @PostMapping("/generate/{promotionId}")
    @Operation(summary = "Generate bulk coupons", description = "Generates multiple coupons for a promotion")
    @RequiresPermission("POS_COUPON_GENERATE")
    public ResponseEntity<List<CouponDto>> generateCoupons(
            @Parameter(description = "Promotion ID") @PathVariable Long promotionId,
            @Parameter(description = "Number of coupons to generate") @RequestParam int count,
            @Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(couponService.generateCoupons(promotionId, count, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update coupon", description = "Updates an existing coupon")
    @RequiresPermission("POS_COUPON_UPDATE")
    public ResponseEntity<CouponDto> updateCoupon(
            @Parameter(description = "Coupon ID") @PathVariable Long id,
            @Valid @RequestBody CreateCouponRequest request) {
        return ResponseEntity.ok(couponService.updateCoupon(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete coupon", description = "Deletes a coupon (only if unused)")
    @RequiresPermission("POS_COUPON_DELETE")
    public ResponseEntity<Void> deleteCoupon(
            @Parameter(description = "Coupon ID") @PathVariable Long id) {
        couponService.deleteCoupon(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate coupon", description = "Activates a coupon")
    @RequiresPermission("POS_COUPON_UPDATE")
    public ResponseEntity<CouponDto> activateCoupon(
            @Parameter(description = "Coupon ID") @PathVariable Long id) {
        return ResponseEntity.ok(couponService.activateCoupon(id));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate coupon", description = "Deactivates a coupon")
    @RequiresPermission("POS_COUPON_UPDATE")
    public ResponseEntity<CouponDto> deactivateCoupon(
            @Parameter(description = "Coupon ID") @PathVariable Long id) {
        return ResponseEntity.ok(couponService.deactivateCoupon(id));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel coupon", description = "Cancels a coupon")
    @RequiresPermission("POS_COUPON_UPDATE")
    public ResponseEntity<CouponDto> cancelCoupon(
            @Parameter(description = "Coupon ID") @PathVariable Long id) {
        return ResponseEntity.ok(couponService.cancelCoupon(id));
    }

    @GetMapping("/{id}/redemptions")
    @Operation(summary = "Get coupon redemptions", description = "Retrieves redemption history for a coupon")
    @RequiresPermission("POS_COUPON_REDEMPTIONS_VIEW")
    public ResponseEntity<List<CouponRedemption>> getCouponRedemptions(
            @Parameter(description = "Coupon ID") @PathVariable Long id) {
        return ResponseEntity.ok(couponService.getCouponRedemptions(id));
    }

    @PostMapping("/update-expired")
    @Operation(summary = "Update expired coupons", description = "Marks expired coupons as EXPIRED status")
    @RequiresPermission("POS_COUPON_UPDATE")
    public ResponseEntity<Void> updateExpiredCoupons() {
        couponService.updateExpiredCoupons();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-depleted")
    @Operation(summary = "Update depleted coupons", description = "Marks fully used coupons as DEPLETED status")
    @RequiresPermission("POS_COUPON_UPDATE")
    public ResponseEntity<Void> updateDepletedCoupons() {
        couponService.updateDepletedCoupons();
        return ResponseEntity.ok().build();
    }
}
