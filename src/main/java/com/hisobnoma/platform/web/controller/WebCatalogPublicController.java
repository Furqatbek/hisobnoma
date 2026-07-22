package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.web.dto.PublicCatalogProductDto;
import com.hisobnoma.platform.web.dto.PublicCategoryDto;
import com.hisobnoma.platform.web.service.WebCatalogPublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public (anonymous) catalog API for the online shop mobile app.
 * The /api/v1/web/** prefix is whitelisted in SecurityConfig;
 * tenant is resolved from the X-Tenant-ID header and is required — requests without it are
 * rejected with 400 TENANT_REQUIRED (fail closed).
 */
@RestController
@RequestMapping("/api/v1/web/catalog")
@RequiredArgsConstructor
public class WebCatalogPublicController {

    private final WebCatalogPublicService publicService;

    @GetMapping("/products")
    public ResponseEntity<PageResponse<PublicCatalogProductDto>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PublicCatalogProductDto> page = publicService.getProducts(search, categoryId, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<PublicCatalogProductDto>> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(publicService.getProduct(id)));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<PublicCategoryDto>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.success(publicService.getCategories()));
    }
}
