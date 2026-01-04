package com.hisobnoma.platform.pos.controller;

import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.PriceListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/price-lists")
@RequiredArgsConstructor
@Tag(name = "Price Lists", description = "Price List management APIs")
public class PriceListController {

    private final PriceListService priceListService;

    @GetMapping
    @Operation(summary = "Get all price lists", description = "Retrieves all price lists with pagination")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<Page<PriceListDto>> getAllPriceLists(Pageable pageable) {
        return ResponseEntity.ok(priceListService.findAllPriceLists(pageable));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active price lists", description = "Retrieves currently active/effective price lists")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<List<PriceListDto>> getActivePriceLists() {
        return ResponseEntity.ok(priceListService.findActivePriceLists());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get price list by ID", description = "Retrieves a price list by its ID with all items")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<PriceListDto> getPriceListById(
            @Parameter(description = "Price list ID") @PathVariable Long id) {
        return ResponseEntity.ok(priceListService.findPriceListById(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get price list by code", description = "Retrieves a price list by its code")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<PriceListDto> getPriceListByCode(
            @Parameter(description = "Price list code") @PathVariable String code) {
        return ResponseEntity.ok(priceListService.findPriceListByCode(code));
    }

    @PostMapping
    @Operation(summary = "Create price list", description = "Creates a new price list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PriceListDto> createPriceList(
            @Valid @RequestBody CreatePriceListRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(priceListService.createPriceList(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update price list", description = "Updates an existing price list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PriceListDto> updatePriceList(
            @Parameter(description = "Price list ID") @PathVariable Long id,
            @Valid @RequestBody CreatePriceListRequest request) {
        return ResponseEntity.ok(priceListService.updatePriceList(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete price list", description = "Deletes a price list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deletePriceList(
            @Parameter(description = "Price list ID") @PathVariable Long id) {
        priceListService.deletePriceList(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate price list", description = "Activates a price list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PriceListDto> activatePriceList(
            @Parameter(description = "Price list ID") @PathVariable Long id) {
        return ResponseEntity.ok(priceListService.activatePriceList(id));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate price list", description = "Deactivates a price list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PriceListDto> deactivatePriceList(
            @Parameter(description = "Price list ID") @PathVariable Long id) {
        return ResponseEntity.ok(priceListService.deactivatePriceList(id));
    }

    // ==================== Price List Items ====================

    @GetMapping("/{priceListId}/items")
    @Operation(summary = "Get price list items", description = "Retrieves items in a price list with pagination")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<Page<PriceListItemDto>> getPriceListItems(
            @Parameter(description = "Price list ID") @PathVariable Long priceListId,
            Pageable pageable) {
        return ResponseEntity.ok(priceListService.findPriceListItems(priceListId, pageable));
    }

    @GetMapping("/{priceListId}/items/{itemId}")
    @Operation(summary = "Get price list item", description = "Retrieves a specific item from a price list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<PriceListItemDto> getPriceListItem(
            @Parameter(description = "Price list ID") @PathVariable Long priceListId,
            @Parameter(description = "Item ID") @PathVariable Long itemId) {
        return ResponseEntity.ok(priceListService.findPriceListItem(priceListId, itemId));
    }

    @PostMapping("/{priceListId}/items")
    @Operation(summary = "Add item to price list", description = "Adds a new item to a price list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PriceListItemDto> addPriceListItem(
            @Parameter(description = "Price list ID") @PathVariable Long priceListId,
            @Valid @RequestBody CreatePriceListItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(priceListService.addPriceListItem(priceListId, request));
    }

    @PutMapping("/{priceListId}/items/{itemId}")
    @Operation(summary = "Update price list item", description = "Updates an item in a price list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PriceListItemDto> updatePriceListItem(
            @Parameter(description = "Price list ID") @PathVariable Long priceListId,
            @Parameter(description = "Item ID") @PathVariable Long itemId,
            @Valid @RequestBody CreatePriceListItemRequest request) {
        return ResponseEntity.ok(priceListService.updatePriceListItem(priceListId, itemId, request));
    }

    @DeleteMapping("/{priceListId}/items/{itemId}")
    @Operation(summary = "Remove item from price list", description = "Removes an item from a price list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> removePriceListItem(
            @Parameter(description = "Price list ID") @PathVariable Long priceListId,
            @Parameter(description = "Item ID") @PathVariable Long itemId) {
        priceListService.removePriceListItem(priceListId, itemId);
        return ResponseEntity.noContent().build();
    }

    // ==================== Customer Price List Assignments ====================

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get customer price lists", description = "Retrieves price lists assigned to a customer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<List<PriceListDto>> getCustomerPriceLists(
            @Parameter(description = "Customer ID") @PathVariable Long customerId) {
        return ResponseEntity.ok(priceListService.findCustomerPriceLists(customerId));
    }

    @PostMapping("/{priceListId}/assign-customer/{customerId}")
    @Operation(summary = "Assign price list to customer", description = "Assigns a price list to a customer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> assignPriceListToCustomer(
            @Parameter(description = "Price list ID") @PathVariable Long priceListId,
            @Parameter(description = "Customer ID") @PathVariable Long customerId,
            @RequestParam(required = false) Integer priority) {
        priceListService.assignPriceListToCustomer(priceListId, customerId, priority);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{priceListId}/unassign-customer/{customerId}")
    @Operation(summary = "Remove price list from customer", description = "Removes a price list assignment from a customer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> removePriceListFromCustomer(
            @Parameter(description = "Price list ID") @PathVariable Long priceListId,
            @Parameter(description = "Customer ID") @PathVariable Long customerId) {
        priceListService.removePriceListFromCustomer(priceListId, customerId);
        return ResponseEntity.noContent().build();
    }
}
