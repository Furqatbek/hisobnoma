package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreatePurchaseOrderRequest;
import com.hisobnoma.platform.inventory.dto.PurchaseOrderDto;
import com.hisobnoma.platform.inventory.entity.POStatus;
import com.hisobnoma.platform.inventory.service.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_PO_VIEW')")
    public ResponseEntity<PageResponse<PurchaseOrderDto>> getPurchaseOrders(
            @PageableDefault(sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrders(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PO_VIEW')")
    public ResponseEntity<PurchaseOrderDto> getPurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrder(id));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('INVENTORY_PO_VIEW')")
    public ResponseEntity<PageResponse<PurchaseOrderDto>> getPurchaseOrdersByStatus(
            @PathVariable POStatus status,
            @PageableDefault(sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrdersByStatus(status, pageable));
    }

    @GetMapping("/vendor/{vendorId}")
    @PreAuthorize("hasAuthority('INVENTORY_PO_VIEW')")
    public ResponseEntity<PageResponse<PurchaseOrderDto>> getPurchaseOrdersByVendor(
            @PathVariable Long vendorId,
            @PageableDefault(sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(purchaseOrderService.getPurchaseOrdersByVendor(vendorId, pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('INVENTORY_PO_VIEW')")
    public ResponseEntity<PageResponse<PurchaseOrderDto>> searchPurchaseOrders(
            @RequestParam String q,
            @PageableDefault(sort = "orderDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(purchaseOrderService.searchPurchaseOrders(q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_PO_CREATE')")
    public ResponseEntity<PurchaseOrderDto> createPurchaseOrder(
            @Valid @RequestBody CreatePurchaseOrderRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(purchaseOrderService.createPurchaseOrder(request));
    }

    @PutMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('INVENTORY_PO_UPDATE')")
    public ResponseEntity<PurchaseOrderDto> submitForApproval(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.submitForApproval(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('INVENTORY_PO_APPROVE')")
    public ResponseEntity<PurchaseOrderDto> approvePurchaseOrder(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.approvePurchaseOrder(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('INVENTORY_PO_CANCEL')")
    public ResponseEntity<PurchaseOrderDto> cancelPurchaseOrder(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        return ResponseEntity.ok(purchaseOrderService.cancelPurchaseOrder(id, reason));
    }
}
