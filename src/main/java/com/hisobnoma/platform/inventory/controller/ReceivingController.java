package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreateReceivingRequest;
import com.hisobnoma.platform.inventory.dto.ReceivingOrderDto;
import com.hisobnoma.platform.inventory.entity.ReceivingStatus;
import com.hisobnoma.platform.inventory.service.ReceivingService;
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
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory/receiving")
@RequiredArgsConstructor
public class ReceivingController {

    private final ReceivingService receivingService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_RECEIVING_VIEW')")
    public ResponseEntity<PageResponse<ReceivingOrderDto>> getReceivingOrders(
            @PageableDefault(sort = "receivingDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(receivingService.getReceivingOrders(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_RECEIVING_VIEW')")
    public ResponseEntity<ReceivingOrderDto> getReceivingOrder(@PathVariable Long id) {
        return ResponseEntity.ok(receivingService.getReceivingOrder(id));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('INVENTORY_RECEIVING_VIEW')")
    public ResponseEntity<PageResponse<ReceivingOrderDto>> getReceivingOrdersByStatus(
            @PathVariable ReceivingStatus status,
            @PageableDefault(sort = "receivingDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(receivingService.getReceivingOrdersByStatus(status, pageable));
    }

    @GetMapping("/purchase-order/{poId}")
    @PreAuthorize("hasAuthority('INVENTORY_RECEIVING_VIEW')")
    public ResponseEntity<List<ReceivingOrderDto>> getReceivingOrdersForPO(@PathVariable Long poId) {
        return ResponseEntity.ok(receivingService.getReceivingOrdersForPO(poId));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('INVENTORY_RECEIVING_VIEW')")
    public ResponseEntity<PageResponse<ReceivingOrderDto>> searchReceivingOrders(
            @RequestParam String q,
            @PageableDefault(sort = "receivingDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(receivingService.searchReceivingOrders(q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_RECEIVING_CREATE')")
    public ResponseEntity<ReceivingOrderDto> createReceivingOrder(
            @Valid @RequestBody CreateReceivingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(receivingService.createReceivingOrder(request));
    }

    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('INVENTORY_RECEIVING_CONFIRM')")
    public ResponseEntity<ReceivingOrderDto> confirmReceiving(@PathVariable Long id) {
        return ResponseEntity.ok(receivingService.confirmReceiving(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('INVENTORY_RECEIVING_CONFIRM')")
    public ResponseEntity<ReceivingOrderDto> cancelReceiving(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        return ResponseEntity.ok(receivingService.cancelReceiving(id, reason));
    }
}
