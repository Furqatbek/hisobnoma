package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.expense.entity.ExpenseRecord;
import com.hisobnoma.platform.expense.service.ExpenseService;
import com.hisobnoma.platform.finance.dto.ARPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateARPaymentAllocationRequest;
import com.hisobnoma.platform.finance.dto.CreateARPaymentRequest;
import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import com.hisobnoma.platform.finance.entity.ARPaymentMethod;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.service.ARPaymentService;
import com.hisobnoma.platform.hr.dto.CreateSalaryAdvanceRequest;
import com.hisobnoma.platform.hr.dto.CreateSalaryRecordRequest;
import com.hisobnoma.platform.hr.dto.SalaryAdvanceDto;
import com.hisobnoma.platform.hr.dto.SalaryRecordDto;
import com.hisobnoma.platform.hr.service.SalaryAdvanceService;
import com.hisobnoma.platform.hr.service.SalaryService;
import com.hisobnoma.platform.mobile.dto.MobileDebtorPaymentRequest;
import com.hisobnoma.platform.mobile.dto.MobileExpenseRequest;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin write-actions from the mobile app: record an expense, accept a debtor (AR)
 * payment, and pay a salary/advance. Thin orchestration over the existing finance/HR
 * services — those already resolve tenant/user from the staff security context (which
 * the mobile JWT populates) and post to the GL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MobileAdminActionService {

    /** Invoice statuses a debtor payment may settle (posted, not fully paid). */
    private static final List<ARInvoiceStatus> OPEN_INVOICE_STATUSES = List.of(
            ARInvoiceStatus.PENDING, ARInvoiceStatus.SENT,
            ARInvoiceStatus.PARTIAL, ARInvoiceStatus.OVERDUE);

    private final SecurityContextHelper securityContextHelper;
    private final ExpenseService expenseService;
    private final ARPaymentService arPaymentService;
    private final ARInvoiceRepository arInvoiceRepository;
    private final SalaryService salaryService;
    private final SalaryAdvanceService salaryAdvanceService;

    // ---- 1. Expense ----

    @Transactional
    public ExpenseRecord recordExpense(MobileExpenseRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return expenseService.create(tenantId, request.getCreateDate(), request.getCategory(),
                request.getTotalAmount(), request.getCurrency(), request.getNotes(), null);
    }

    // ---- 2. Debtor (AR) payment ----

    /**
     * Records a completed (GL-posted) AR payment from a customer, allocated oldest-due-first
     * across their open invoices (or to {@code invoiceId} when supplied). Any amount beyond the
     * open balance stays on the payment as an unallocated advance.
     */
    @Transactional
    public ARPaymentDto collectDebtorPayment(MobileDebtorPaymentRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        List<CreateARPaymentAllocationRequest> allocations = new ArrayList<>();
        BigDecimal remaining = request.getAmount();

        List<ARInvoice> targets;
        if (request.getInvoiceId() != null) {
            ARInvoice invoice = arInvoiceRepository
                    .findByIdAndTenantId(request.getInvoiceId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("ARInvoice", request.getInvoiceId()));
            if (!request.getCustomerId().equals(invoice.getCustomer().getId())) {
                throw new BusinessException("Invoice does not belong to that customer",
                        "INVOICE_CUSTOMER_MISMATCH");
            }
            targets = List.of(invoice);
        } else {
            targets = arInvoiceRepository.findUnpaidByCustomer(
                    tenantId, request.getCustomerId(), OPEN_INVOICE_STATUSES);
        }

        for (ARInvoice invoice : targets) {
            if (remaining.signum() <= 0) break;
            BigDecimal due = invoice.getBalanceDue();
            if (due == null || due.signum() <= 0) continue;
            BigDecimal alloc = remaining.min(due);
            allocations.add(CreateARPaymentAllocationRequest.builder()
                    .arInvoiceId(invoice.getId())
                    .allocatedAmount(alloc)
                    .build());
            remaining = remaining.subtract(alloc);
        }

        ARPaymentMethod method = request.getPaymentMethod() != null
                ? request.getPaymentMethod() : ARPaymentMethod.CASH;

        ARPaymentDto payment = arPaymentService.createAndCompletePayment(CreateARPaymentRequest.builder()
                .customerId(request.getCustomerId())
                .paymentDate(LocalDate.now())
                .paymentMethod(method)
                .paymentAmount(request.getAmount())
                .referenceNumber(request.getReferenceNumber())
                .notes(request.getNotes())
                .allocations(allocations)
                .build());
        log.info("Mobile debtor payment {} for customer {} ({} invoice(s) settled, tenant {})",
                payment.getPaymentNumber(), request.getCustomerId(), allocations.size(), tenantId);
        return payment;
    }

    // ---- 3. Salary / advance ----

    /** Creates the salary record for the employee/period and immediately marks it PAID (posts to GL). */
    @Transactional
    public SalaryRecordDto paySalary(CreateSalaryRecordRequest request) {
        SalaryRecordDto record = salaryService.create(request);
        return salaryService.markPaid(record.getId());
    }

    /** Records a paid advance to an employee (posts to GL). */
    @Transactional
    public SalaryAdvanceDto recordAdvance(CreateSalaryAdvanceRequest request) {
        return salaryAdvanceService.create(request);
    }
}
