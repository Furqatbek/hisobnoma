package com.hisobnoma.platform.distribution.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.distribution.dto.*;
import com.hisobnoma.platform.distribution.entity.DistributionOrderStatus;
import com.hisobnoma.platform.distribution.service.DistributionOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/distribution/orders")
@RequiredArgsConstructor
public class DistributionOrderController {

    private final DistributionOrderService orderService;

    @GetMapping
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_VIEW')")
    public ResponseEntity<PageResponse<DistributionOrderDto>> getOrders(
            @RequestParam(required = false) DistributionOrderStatus status,
            @RequestParam(required = false) Long agentId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        if (status != null) {
            return ResponseEntity.ok(orderService.getOrdersByStatus(status, pageable));
        }
        if (agentId != null) {
            return ResponseEntity.ok(orderService.getOrdersByAgent(agentId, pageable));
        }
        return ResponseEntity.ok(orderService.getOrders(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_VIEW')")
    public ResponseEntity<PageResponse<DistributionOrderDto>> searchOrders(
            @RequestParam String q,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(orderService.searchOrders(q, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_VIEW')")
    public ResponseEntity<DistributionOrderDto> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrder(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_CREATE')")
    public ResponseEntity<DistributionOrderDto> createOrder(
            @Valid @RequestBody CreateDistributionOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_MANAGE')")
    public ResponseEntity<DistributionOrderDto> updateOrder(
            @PathVariable Long id, @Valid @RequestBody UpdateDistributionOrderRequest request) {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_MANAGE')")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }

    // ---- fulfilment lifecycle ----

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_MANAGE')")
    public ResponseEntity<DistributionOrderDto> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.confirm(id));
    }

    @PostMapping("/{id}/pick")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_MANAGE')")
    public ResponseEntity<DistributionOrderDto> pick(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.startPicking(id));
    }

    @PostMapping("/{id}/load")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_MANAGE')")
    public ResponseEntity<DistributionOrderDto> load(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.markLoaded(id));
    }

    @PostMapping("/{id}/transit")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_MANAGE')")
    public ResponseEntity<DistributionOrderDto> transit(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.markInTransit(id));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_MANAGE')")
    public ResponseEntity<DistributionOrderDto> deliver(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) DeliverDistributionOrderRequest request) {
        return ResponseEntity.ok(orderService.deliver(id, request));
    }

    @PostMapping("/{id}/invoice")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_MANAGE')")
    public ResponseEntity<DistributionOrderDto> invoice(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.invoice(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('DISTRIBUTION_ORDER_MANAGE')")
    public ResponseEntity<DistributionOrderDto> cancel(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelDistributionOrderRequest request) {
        return ResponseEntity.ok(orderService.cancel(id, request));
    }
}
