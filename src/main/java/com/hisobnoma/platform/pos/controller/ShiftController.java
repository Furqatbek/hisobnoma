package com.hisobnoma.platform.pos.controller;

import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.pos.dto.*;
import com.hisobnoma.platform.pos.service.ShiftService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pos/shifts")
@RequiredArgsConstructor
public class ShiftController {

    private final ShiftService shiftService;

    @GetMapping
    @RequiresPermission("POS_SHIFT_READ")
    public ResponseEntity<PageResponse<ShiftDto>> getAll(
            @PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ShiftDto> page = shiftService.findAll(pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/{id}")
    @RequiresPermission("POS_SHIFT_READ")
    public ResponseEntity<ApiResponse<ShiftDto>> getById(@PathVariable Long id) {
        ShiftDto shift = shiftService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(shift));
    }

    @GetMapping("/terminal/{terminalId}")
    @RequiresPermission("POS_SHIFT_READ")
    public ResponseEntity<PageResponse<ShiftDto>> getByTerminal(
            @PathVariable Long terminalId,
            @PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ShiftDto> page = shiftService.findByTerminal(terminalId, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/cashier/{cashierId}")
    @RequiresPermission("POS_SHIFT_READ")
    public ResponseEntity<PageResponse<ShiftDto>> getByCashier(
            @PathVariable Long cashierId,
            @PageableDefault(size = 20, sort = "openedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ShiftDto> page = shiftService.findByCashier(cashierId, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }

    @GetMapping("/current")
    @RequiresPermission("POS_SHIFT_READ")
    public ResponseEntity<ApiResponse<ShiftDto>> getCurrentShift() {
        ShiftDto shift = shiftService.getCurrentShiftForUser();
        return ResponseEntity.ok(ApiResponse.success(shift));
    }

    @GetMapping("/current/terminal/{terminalId}")
    @RequiresPermission("POS_SHIFT_READ")
    public ResponseEntity<ApiResponse<ShiftDto>> getCurrentShiftForTerminal(@PathVariable Long terminalId) {
        ShiftDto shift = shiftService.getCurrentShift(terminalId);
        return ResponseEntity.ok(ApiResponse.success(shift));
    }

    @GetMapping("/open")
    @RequiresPermission("POS_SHIFT_READ")
    public ResponseEntity<ApiResponse<List<ShiftDto>>> getOpenShifts() {
        List<ShiftDto> shifts = shiftService.findOpenShifts();
        return ResponseEntity.ok(ApiResponse.success(shifts));
    }

    @PostMapping("/open")
    @RequiresPermission("POS_SHIFT_OPEN")
    public ResponseEntity<ApiResponse<ShiftDto>> openShift(@Valid @RequestBody OpenShiftRequest request) {
        ShiftDto shift = shiftService.openShift(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(shift, "Shift opened"));
    }

    @PostMapping("/{id}/close")
    @RequiresPermission("POS_SHIFT_CLOSE")
    public ResponseEntity<ApiResponse<ShiftDto>> closeShift(
            @PathVariable Long id,
            @Valid @RequestBody CloseShiftRequest request) {
        ShiftDto shift = shiftService.closeShift(id, request);
        return ResponseEntity.ok(ApiResponse.success(shift, "Shift closed"));
    }

    @PostMapping("/{id}/cash-operation")
    @RequiresPermission("POS_SHIFT_CASH_OPERATION")
    public ResponseEntity<ApiResponse<ShiftDto>> cashOperation(
            @PathVariable Long id,
            @Valid @RequestBody CashOperationRequest request) {
        ShiftDto shift = shiftService.cashOperation(id, request);
        String message = request.getOperationType() == CashOperationRequest.OperationType.CASH_IN
                ? "Cash in recorded" : "Cash out recorded";
        return ResponseEntity.ok(ApiResponse.success(shift, message));
    }

    @GetMapping("/{id}/cash-operations")
    @RequiresPermission("POS_SHIFT_READ")
    public ResponseEntity<ApiResponse<List<CashOperationDto>>> getCashOperations(@PathVariable Long id) {
        List<CashOperationDto> operations = shiftService.findCashOperationsByShift(id);
        return ResponseEntity.ok(ApiResponse.success(operations));
    }

    @PostMapping("/{id}/reconcile")
    @RequiresPermission("POS_SHIFT_CLOSE")
    public ResponseEntity<ApiResponse<ShiftDto>> reconcileShift(@PathVariable Long id) {
        ShiftDto shift = shiftService.reconcileShift(id);
        return ResponseEntity.ok(ApiResponse.success(shift, "Shift reconciled"));
    }
}
