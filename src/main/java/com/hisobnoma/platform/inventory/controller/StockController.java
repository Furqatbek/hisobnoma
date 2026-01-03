package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/inventory/stock")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    // ==================== Stock Query Endpoints ====================

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<PageResponse<StockDto>> getStock(
            @PageableDefault(sort = "product.name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(stockService.getStock(pageable));
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<List<StockDto>> getStockByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(stockService.getStockByProduct(productId));
    }

    @GetMapping("/location/{locationId}")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<PageResponse<StockDto>> getStockByLocation(
            @PathVariable Long locationId,
            @PageableDefault(sort = "product.name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(stockService.getStockByLocation(locationId, pageable));
    }

    @GetMapping("/product/{productId}/location/{locationId}")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<StockDto> getStockByProductAndLocation(
            @PathVariable Long productId,
            @PathVariable Long locationId) {
        StockDto stock = stockService.getStockByProductAndLocation(productId, locationId);
        if (stock == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(stock);
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<PageResponse<LowStockDto>> getLowStock(
            @PageableDefault(sort = "product.name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(stockService.getLowStock(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<PageResponse<StockDto>> searchStock(
            @RequestParam String q,
            @PageableDefault(sort = "product.name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(stockService.searchStock(q, pageable));
    }

    @GetMapping("/valuation")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<StockValuationDto> getStockValuation() {
        return ResponseEntity.ok(stockService.getStockValuation());
    }

    @GetMapping("/available/{productId}")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<Map<String, BigDecimal>> getAvailableQuantity(@PathVariable Long productId) {
        BigDecimal available = stockService.getAvailableQuantity(productId);
        return ResponseEntity.ok(Map.of("availableQuantity", available));
    }

    @GetMapping("/check-availability")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<Map<String, Boolean>> checkAvailability(
            @RequestParam Long productId,
            @RequestParam Long locationId,
            @RequestParam BigDecimal quantity) {
        boolean available = stockService.isStockAvailable(productId, locationId, quantity);
        return ResponseEntity.ok(Map.of("available", available));
    }

    // ==================== Stock Movement Query Endpoints ====================

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<PageResponse<StockMovementDto>> getStockMovements(
            @PageableDefault(sort = "movementDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(stockService.getStockMovements(pageable));
    }

    @GetMapping("/movements/product/{productId}")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<PageResponse<StockMovementDto>> getStockMovementsByProduct(
            @PathVariable Long productId,
            @PageableDefault(sort = "movementDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(stockService.getStockMovementsByProduct(productId, pageable));
    }

    @GetMapping("/movements/location/{locationId}")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_VIEW')")
    public ResponseEntity<PageResponse<StockMovementDto>> getStockMovementsByLocation(
            @PathVariable Long locationId,
            @PageableDefault(sort = "movementDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(stockService.getStockMovementsByLocation(locationId, pageable));
    }

    // ==================== Stock Operation Endpoints ====================

    @PostMapping("/receive")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_RECEIVE')")
    public ResponseEntity<List<StockMovementDto>> receiveStock(
            @Valid @RequestBody StockReceiveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(stockService.receiveStock(request));
    }

    @PostMapping("/issue")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_ISSUE')")
    public ResponseEntity<List<StockMovementDto>> issueStock(
            @Valid @RequestBody StockIssueRequest request) {
        return ResponseEntity.ok(stockService.issueStock(request));
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_TRANSFER')")
    public ResponseEntity<List<StockMovementDto>> transferStock(
            @Valid @RequestBody StockTransferRequest request) {
        return ResponseEntity.ok(stockService.transferStock(request));
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_ADJUST')")
    public ResponseEntity<List<StockMovementDto>> adjustStock(
            @Valid @RequestBody StockAdjustRequest request) {
        return ResponseEntity.ok(stockService.adjustStock(request));
    }
}
