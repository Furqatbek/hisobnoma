package com.hisobnoma.platform.mobile.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.mobile.dto.QuickSaleRequest;
import com.hisobnoma.platform.mobile.dto.QuickStockCountRequest;
import com.hisobnoma.platform.mobile.service.MobileQuickActionService;
import com.hisobnoma.platform.pos.dto.POSTransactionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mobile quick action controller for barcode lookup, quick sales, and stock counts.
 */
@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
@Tag(name = "Mobile Quick Actions", description = "Mobile quick action APIs for barcode scanning and quick operations")
public class MobileQuickActionController {

    private final MobileQuickActionService quickActionService;

    @GetMapping("/inventory/barcode/{barcode}")
    @PreAuthorize("hasAnyAuthority('INVENTORY_PRODUCT_READ', 'POS_SALE_CREATE', 'MOBILE_INVENTORY_VIEW')")
    @Operation(summary = "Barcode lookup", description = "Look up product by barcode with stock information")
    public ResponseEntity<ApiResponse<Map<String, Object>>> lookupByBarcode(
            @Parameter(description = "Product barcode") @PathVariable String barcode) {
        Map<String, Object> result = quickActionService.lookupByBarcode(barcode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/inventory/quick-count")
    @PreAuthorize("hasAnyAuthority('MOBILE_QUICK_COUNT', 'INVENTORY_COUNT_CREATE', 'INVENTORY_STOCK_ADJUST')")
    @Operation(summary = "Quick stock count", description = "Perform a quick stock count for a product")
    public ResponseEntity<ApiResponse<Map<String, Object>>> quickStockCount(
            @Valid @RequestBody QuickStockCountRequest request) {
        Map<String, Object> result = quickActionService.performQuickStockCount(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/pos/quick-sale")
    @PreAuthorize("hasAnyAuthority('MOBILE_QUICK_SALE', 'POS_SALE_CREATE')")
    @Operation(summary = "Quick sale", description = "Create and complete a simple quick sale transaction")
    public ResponseEntity<ApiResponse<POSTransactionDto>> quickSale(
            @Valid @RequestBody QuickSaleRequest request) {
        POSTransactionDto transaction = quickActionService.performQuickSale(request);
        return ResponseEntity.ok(ApiResponse.success(transaction));
    }

    @GetMapping("/customers/search")
    @PreAuthorize("hasAnyAuthority('FINANCE_AR_CUSTOMER_VIEW', 'POS_SALE_CREATE')")
    @Operation(summary = "Search customers", description = "Search customers by name, code, or phone")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> searchCustomers(
            @Parameter(description = "Search query") @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Map<String, Object>> results = quickActionService.searchCustomers(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(results)));
    }

    @GetMapping("/inventory/search")
    @PreAuthorize("hasAnyAuthority('INVENTORY_PRODUCT_READ', 'POS_SALE_CREATE')")
    @Operation(summary = "Search products", description = "Search products by name, SKU, or barcode")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> searchProducts(
            @Parameter(description = "Search query") @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Map<String, Object>> results = quickActionService.searchProducts(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(results)));
    }

    @GetMapping("/products/search")
    @PreAuthorize("hasAnyAuthority('INVENTORY_PRODUCT_READ', 'POS_SALE_CREATE')")
    @Operation(summary = "Search products by query", description = "Search products by name, SKU, or barcode (alias endpoint)")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> searchProductsByQuery(
            @Parameter(description = "Search query") @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Map<String, Object>> results = quickActionService.searchProducts(query, page, size);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(results)));
    }
}
