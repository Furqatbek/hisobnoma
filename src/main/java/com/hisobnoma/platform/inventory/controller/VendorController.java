package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreateVendorRequest;
import com.hisobnoma.platform.inventory.dto.VendorDto;
import com.hisobnoma.platform.inventory.service.VendorService;
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
@RequestMapping("/api/v1/inventory/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_VIEW')")
    public ResponseEntity<PageResponse<VendorDto>> getVendors(
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(vendorService.getVendors(pageable));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_VIEW')")
    public ResponseEntity<List<VendorDto>> getActiveVendors() {
        return ResponseEntity.ok(vendorService.getActiveVendors());
    }

    @GetMapping("/preferred")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_VIEW')")
    public ResponseEntity<List<VendorDto>> getPreferredVendors() {
        return ResponseEntity.ok(vendorService.getPreferredVendors());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_VIEW')")
    public ResponseEntity<VendorDto> getVendor(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.getVendor(id));
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_VIEW')")
    public ResponseEntity<VendorDto> getVendorByCode(@PathVariable String code) {
        return ResponseEntity.ok(vendorService.getVendorByCode(code));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_VIEW')")
    public ResponseEntity<PageResponse<VendorDto>> searchVendors(
            @RequestParam String q,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(vendorService.searchVendors(q, pageable));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_MANAGE')")
    public ResponseEntity<VendorDto> createVendor(@Valid @RequestBody CreateVendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vendorService.createVendor(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_MANAGE')")
    public ResponseEntity<VendorDto> updateVendor(
            @PathVariable Long id,
            @Valid @RequestBody CreateVendorRequest request) {
        return ResponseEntity.ok(vendorService.updateVendor(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_MANAGE')")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long id) {
        vendorService.deleteVendor(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_MANAGE')")
    public ResponseEntity<VendorDto> activateVendor(@PathVariable Long id) {
        return ResponseEntity.ok(vendorService.activateVendor(id));
    }
}
