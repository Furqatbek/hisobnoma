package com.hisobnoma.platform.inventory.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.inventory.dto.CreateVendorContactRequest;
import com.hisobnoma.platform.inventory.dto.CreateVendorRequest;
import com.hisobnoma.platform.inventory.dto.VendorContactDto;
import com.hisobnoma.platform.inventory.dto.VendorDto;
import com.hisobnoma.platform.inventory.service.VendorContactService;
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
    private final VendorContactService vendorContactService;

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

    // ============ Vendor Contacts ============

    @GetMapping("/{vendorId}/contacts")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_VIEW')")
    public ResponseEntity<ApiResponse<List<VendorContactDto>>> getVendorContacts(@PathVariable Long vendorId) {
        List<VendorContactDto> contacts = vendorContactService.getContactsByVendor(vendorId);
        return ResponseEntity.ok(ApiResponse.success(contacts));
    }

    @GetMapping("/{vendorId}/contacts/primary")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_VIEW')")
    public ResponseEntity<ApiResponse<VendorContactDto>> getPrimaryContact(@PathVariable Long vendorId) {
        VendorContactDto contact = vendorContactService.getPrimaryContact(vendorId);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @GetMapping("/{vendorId}/contacts/billing")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_VIEW')")
    public ResponseEntity<ApiResponse<VendorContactDto>> getBillingContact(@PathVariable Long vendorId) {
        VendorContactDto contact = vendorContactService.getBillingContact(vendorId);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @GetMapping("/{vendorId}/contacts/ordering")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_VIEW')")
    public ResponseEntity<ApiResponse<VendorContactDto>> getOrderingContact(@PathVariable Long vendorId) {
        VendorContactDto contact = vendorContactService.getOrderingContact(vendorId);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @GetMapping("/contacts/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_VIEW')")
    public ResponseEntity<ApiResponse<VendorContactDto>> getContact(@PathVariable Long id) {
        VendorContactDto contact = vendorContactService.getContact(id);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @PostMapping("/{vendorId}/contacts")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_MANAGE')")
    public ResponseEntity<ApiResponse<VendorContactDto>> createContact(
            @PathVariable Long vendorId,
            @Valid @RequestBody CreateVendorContactRequest request) {
        request.setVendorId(vendorId);
        VendorContactDto contact = vendorContactService.createContact(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(contact));
    }

    @PutMapping("/contacts/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_MANAGE')")
    public ResponseEntity<ApiResponse<VendorContactDto>> updateContact(
            @PathVariable Long id,
            @Valid @RequestBody CreateVendorContactRequest request) {
        VendorContactDto contact = vendorContactService.updateContact(id, request);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }

    @DeleteMapping("/contacts/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_MANAGE')")
    public ResponseEntity<Void> deleteContact(@PathVariable Long id) {
        vendorContactService.deleteContact(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/contacts/{id}/set-primary")
    @PreAuthorize("hasAuthority('INVENTORY_VENDOR_MANAGE')")
    public ResponseEntity<ApiResponse<VendorContactDto>> setPrimaryContact(@PathVariable Long id) {
        VendorContactDto contact = vendorContactService.setPrimaryContact(id);
        return ResponseEntity.ok(ApiResponse.success(contact));
    }
}
