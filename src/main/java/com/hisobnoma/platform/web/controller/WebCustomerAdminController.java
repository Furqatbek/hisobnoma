package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.web.dto.LoyaltyAdjustRequest;
import com.hisobnoma.platform.web.dto.LoyaltyBalanceDto;
import com.hisobnoma.platform.web.dto.WebCustomerDto;
import com.hisobnoma.platform.web.dto.WishlistItemDto;
import com.hisobnoma.platform.web.service.WebCustomerService;
import com.hisobnoma.platform.web.service.WebLoyaltyService;
import com.hisobnoma.platform.web.service.WebWishlistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Staff management of online (mobile app) customer accounts.
 */
@RestController
@RequestMapping("/api/v1/web-customers")
@RequiredArgsConstructor
public class WebCustomerAdminController {

    private final WebCustomerService customerService;
    private final WebLoyaltyService loyaltyService;
    private final WebWishlistService wishlistService;
    private final com.hisobnoma.platform.web.service.WebCouponIssueService couponIssueService;

    @GetMapping
    @RequiresPermission("WEB_CUSTOMER_VIEW")
    public ResponseEntity<PageResponse<WebCustomerDto>> getCustomers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<WebCustomerDto> page = customerService.getCustomers(search, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/{id}")
    @RequiresPermission("WEB_CUSTOMER_VIEW")
    public ResponseEntity<ApiResponse<WebCustomerDto>> getCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.getCustomer(id)));
    }

    @PostMapping("/{id}/link-customer")
    @RequiresPermission("WEB_CUSTOMER_MANAGE")
    public ResponseEntity<ApiResponse<WebCustomerDto>> linkCustomer(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        return ResponseEntity.ok(ApiResponse.success(
                customerService.linkCustomer(id, body.get("customerId"))));
    }

    @PostMapping("/{id}/unlink-customer")
    @RequiresPermission("WEB_CUSTOMER_MANAGE")
    public ResponseEntity<ApiResponse<WebCustomerDto>> unlinkCustomer(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(customerService.unlinkCustomer(id)));
    }

    @PostMapping("/{id}/sms-opt-out")
    @RequiresPermission("WEB_CUSTOMER_MANAGE")
    public ResponseEntity<ApiResponse<WebCustomerDto>> setSmsOptOut(
            @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        return ResponseEntity.ok(ApiResponse.success(
                customerService.setSmsOptOut(id, Boolean.TRUE.equals(body.get("optOut")))));
    }

    @GetMapping("/{id}/wishlist")
    @RequiresPermission("WEB_CUSTOMER_VIEW")
    public ResponseEntity<PageResponse<WishlistItemDto>> getCustomerWishlist(
            @PathVariable Long id,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.of(customerService.getCustomerWishlist(id, pageable)));
    }

    @GetMapping("/{id}/loyalty")
    @RequiresPermission("WEB_LOYALTY_VIEW")
    public ResponseEntity<ApiResponse<LoyaltyBalanceDto>> getLoyalty(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(loyaltyService.getCustomerLoyalty(id)));
    }

    @PostMapping("/{id}/loyalty/adjust")
    @RequiresPermission("WEB_LOYALTY_MANAGE")
    public ResponseEntity<ApiResponse<LoyaltyBalanceDto>> adjustLoyalty(
            @PathVariable Long id, @Valid @RequestBody LoyaltyAdjustRequest request) {
        return ResponseEntity.ok(ApiResponse.success(loyaltyService.adjust(id, request)));
    }

    // ---- segmentation ----

    /** Per-segment customer counts: active = ordered within activeDays; lost = quiet for lostDays+. */
    @GetMapping("/segments")
    @RequiresPermission("WEB_CUSTOMER_VIEW")
    public ResponseEntity<ApiResponse<java.util.Map<String, Long>>> segmentCounts(
            @RequestParam(defaultValue = "30") int activeDays,
            @RequestParam(defaultValue = "90") int lostDays) {
        return ResponseEntity.ok(ApiResponse.success(
                customerService.getSegmentCounts(activeDays, lostDays)));
    }

    /** Customers of one segment (param = days for the N-days segments, min amount for MIN_TOTAL_SPENT). */
    @GetMapping("/segments/{segment}/customers")
    @RequiresPermission("WEB_CUSTOMER_VIEW")
    public ResponseEntity<ApiResponse<java.util.List<WebCustomerDto>>> segmentCustomers(
            @PathVariable com.hisobnoma.platform.web.entity.WebSegmentType segment,
            @RequestParam(required = false) Integer param) {
        return ResponseEntity.ok(ApiResponse.success(
                customerService.getSegmentCustomers(segment, param)));
    }

    // ---- personal coupons ----

    @PostMapping("/{id}/coupons")
    @RequiresPermission("POS_COUPON_CREATE")
    public ResponseEntity<ApiResponse<com.hisobnoma.platform.pos.dto.CouponDto>> issueCoupon(
            @PathVariable Long id,
            @Valid @RequestBody com.hisobnoma.platform.web.dto.IssueCouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                couponIssueService.issueToCustomer(id, request), "Coupon issued"));
    }

    /** Issues one personal single-use coupon to every customer in the segment. */
    @PostMapping("/segments/{segment}/coupons")
    @RequiresPermission("POS_COUPON_CREATE")
    public ResponseEntity<ApiResponse<java.util.Map<String, Integer>>> issueCouponsToSegment(
            @PathVariable com.hisobnoma.platform.web.entity.WebSegmentType segment,
            @RequestParam(required = false) Integer param,
            @Valid @RequestBody com.hisobnoma.platform.web.dto.IssueCouponRequest request) {
        int issued = couponIssueService.issueToSegment(segment, param, request);
        return ResponseEntity.ok(ApiResponse.success(
                java.util.Map.of("issued", issued), "Coupons issued"));
    }
}
