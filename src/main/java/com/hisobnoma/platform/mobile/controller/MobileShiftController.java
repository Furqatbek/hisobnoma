package com.hisobnoma.platform.mobile.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.ShiftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Mobile shift controller for POS shift management from mobile app.
 */
@RestController
@RequestMapping("/api/v1/mobile/shifts")
@RequiredArgsConstructor
@Tag(name = "Mobile Shifts", description = "Mobile POS shift management APIs")
public class MobileShiftController {

    private final ShiftService shiftService;

    @GetMapping("/current")
    @PreAuthorize("hasAnyAuthority('POS_SHIFT_READ', 'MOBILE_SYNC_ACCESS')")
    @Operation(summary = "Get current shift", description = "Get the current open shift for the authenticated user")
    public ResponseEntity<ApiResponse<ShiftDto>> getCurrentShift() {
        ShiftDto shift = shiftService.getCurrentShiftForUser();
        return ResponseEntity.ok(ApiResponse.success(shift));
    }

    @GetMapping("/open")
    @PreAuthorize("hasAnyAuthority('POS_SHIFT_READ', 'MOBILE_SYNC_ACCESS')")
    @Operation(summary = "Get open shifts", description = "Get all open shifts for the current tenant")
    public ResponseEntity<ApiResponse<List<ShiftDto>>> getOpenShifts() {
        List<ShiftDto> shifts = shiftService.findOpenShifts();
        return ResponseEntity.ok(ApiResponse.success(shifts));
    }

    @PostMapping("/open")
    @PreAuthorize("hasAnyAuthority('POS_SHIFT_OPEN', 'MOBILE_SYNC_ACCESS')")
    @Operation(summary = "Open a new shift", description = "Open a new POS shift on a terminal")
    public ResponseEntity<ApiResponse<ShiftDto>> openShift(@Valid @RequestBody OpenShiftRequest request) {
        ShiftDto shift = shiftService.openShift(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(shift, "Shift opened"));
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyAuthority('POS_SHIFT_CLOSE', 'MOBILE_SYNC_ACCESS')")
    @Operation(summary = "Close a shift", description = "Close an open POS shift")
    public ResponseEntity<ApiResponse<ShiftDto>> closeShift(
            @PathVariable Long id,
            @Valid @RequestBody CloseShiftRequest request) {
        ShiftDto shift = shiftService.closeShift(id, request);
        return ResponseEntity.ok(ApiResponse.success(shift, "Shift closed"));
    }

    @PostMapping("/{id}/cash-operation")
    @PreAuthorize("hasAnyAuthority('POS_SHIFT_CASH_OPERATION', 'MOBILE_SYNC_ACCESS')")
    @Operation(summary = "Record cash operation", description = "Record a cash in or cash out operation on a shift")
    public ResponseEntity<ApiResponse<ShiftDto>> cashOperation(
            @PathVariable Long id,
            @Valid @RequestBody CashOperationRequest request) {
        ShiftDto shift = shiftService.cashOperation(id, request);
        String message = request.getOperationType() == CashOperationRequest.OperationType.CASH_IN
                ? "Cash in recorded" : "Cash out recorded";
        return ResponseEntity.ok(ApiResponse.success(shift, message));
    }
}
