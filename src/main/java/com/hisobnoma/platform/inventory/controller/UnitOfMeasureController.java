package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.service.UnitOfMeasureService;
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
@RequestMapping("/api/v1/inventory/uom")
@RequiredArgsConstructor
public class UnitOfMeasureController {

    private final UnitOfMeasureService uomService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<List<UnitOfMeasureDto>> getAllUoms() {
        return ResponseEntity.ok(uomService.getAllUoms());
    }

    @GetMapping("/paginated")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<PageResponse<UnitOfMeasureDto>> getUomsPaginated(
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(uomService.getUoms(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<List<UnitOfMeasureDto>> getActiveUoms() {
        return ResponseEntity.ok(uomService.getActiveUoms());
    }

    @GetMapping("/base")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<List<UnitOfMeasureDto>> getBaseUoms() {
        return ResponseEntity.ok(uomService.getBaseUoms());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<UnitOfMeasureDto> getUom(@PathVariable Long id) {
        return ResponseEntity.ok(uomService.getUom(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<UnitOfMeasureDto> getUomByCode(@PathVariable String code) {
        return ResponseEntity.ok(uomService.getUomByCode(code));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<PageResponse<UnitOfMeasureDto>> searchUoms(
            @RequestParam String q,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(uomService.searchUoms(q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_CREATE')")
    public ResponseEntity<UnitOfMeasureDto> createUom(@Valid @RequestBody CreateUomRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(uomService.createUom(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_UPDATE')")
    public ResponseEntity<UnitOfMeasureDto> updateUom(
            @PathVariable Long id,
            @Valid @RequestBody CreateUomRequest request) {
        return ResponseEntity.ok(uomService.updateUom(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_DELETE')")
    public ResponseEntity<Void> deleteUom(@PathVariable Long id) {
        uomService.deleteUom(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/convert")
    @PreAuthorize("hasAuthority('INVENTORY_PRODUCT_READ')")
    public ResponseEntity<Map<String, BigDecimal>> convert(
            @RequestParam BigDecimal quantity,
            @RequestParam Long fromUomId,
            @RequestParam Long toUomId) {
        BigDecimal result = uomService.convert(quantity, fromUomId, toUomId);
        return ResponseEntity.ok(Map.of("result", result));
    }
}
