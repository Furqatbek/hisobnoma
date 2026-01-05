package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.inventory.dto.AbcAnalysisDto;
import com.hisobnoma.platform.inventory.dto.LowStockDto;
import com.hisobnoma.platform.inventory.dto.ReorderSuggestionDto;
import com.hisobnoma.platform.inventory.service.InventoryPlanningService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for inventory planning and analysis APIs.
 */
@RestController
@RequestMapping("/api/v1/inventory/planning")
@RequiredArgsConstructor
public class InventoryPlanningController {

    private final InventoryPlanningService inventoryPlanningService;

    /**
     * Get reorder suggestions for products below reorder point.
     */
    @GetMapping("/reorder-suggestions")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_READ')")
    public ResponseEntity<List<ReorderSuggestionDto>> getReorderSuggestions(
            @RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(inventoryPlanningService.getReorderSuggestions(locationId));
    }

    /**
     * Perform ABC analysis on products based on value/movement.
     */
    @GetMapping("/abc-analysis")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_READ')")
    public ResponseEntity<List<AbcAnalysisDto>> getAbcAnalysis(
            @RequestParam(defaultValue = "365") int days) {
        return ResponseEntity.ok(inventoryPlanningService.performAbcAnalysis(days));
    }

    /**
     * Get slow-moving products (not sold in X days).
     */
    @GetMapping("/slow-moving")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_READ')")
    public ResponseEntity<List<LowStockDto>> getSlowMovingProducts(
            @RequestParam(defaultValue = "90") int days,
            @RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(inventoryPlanningService.getSlowMovingProducts(days, locationId));
    }

    /**
     * Get dead stock (zero movement products).
     */
    @GetMapping("/dead-stock")
    @PreAuthorize("hasAuthority('INVENTORY_STOCK_READ')")
    public ResponseEntity<List<LowStockDto>> getDeadStock(
            @RequestParam(defaultValue = "180") int days,
            @RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(inventoryPlanningService.getDeadStock(days, locationId));
    }
}
