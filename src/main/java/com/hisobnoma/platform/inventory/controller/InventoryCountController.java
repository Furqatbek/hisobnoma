package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.CountStatus;
import com.hisobnoma.platform.inventory.service.InventoryCountService;
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
@RequestMapping("/api/v1/inventory/counts")
@RequiredArgsConstructor
public class InventoryCountController {

    private final InventoryCountService inventoryCountService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_VIEW')")
    public ResponseEntity<PageResponse<InventoryCountDto>> getInventoryCounts(
            @PageableDefault(sort = "countDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryCountService.getInventoryCounts(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_VIEW')")
    public ResponseEntity<InventoryCountDto> getInventoryCount(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryCountService.getInventoryCount(id));
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_VIEW')")
    public ResponseEntity<PageResponse<InventoryCountDto>> getInventoryCountsByStatus(
            @PathVariable CountStatus status,
            @PageableDefault(sort = "countDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryCountService.getInventoryCountsByStatus(status, pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_VIEW')")
    public ResponseEntity<PageResponse<InventoryCountDto>> searchInventoryCounts(
            @RequestParam String q,
            @PageableDefault(sort = "countDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(inventoryCountService.searchInventoryCounts(q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_CREATE')")
    public ResponseEntity<InventoryCountDto> createInventoryCount(
            @Valid @RequestBody CreateInventoryCountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inventoryCountService.createInventoryCount(request));
    }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_PERFORM')")
    public ResponseEntity<InventoryCountDto> startCount(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryCountService.startCount(id));
    }

    @PutMapping("/{countId}/lines/{lineId}/count")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_PERFORM')")
    public ResponseEntity<InventoryCountLineDto> recordCount(
            @PathVariable Long countId,
            @PathVariable Long lineId,
            @Valid @RequestBody RecordCountRequest request) {
        return ResponseEntity.ok(inventoryCountService.recordCount(countId, lineId, request));
    }

    @PutMapping("/{countId}/lines/{lineId}/recount")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_PERFORM')")
    public ResponseEntity<Void> markForRecount(
            @PathVariable Long countId,
            @PathVariable Long lineId,
            @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        inventoryCountService.markForRecount(countId, lineId, reason);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_PERFORM')")
    public ResponseEntity<InventoryCountDto> completeCount(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryCountService.completeCount(id));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_APPROVE')")
    public ResponseEntity<InventoryCountDto> approveCount(@PathVariable Long id) {
        return ResponseEntity.ok(inventoryCountService.approveCount(id));
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('INVENTORY_COUNT_APPROVE')")
    public ResponseEntity<InventoryCountDto> cancelCount(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String reason = body.get("reason");
        return ResponseEntity.ok(inventoryCountService.cancelCount(id, reason));
    }
}
