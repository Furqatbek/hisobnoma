package com.hisobnoma.platform.distribution.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.distribution.dto.CreateDistributionRouteRequest;
import com.hisobnoma.platform.distribution.dto.DistributionRouteDto;
import com.hisobnoma.platform.distribution.dto.UpdateDistributionRouteRequest;
import com.hisobnoma.platform.distribution.service.DistributionRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/distribution/routes")
@RequiredArgsConstructor
public class DistributionRouteController {

    private final DistributionRouteService routeService;

    @GetMapping
    @PreAuthorize("hasAuthority('DISTRIBUTION_ROUTE_VIEW')")
    public ResponseEntity<PageResponse<DistributionRouteDto>> getRoutes(
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(routeService.getRoutes(pageable));
    }

    @GetMapping("/by-agent/{agentId}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ROUTE_VIEW')")
    public ResponseEntity<List<DistributionRouteDto>> getRoutesByAgent(@PathVariable Long agentId) {
        return ResponseEntity.ok(routeService.getRoutesByAgent(agentId));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ROUTE_VIEW')")
    public ResponseEntity<PageResponse<DistributionRouteDto>> searchRoutes(
            @RequestParam String q,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(routeService.searchRoutes(q, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ROUTE_VIEW')")
    public ResponseEntity<DistributionRouteDto> getRoute(@PathVariable Long id) {
        return ResponseEntity.ok(routeService.getRoute(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DISTRIBUTION_ROUTE_MANAGE')")
    public ResponseEntity<DistributionRouteDto> createRoute(@Valid @RequestBody CreateDistributionRouteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(routeService.createRoute(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ROUTE_MANAGE')")
    public ResponseEntity<DistributionRouteDto> updateRoute(
            @PathVariable Long id, @Valid @RequestBody UpdateDistributionRouteRequest request) {
        return ResponseEntity.ok(routeService.updateRoute(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ROUTE_MANAGE')")
    public ResponseEntity<Void> deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }
}
