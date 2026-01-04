package com.hisobnoma.platform.pos.controller;

import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.PromotionService;
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
@RequestMapping("/api/v1/pos/promotions")
@RequiredArgsConstructor
@Tag(name = "Promotions", description = "Promotion management APIs")
public class PromotionController {

    private final PromotionService promotionService;

    @GetMapping
    @Operation(summary = "Get all promotions", description = "Retrieves all promotions with pagination")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<Page<PromotionDto>> getAllPromotions(Pageable pageable) {
        return ResponseEntity.ok(promotionService.findAll(pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Search promotions", description = "Search promotions by query string")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<Page<PromotionDto>> searchPromotions(
            @Parameter(description = "Search query") @RequestParam String query,
            Pageable pageable) {
        return ResponseEntity.ok(promotionService.search(query, pageable));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active promotions", description = "Retrieves currently active promotions")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<List<PromotionDto>> getActivePromotions(
            @Parameter(description = "Location ID (optional)") @RequestParam(required = false) Long locationId) {
        return ResponseEntity.ok(promotionService.findActivePromotions(locationId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get promotion by ID", description = "Retrieves a promotion by its ID with full details")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<PromotionDto> getPromotionById(
            @Parameter(description = "Promotion ID") @PathVariable Long id) {
        return ResponseEntity.ok(promotionService.findById(id));
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get promotion by code", description = "Retrieves a promotion by its code")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'POS_OPERATOR')")
    public ResponseEntity<PromotionDto> getPromotionByCode(
            @Parameter(description = "Promotion code") @PathVariable String code) {
        return ResponseEntity.ok(promotionService.findByCode(code));
    }

    @PostMapping
    @Operation(summary = "Create promotion", description = "Creates a new promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PromotionDto> createPromotion(
            @Valid @RequestBody CreatePromotionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promotionService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update promotion", description = "Updates an existing promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PromotionDto> updatePromotion(
            @Parameter(description = "Promotion ID") @PathVariable Long id,
            @Valid @RequestBody CreatePromotionRequest request) {
        return ResponseEntity.ok(promotionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete promotion", description = "Deletes a promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Void> deletePromotion(
            @Parameter(description = "Promotion ID") @PathVariable Long id) {
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate promotion", description = "Activates a promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PromotionDto> activatePromotion(
            @Parameter(description = "Promotion ID") @PathVariable Long id) {
        return ResponseEntity.ok(promotionService.activate(id));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate promotion", description = "Deactivates a promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PromotionDto> deactivatePromotion(
            @Parameter(description = "Promotion ID") @PathVariable Long id) {
        return ResponseEntity.ok(promotionService.deactivate(id));
    }

    // ==================== Promotion Conditions ====================

    @PostMapping("/{promotionId}/conditions")
    @Operation(summary = "Add condition to promotion", description = "Adds a condition to a promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PromotionDto> addCondition(
            @Parameter(description = "Promotion ID") @PathVariable Long promotionId,
            @Valid @RequestBody CreatePromotionConditionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promotionService.addCondition(promotionId, request));
    }

    @DeleteMapping("/{promotionId}/conditions/{conditionId}")
    @Operation(summary = "Remove condition from promotion", description = "Removes a condition from a promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PromotionDto> removeCondition(
            @Parameter(description = "Promotion ID") @PathVariable Long promotionId,
            @Parameter(description = "Condition ID") @PathVariable Long conditionId) {
        return ResponseEntity.ok(promotionService.removeCondition(promotionId, conditionId));
    }

    // ==================== Promotion Actions ====================

    @PostMapping("/{promotionId}/actions")
    @Operation(summary = "Add action to promotion", description = "Adds an action to a promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PromotionDto> addAction(
            @Parameter(description = "Promotion ID") @PathVariable Long promotionId,
            @Valid @RequestBody CreatePromotionActionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(promotionService.addAction(promotionId, request));
    }

    @DeleteMapping("/{promotionId}/actions/{actionId}")
    @Operation(summary = "Remove action from promotion", description = "Removes an action from a promotion")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<PromotionDto> removeAction(
            @Parameter(description = "Promotion ID") @PathVariable Long promotionId,
            @Parameter(description = "Action ID") @PathVariable Long actionId) {
        return ResponseEntity.ok(promotionService.removeAction(promotionId, actionId));
    }
}
