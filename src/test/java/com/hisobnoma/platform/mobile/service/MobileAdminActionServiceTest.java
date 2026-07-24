package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.expense.service.ExpenseService;
import com.hisobnoma.platform.finance.dto.ARPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateARPaymentRequest;
import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.entity.ARPaymentMethod;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.service.ARPaymentService;
import com.hisobnoma.platform.hr.dto.CreateSalaryRecordRequest;
import com.hisobnoma.platform.hr.dto.SalaryRecordDto;
import com.hisobnoma.platform.hr.service.SalaryAdvanceService;
import com.hisobnoma.platform.hr.service.SalaryService;
import com.hisobnoma.platform.mobile.dto.MobileDebtorPaymentRequest;
import com.hisobnoma.platform.mobile.dto.MobileExpenseRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileAdminActionServiceTest {

    private static final Long TENANT = 1L;

    @Mock private SecurityContextHelper securityContextHelper;
    @Mock private ExpenseService expenseService;
    @Mock private ARPaymentService arPaymentService;
    @Mock private ARInvoiceRepository arInvoiceRepository;
    @Mock private SalaryService salaryService;
    @Mock private SalaryAdvanceService salaryAdvanceService;

    @InjectMocks private MobileAdminActionService service;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT);
    }

    private ARInvoice invoice(long id, long customerId, String balance) {
        Customer c = Customer.builder().build();
        c.setId(customerId);
        ARInvoice inv = ARInvoice.builder().customer(c)
                .balanceDue(new BigDecimal(balance)).totalAmount(new BigDecimal(balance)).build();
        inv.setId(id);
        return inv;
    }

    // ---- expense ----

    @Test
    void recordExpense_delegatesToExpenseServiceWithTenant() {
        MobileExpenseRequest req = new MobileExpenseRequest();
        req.setTotalAmount(new BigDecimal("50000"));
        req.setCategory("Transport");

        service.recordExpense(req);

        verify(expenseService).create(eq(TENANT), isNull(), eq("Transport"),
                eq(new BigDecimal("50000")), isNull(), isNull(), isNull());
    }

    // ---- debtor payment ----

    @Test
    void collectDebtorPayment_allocatesOldestFirst() {
        MobileDebtorPaymentRequest req = new MobileDebtorPaymentRequest();
        req.setCustomerId(100L);
        req.setAmount(new BigDecimal("60000"));
        when(arInvoiceRepository.findUnpaidByCustomer(eq(TENANT), eq(100L), anyList()))
                .thenReturn(List.of(invoice(71L, 100L, "30000"), invoice(72L, 100L, "50000")));
        when(arPaymentService.createAndCompletePayment(any()))
                .thenReturn(ARPaymentDto.builder().id(900L).paymentNumber("PAY-900").build());

        service.collectDebtorPayment(req);

        ArgumentCaptor<CreateARPaymentRequest> cap = ArgumentCaptor.forClass(CreateARPaymentRequest.class);
        verify(arPaymentService).createAndCompletePayment(cap.capture());
        CreateARPaymentRequest sent = cap.getValue();
        assertEquals(100L, sent.getCustomerId());
        assertEquals(ARPaymentMethod.CASH, sent.getPaymentMethod());
        assertEquals(2, sent.getAllocations().size());
        assertEquals(71L, sent.getAllocations().get(0).getArInvoiceId());
        assertEquals(0, new BigDecimal("30000").compareTo(sent.getAllocations().get(0).getAllocatedAmount()));
        assertEquals(0, new BigDecimal("30000").compareTo(sent.getAllocations().get(1).getAllocatedAmount()));
    }

    @Test
    void collectDebtorPayment_excessBeyondBalance_leavesUnallocated() {
        MobileDebtorPaymentRequest req = new MobileDebtorPaymentRequest();
        req.setCustomerId(100L);
        req.setAmount(new BigDecimal("50000"));
        req.setPaymentMethod(ARPaymentMethod.MOBILE_PAYMENT);
        when(arInvoiceRepository.findUnpaidByCustomer(eq(TENANT), eq(100L), anyList()))
                .thenReturn(List.of(invoice(71L, 100L, "20000")));
        when(arPaymentService.createAndCompletePayment(any()))
                .thenReturn(ARPaymentDto.builder().id(901L).paymentNumber("PAY-901").build());

        service.collectDebtorPayment(req);

        ArgumentCaptor<CreateARPaymentRequest> cap = ArgumentCaptor.forClass(CreateARPaymentRequest.class);
        verify(arPaymentService).createAndCompletePayment(cap.capture());
        assertEquals(1, cap.getValue().getAllocations().size());
        assertEquals(0, new BigDecimal("20000")
                .compareTo(cap.getValue().getAllocations().get(0).getAllocatedAmount()));
        assertEquals(ARPaymentMethod.MOBILE_PAYMENT, cap.getValue().getPaymentMethod());
    }

    @Test
    void collectDebtorPayment_explicitInvoice_targetsThatInvoiceOnly() {
        MobileDebtorPaymentRequest req = new MobileDebtorPaymentRequest();
        req.setCustomerId(100L);
        req.setAmount(new BigDecimal("15000"));
        req.setInvoiceId(72L);
        when(arInvoiceRepository.findByIdAndTenantId(72L, TENANT))
                .thenReturn(Optional.of(invoice(72L, 100L, "50000")));
        when(arPaymentService.createAndCompletePayment(any()))
                .thenReturn(ARPaymentDto.builder().id(902L).build());

        service.collectDebtorPayment(req);

        ArgumentCaptor<CreateARPaymentRequest> cap = ArgumentCaptor.forClass(CreateARPaymentRequest.class);
        verify(arPaymentService).createAndCompletePayment(cap.capture());
        assertEquals(1, cap.getValue().getAllocations().size());
        assertEquals(72L, cap.getValue().getAllocations().get(0).getArInvoiceId());
        verify(arInvoiceRepository, never()).findUnpaidByCustomer(any(), any(), anyList());
    }

    @Test
    void collectDebtorPayment_explicitInvoiceOfAnotherCustomer_rejected() {
        MobileDebtorPaymentRequest req = new MobileDebtorPaymentRequest();
        req.setCustomerId(100L);
        req.setAmount(new BigDecimal("15000"));
        req.setInvoiceId(72L);
        when(arInvoiceRepository.findByIdAndTenantId(72L, TENANT))
                .thenReturn(Optional.of(invoice(72L, 999L, "50000")));

        assertThrows(BusinessException.class, () -> service.collectDebtorPayment(req));
        verify(arPaymentService, never()).createAndCompletePayment(any());
    }

    @Test
    void collectDebtorPayment_unknownInvoice_notFound() {
        MobileDebtorPaymentRequest req = new MobileDebtorPaymentRequest();
        req.setCustomerId(100L);
        req.setAmount(new BigDecimal("15000"));
        req.setInvoiceId(72L);
        when(arInvoiceRepository.findByIdAndTenantId(72L, TENANT)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.collectDebtorPayment(req));
    }

    // ---- salary / advance ----

    @Test
    void paySalary_createsThenMarksPaid() {
        CreateSalaryRecordRequest req = new CreateSalaryRecordRequest();
        SalaryRecordDto created = new SalaryRecordDto();
        created.setId(55L);
        when(salaryService.create(req)).thenReturn(created);
        SalaryRecordDto paid = new SalaryRecordDto();
        paid.setId(55L);
        paid.setStatus("PAID");
        when(salaryService.markPaid(55L)).thenReturn(paid);

        SalaryRecordDto result = service.paySalary(req);

        assertEquals("PAID", result.getStatus());
        var inOrder = inOrder(salaryService);
        inOrder.verify(salaryService).create(req);
        inOrder.verify(salaryService).markPaid(55L);
    }

    @Mock private com.hisobnoma.platform.hr.service.EmployeeService employeeService;

    @Test
    void listEmployeesForPicker_mapsToLightweightOptions() {
        com.hisobnoma.platform.hr.dto.EmployeeDto e = new com.hisobnoma.platform.hr.dto.EmployeeDto();
        e.setId(7L);
        e.setFullName("Ali Valiyev");
        e.setPositionName("Cashier");
        e.setEmployeeCode("EMP-007");
        when(employeeService.getActive()).thenReturn(List.of(e));

        var options = service.listEmployeesForPicker();

        assertEquals(1, options.size());
        assertEquals(7L, options.get(0).getId());
        assertEquals("Ali Valiyev", options.get(0).getName());
        assertEquals("Cashier", options.get(0).getPosition());
        assertEquals("EMP-007", options.get(0).getCode());
    }

    @Test
    void recordAdvance_delegatesToAdvanceService() {
        CreateSalaryAdvanceRequestStub req = new CreateSalaryAdvanceRequestStub();
        service.recordAdvance(req);
        verify(salaryAdvanceService).create(req);
    }

    /** Minimal alias so we don't import the concrete request twice. */
    static class CreateSalaryAdvanceRequestStub extends com.hisobnoma.platform.hr.dto.CreateSalaryAdvanceRequest {
    }
}
