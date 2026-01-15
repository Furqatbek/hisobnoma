package com.hisobnoma.platform.mobile.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.mobile.dto.MobileSyncDataDTO;
import com.hisobnoma.platform.mobile.service.MobileSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Mobile sync controller for offline data synchronization.
 */
@RestController
@RequestMapping("/api/v1/mobile/sync")
@RequiredArgsConstructor
@Tag(name = "Mobile Sync", description = "Mobile offline data synchronization APIs")
public class MobileSyncController {

    private final MobileSyncService syncService;

    @GetMapping("/products")
    @PreAuthorize("hasAnyAuthority('MOBILE_SYNC_ACCESS', 'INVENTORY_PRODUCT_READ', 'POS_SALE_CREATE')")
    @Operation(summary = "Sync products", description = "Get product catalog for offline sync")
    public ResponseEntity<ApiResponse<MobileSyncDataDTO>> syncProducts(
            @Parameter(description = "Last sync timestamp for incremental sync")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant lastSyncAt) {
        MobileSyncDataDTO data = syncService.getProductsForSync(lastSyncAt);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/customers")
    @PreAuthorize("hasAnyAuthority('MOBILE_SYNC_ACCESS', 'FINANCE_AR_CUSTOMER_VIEW', 'POS_SALE_CREATE')")
    @Operation(summary = "Sync customers", description = "Get customer list for offline sync")
    public ResponseEntity<ApiResponse<MobileSyncDataDTO>> syncCustomers(
            @Parameter(description = "Last sync timestamp for incremental sync")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant lastSyncAt) {
        MobileSyncDataDTO data = syncService.getCustomersForSync(lastSyncAt);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/categories")
    @PreAuthorize("hasAnyAuthority('MOBILE_SYNC_ACCESS', 'INVENTORY_PRODUCT_READ', 'POS_SALE_CREATE')")
    @Operation(summary = "Sync categories", description = "Get category tree for offline sync")
    public ResponseEntity<ApiResponse<MobileSyncDataDTO>> syncCategories() {
        MobileSyncDataDTO data = syncService.getCategoriesForSync();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/last-updated")
    @PreAuthorize("hasAuthority('MOBILE_SYNC_ACCESS')")
    @Operation(summary = "Check last update", description = "Check when data was last updated to decide if sync is needed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLastUpdated() {
        Instant lastUpdated = syncService.getLastUpdatedTimestamp();
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "lastUpdated", lastUpdated != null ? lastUpdated.toString() : null,
                "syncRequired", lastUpdated != null
        )));
    }
}
