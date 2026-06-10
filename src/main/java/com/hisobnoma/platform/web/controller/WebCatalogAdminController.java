package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.web.dto.AddCatalogItemsRequest;
import com.hisobnoma.platform.web.dto.UpdateCatalogItemRequest;
import com.hisobnoma.platform.web.dto.WebCatalogCountsDto;
import com.hisobnoma.platform.web.dto.WebCatalogItemDto;
import com.hisobnoma.platform.web.service.WebCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Staff management of the online shop catalog.
 */
@RestController
@RequestMapping("/api/v1/web-catalog")
@RequiredArgsConstructor
public class WebCatalogAdminController {

    private final WebCatalogService catalogService;

    @GetMapping
    @RequiresPermission("WEB_CATALOG_VIEW")
    public ResponseEntity<PageResponse<WebCatalogItemDto>> getItems(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 50) Pageable pageable) {
        Page<WebCatalogItemDto> page = catalogService.getItems(search, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/counts")
    @RequiresPermission("WEB_CATALOG_VIEW")
    public ResponseEntity<ApiResponse<WebCatalogCountsDto>> getCounts() {
        return ResponseEntity.ok(ApiResponse.success(catalogService.getCounts()));
    }

    @PostMapping("/items")
    @RequiresPermission("WEB_CATALOG_MANAGE")
    public ResponseEntity<ApiResponse<List<WebCatalogItemDto>>> addProducts(
            @Valid @RequestBody AddCatalogItemsRequest request) {
        List<WebCatalogItemDto> added = catalogService.addProducts(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(added));
    }

    @PutMapping("/items/{id}")
    @RequiresPermission("WEB_CATALOG_MANAGE")
    public ResponseEntity<ApiResponse<WebCatalogItemDto>> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCatalogItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.updateItem(id, request)));
    }

    @DeleteMapping("/items/{id}")
    @RequiresPermission("WEB_CATALOG_MANAGE")
    public ResponseEntity<ApiResponse<Void>> removeItem(@PathVariable Long id) {
        catalogService.removeItem(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Catalog item removed"));
    }

    @PostMapping("/items/{id}/publish")
    @RequiresPermission("WEB_CATALOG_MANAGE")
    public ResponseEntity<ApiResponse<WebCatalogItemDto>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.publish(id)));
    }

    @PostMapping("/items/{id}/unpublish")
    @RequiresPermission("WEB_CATALOG_MANAGE")
    public ResponseEntity<ApiResponse<WebCatalogItemDto>> unpublish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(catalogService.unpublish(id)));
    }

    @PostMapping("/items/{id}/move-up")
    @RequiresPermission("WEB_CATALOG_MANAGE")
    public ResponseEntity<ApiResponse<Void>> moveUp(@PathVariable Long id) {
        catalogService.moveUp(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Item moved up"));
    }

    @PostMapping("/items/{id}/move-down")
    @RequiresPermission("WEB_CATALOG_MANAGE")
    public ResponseEntity<ApiResponse<Void>> moveDown(@PathVariable Long id) {
        catalogService.moveDown(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Item moved down"));
    }

    @PostMapping("/publish-all")
    @RequiresPermission("WEB_CATALOG_MANAGE")
    public ResponseEntity<ApiResponse<Integer>> publishAll() {
        return ResponseEntity.ok(ApiResponse.success(catalogService.publishAll()));
    }

    @PostMapping("/unpublish-all")
    @RequiresPermission("WEB_CATALOG_MANAGE")
    public ResponseEntity<ApiResponse<Integer>> unpublishAll() {
        return ResponseEntity.ok(ApiResponse.success(catalogService.unpublishAll()));
    }
}
