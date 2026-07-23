package com.hisobnoma.platform.distribution.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.distribution.dto.DistributionOrderDto;
import com.hisobnoma.platform.distribution.dto.DistributionRouteDto;
import com.hisobnoma.platform.distribution.dto.DistributionVisitDto;
import com.hisobnoma.platform.distribution.dto.VanLoadoutDto;
import com.hisobnoma.platform.distribution.dto.AgentCheckInRequest;
import com.hisobnoma.platform.distribution.dto.AgentCreateOrderRequest;
import com.hisobnoma.platform.distribution.dto.VisitCheckOutRequest;
import com.hisobnoma.platform.distribution.service.AgentPortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Self-service agent-app portal. All endpoints are token-guarded inside the
 * service (identity resolved from the bearer token); nothing here trusts a
 * client-supplied agentId or tenant.
 */
@RestController
@RequestMapping("/api/v1/agent/me")
@RequiredArgsConstructor
public class AgentPortalController {

    private final AgentPortalService portalService;

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> summary(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getTodaySummary(auth)));
    }

    @GetMapping("/routes")
    public ResponseEntity<ApiResponse<List<DistributionRouteDto>>> routes(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getMyRoutes(auth)));
    }

    @GetMapping("/visits")
    public ResponseEntity<PageResponse<DistributionVisitDto>> visits(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(portalService.getMyVisits(auth, pageable));
    }

    @GetMapping("/loadout/current")
    public ResponseEntity<ApiResponse<VanLoadoutDto>> currentLoadout(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth) {
        return ResponseEntity.ok(ApiResponse.success(portalService.getCurrentLoadout(auth)));
    }

    @GetMapping("/orders")
    public ResponseEntity<PageResponse<DistributionOrderDto>> orders(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(portalService.getMyOrders(auth, pageable));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<DistributionOrderDto>> placeOrder(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @Valid @RequestBody AgentCreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(portalService.placeOrder(auth, request), "Order placed"));
    }

    @PostMapping("/visits/check-in")
    public ResponseEntity<ApiResponse<DistributionVisitDto>> checkIn(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @Valid @RequestBody AgentCheckInRequest request) {
        return ResponseEntity.ok(ApiResponse.success(portalService.checkIn(auth, request), "Checked in"));
    }

    @PostMapping("/visits/{id}/check-out")
    public ResponseEntity<ApiResponse<DistributionVisitDto>> checkOut(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String auth,
            @PathVariable Long id,
            @Valid @RequestBody VisitCheckOutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(portalService.checkOut(auth, id, request), "Checked out"));
    }
}
