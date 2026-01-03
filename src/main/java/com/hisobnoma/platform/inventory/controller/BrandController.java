package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.service.BrandService;
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

@RestController
@RequestMapping("/api/v1/inventory/brands")
@RequiredArgsConstructor
public class BrandController {

    private final BrandService brandService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<List<BrandDto>> getAllBrands() {
        return ResponseEntity.ok(brandService.getAllBrands());
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<PageResponse<BrandDto>> getBrandsPaginated(
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(brandService.getBrands(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<List<BrandDto>> getActiveBrands() {
        return ResponseEntity.ok(brandService.getActiveBrands());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<BrandDto> getBrand(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.getBrand(id));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<PageResponse<BrandDto>> searchBrands(
            @RequestParam String q,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(brandService.searchBrands(q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_CREATE')")
    public ResponseEntity<BrandDto> createBrand(@Valid @RequestBody CreateBrandRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(brandService.createBrand(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<BrandDto> updateBrand(
            @PathVariable Long id,
            @Valid @RequestBody UpdateBrandRequest request) {
        return ResponseEntity.ok(brandService.updateBrand(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_DELETE')")
    public ResponseEntity<Void> deleteBrand(@PathVariable Long id) {
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}
