package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.common.util.ClientIpResolver;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.web.dto.CheckoutRequest;
import com.hisobnoma.platform.web.dto.PublicOrderDto;
import com.hisobnoma.platform.web.dto.PublicRegionDto;
import com.hisobnoma.platform.web.dto.PublicVillageDto;
import com.hisobnoma.platform.web.exception.TooManyRequestsException;
import com.hisobnoma.platform.web.service.CheckoutRateLimiter;
import com.hisobnoma.platform.web.service.WebOrderPublicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public (anonymous) ordering API for the shop mobile app.
 * Lives under the whitelisted /api/v1/web/** prefix; tenant from X-Tenant-ID.
 */
@RestController
@RequestMapping("/api/v1/web")
@RequiredArgsConstructor
public class WebOrderPublicController {

    private final ClientIpResolver clientIpResolver;
    private final WebOrderPublicService publicService;
    private final CheckoutRateLimiter rateLimiter;

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<PublicOrderDto>> checkout(
            @Valid @RequestBody CheckoutRequest request,
            HttpServletRequest httpRequest) {
        PublicOrderDto order = publicService.checkout(
                request, clientIp(httpRequest), httpRequest.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(order));
    }

    @GetMapping("/orders/{orderNumber}")
    public ResponseEntity<ApiResponse<PublicOrderDto>> getOrderStatus(
            @PathVariable String orderNumber,
            @RequestParam String phone,
            HttpServletRequest httpRequest) {
        // Order numbers are sequential (WO-000042) and the only auth here is a
        // phone number, so the lookup is enumerable. Throttle per IP: the
        // 10s bucket allows ~5 lookups / 10s (fine for one customer polling
        // their order, but it stops anyone scanning the WO-* range).
        String bucket = String.valueOf(System.currentTimeMillis() / 10_000);
        if (!rateLimiter.tryAcquire("order-lookup|" + bucket + "|" + clientIp(httpRequest))) {
            throw new TooManyRequestsException("Too many lookups, please try again later");
        }
        return ResponseEntity.ok(ApiResponse.success(
                publicService.getOrderStatus(orderNumber, phone)));
    }

    @GetMapping("/delivery/regions")
    public ResponseEntity<ApiResponse<List<PublicRegionDto>>> getRegions() {
        return ResponseEntity.ok(ApiResponse.success(publicService.getRegions()));
    }

    @GetMapping("/delivery/villages")
    public ResponseEntity<ApiResponse<List<PublicVillageDto>>> getVillages(
            @RequestParam(required = false) Long regionId) {
        return ResponseEntity.ok(ApiResponse.success(publicService.getVillages(regionId)));
    }

    private String clientIp(HttpServletRequest request) {
        return clientIpResolver.resolve(request);
    }
}
