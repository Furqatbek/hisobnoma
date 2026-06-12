package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.common.util.ClientIpResolver;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.web.dto.LoyaltyBalanceDto;
import com.hisobnoma.platform.web.dto.PublicCouponDto;
import com.hisobnoma.platform.web.dto.PublicOrderDto;
import com.hisobnoma.platform.web.dto.ReferralStatsDto;
import com.hisobnoma.platform.web.dto.RegisterDeviceTokenRequest;
import com.hisobnoma.platform.web.dto.RequestOtpRequest;
import com.hisobnoma.platform.web.dto.VerifyOtpRequest;
import com.hisobnoma.platform.web.dto.WebAuthResponse;
import com.hisobnoma.platform.web.dto.WebNotificationDto;
import com.hisobnoma.platform.web.dto.WishlistItemDto;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.service.WebAuthService;
import com.hisobnoma.platform.web.service.WebCouponListService;
import com.hisobnoma.platform.web.service.WebLoyaltyService;
import com.hisobnoma.platform.web.service.WebNotificationService;
import com.hisobnoma.platform.web.service.WebPushService;
import com.hisobnoma.platform.web.service.WebReferralService;
import com.hisobnoma.platform.web.service.WebWishlistService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Phone + SMS OTP auth and the customer's own data for the mobile app.
 * Endpoints are anonymous at the security-chain level (/api/v1/web/**);
 * /me endpoints validate the web-customer bearer token themselves.
 */
@RestController
@RequestMapping("/api/v1/web")
@RequiredArgsConstructor
public class WebAuthPublicController {

    private final ClientIpResolver clientIpResolver;
    private final WebAuthService authService;
    private final WebCouponListService couponListService;
    private final WebLoyaltyService loyaltyService;
    private final WebNotificationService notificationService;
    private final WebPushService pushService;
    private final WebReferralService referralService;
    private final WebWishlistService wishlistService;

    @PostMapping("/auth/request-otp")
    public ResponseEntity<ApiResponse<Void>> requestOtp(
            @Valid @RequestBody RequestOtpRequest request,
            HttpServletRequest httpRequest) {
        authService.requestOtp(request.getPhone(), clientIp(httpRequest));
        return ResponseEntity.ok(ApiResponse.success(null, "Code sent"));
    }

    @PostMapping("/auth/verify")
    public ResponseEntity<ApiResponse<WebAuthResponse>> verify(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                authService.verifyOtp(request.getPhone(), request.getCode(),
                        request.getName(), request.getReferralCode())));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, String>>> me(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return ResponseEntity.ok(ApiResponse.success(authService.getProfile(authorization)));
    }

    @GetMapping("/me/orders")
    public ResponseEntity<PageResponse<PublicOrderDto>> myOrders(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PublicOrderDto> page = authService.getMyOrders(authorization, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/me/loyalty")
    public ResponseEntity<ApiResponse<LoyaltyBalanceDto>> myLoyalty(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        WebCustomer customer = authService.requireCustomer(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                loyaltyService.getBalance(customer.getTenantId(), customer.getId())));
    }

    @PostMapping("/me/device-token")
    public ResponseEntity<ApiResponse<Void>> registerDeviceToken(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @Valid @RequestBody RegisterDeviceTokenRequest request) {
        WebCustomer customer = authService.requireCustomer(authorization);
        pushService.register(customer.getTenantId(), customer.getId(),
                request.getToken(), request.getPlatform());
        return ResponseEntity.ok(ApiResponse.success(null, "Device token registered"));
    }

    @DeleteMapping("/me/device-token")
    public ResponseEntity<ApiResponse<Void>> unregisterDeviceToken(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @RequestParam(required = false) String token) {
        WebCustomer customer = authService.requireCustomer(authorization);
        pushService.unregister(customer.getTenantId(), customer.getId(), token);
        return ResponseEntity.ok(ApiResponse.success(null, "Device token removed"));
    }

    @GetMapping("/me/referral-code")
    public ResponseEntity<ApiResponse<Map<String, String>>> myReferralCode(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        WebCustomer customer = authService.requireCustomer(authorization);
        String code = referralService.getOrCreateCode(customer.getTenantId(), customer.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "code", code != null ? code : "")));
    }

    @GetMapping("/me/referral-stats")
    public ResponseEntity<ApiResponse<ReferralStatsDto>> myReferralStats(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        WebCustomer customer = authService.requireCustomer(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                referralService.getStats(customer.getTenantId(), customer.getId())));
    }

    // ---- Notifications ----

    @GetMapping("/me/notifications")
    public ResponseEntity<PageResponse<WebNotificationDto>> myNotifications(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PageableDefault(size = 20) Pageable pageable) {
        WebCustomer customer = authService.requireCustomer(authorization);
        Page<WebNotificationDto> page = notificationService.getNotifications(
                customer.getTenantId(), customer.getId(), pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/me/notifications/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> notificationsUnreadCount(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        WebCustomer customer = authService.requireCustomer(authorization);
        long count = notificationService.countUnread(customer.getTenantId(), customer.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", count)));
    }

    @PutMapping("/me/notifications/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markNotificationRead(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable Long id) {
        WebCustomer customer = authService.requireCustomer(authorization);
        notificationService.markRead(customer.getTenantId(), customer.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/me/notifications/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsRead(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        WebCustomer customer = authService.requireCustomer(authorization);
        notificationService.markAllRead(customer.getTenantId(), customer.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ---- Coupons ----

    @GetMapping("/me/coupons")
    public ResponseEntity<ApiResponse<List<PublicCouponDto>>> myCoupons(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        WebCustomer customer = authService.requireCustomer(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                couponListService.getAvailableCoupons(customer.getTenantId(), customer.getId())));
    }

    @GetMapping("/me/wishlist")
    public ResponseEntity<PageResponse<WishlistItemDto>> myWishlist(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PageableDefault(size = 20) Pageable pageable) {
        WebCustomer customer = authService.requireCustomer(authorization);
        Page<WishlistItemDto> page = wishlistService.getWishlist(
                customer.getTenantId(), customer.getId(), pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/me/wishlist/ids")
    public ResponseEntity<ApiResponse<List<Long>>> myWishlistIds(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        WebCustomer customer = authService.requireCustomer(authorization);
        return ResponseEntity.ok(ApiResponse.success(
                wishlistService.getWishlistIds(customer.getTenantId(), customer.getId())));
    }

    @PutMapping("/me/wishlist/{catalogItemId}")
    public ResponseEntity<ApiResponse<Void>> likeItem(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable Long catalogItemId) {
        WebCustomer customer = authService.requireCustomer(authorization);
        wishlistService.like(customer.getTenantId(), customer.getId(), catalogItemId);
        return ResponseEntity.ok(ApiResponse.success(null, "Liked"));
    }

    @DeleteMapping("/me/wishlist/{catalogItemId}")
    public ResponseEntity<ApiResponse<Void>> unlikeItem(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authorization,
            @PathVariable Long catalogItemId) {
        WebCustomer customer = authService.requireCustomer(authorization);
        wishlistService.unlike(customer.getTenantId(), customer.getId(), catalogItemId);
        return ResponseEntity.ok(ApiResponse.success(null, "Unliked"));
    }

    private String clientIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }
}
