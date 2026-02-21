package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.inventory.dto.CreateProductUomRequest;
import com.hisobnoma.platform.inventory.dto.ProductUomDto;
import com.hisobnoma.platform.inventory.service.ProductUomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory/products/{productId}/uoms")
@RequiredArgsConstructor
public class ProductUomController {

    private final ProductUomService productUomService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_VIEW')")
    public ResponseEntity<List<ProductUomDto>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productUomService.getByProduct(productId));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_VIEW')")
    public ResponseEntity<List<ProductUomDto>> getActiveByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(productUomService.getActiveByProduct(productId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_EDIT')")
    public ResponseEntity<ProductUomDto> create(
            @PathVariable Long productId,
            @Valid @RequestBody CreateProductUomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productUomService.create(productId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_EDIT')")
    public ResponseEntity<ProductUomDto> update(
            @PathVariable Long productId,
            @PathVariable Long id,
            @Valid @RequestBody CreateProductUomRequest request) {
        return ResponseEntity.ok(productUomService.update(productId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_EDIT')")
    public ResponseEntity<Void> delete(
            @PathVariable Long productId,
            @PathVariable Long id) {
        productUomService.delete(productId, id);
        return ResponseEntity.noContent().build();
    }
}
