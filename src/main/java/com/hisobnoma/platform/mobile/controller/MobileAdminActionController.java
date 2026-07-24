package com.hisobnoma.platform.mobile.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.expense.entity.ExpenseRecord;
import com.hisobnoma.platform.finance.dto.ARPaymentDto;
import com.hisobnoma.platform.hr.dto.CreateSalaryAdvanceRequest;
import com.hisobnoma.platform.hr.dto.CreateSalaryRecordRequest;
import com.hisobnoma.platform.hr.dto.SalaryAdvanceDto;
import com.hisobnoma.platform.hr.dto.SalaryRecordDto;
import com.hisobnoma.platform.mobile.dto.MobileDebtorPaymentRequest;
import com.hisobnoma.platform.mobile.dto.MobileExpenseRequest;
import com.hisobnoma.platform.mobile.service.MobileAdminActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Mobile admin write-actions: record an expense, accept a debtor (AR) payment, and
 * pay a salary/advance. Each endpoint accepts the mobile-specific permission OR the
 * underlying module permission (mirrors the other mobile endpoints).
 */
@Tag(name = "Mobile Admin Actions")
@RestController
@RequestMapping("/api/v1/mobile")
@RequiredArgsConstructor
public class MobileAdminActionController {

    private final MobileAdminActionService service;

    @PostMapping("/expenses")
    @PreAuthorize("hasAnyAuthority('MOBILE_EXPENSE_WRITE')")
    @Operation(summary = "Record an expense")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recordExpense(
            @Valid @RequestBody MobileExpenseRequest request) {
        ExpenseRecord saved = service.recordExpense(request);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("id", saved.getId(), "totalAmount", saved.getTotalAmount()), "Expense recorded"));
    }

    @PostMapping("/finance/debtor-payment")
    @PreAuthorize("hasAnyAuthority('MOBILE_AR_COLLECT', 'FINANCE_AR_WRITE')")
    @Operation(summary = "Accept and register a debtor (customer) payment")
    public ResponseEntity<ApiResponse<ARPaymentDto>> collectDebtorPayment(
            @Valid @RequestBody MobileDebtorPaymentRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                service.collectDebtorPayment(request), "Payment recorded"));
    }

    @PostMapping("/hr/salary")
    @PreAuthorize("hasAnyAuthority('MOBILE_SALARY_PAY', 'HR_SALARY_WRITE')")
    @Operation(summary = "Record and pay a salary (create + mark paid, posts to GL)")
    public ResponseEntity<ApiResponse<SalaryRecordDto>> paySalary(
            @Valid @RequestBody CreateSalaryRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.paySalary(request), "Salary paid"));
    }

    @PostMapping("/hr/advance")
    @PreAuthorize("hasAnyAuthority('MOBILE_SALARY_PAY', 'HR_SALARY_WRITE')")
    @Operation(summary = "Record a paid advance to an employee (posts to GL)")
    public ResponseEntity<ApiResponse<SalaryAdvanceDto>> recordAdvance(
            @Valid @RequestBody CreateSalaryAdvanceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.recordAdvance(request), "Advance recorded"));
    }
}
