package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
import com.hisobnoma.platform.reports.dto.AgingReportDTO;
import com.hisobnoma.platform.reports.dto.GenerateReportRequest;
import com.hisobnoma.platform.reports.dto.IncomeStatementDTO;
import com.hisobnoma.platform.reports.dto.TrialBalanceReportDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancialReportServiceTest {

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private JournalLineRepository journalLineRepository;

    @Mock
    private ARInvoiceRepository arInvoiceRepository;

    @Mock
    private APInvoiceRepository apInvoiceRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private FiscalYearRepository fiscalYearRepository;

    @InjectMocks
    private FinancialReportService financialReportService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        // Common setup is minimal; per-test setup below
    }

    // ====== generateTrialBalanceReport ======

    @Test
    void generateTrialBalanceReport_withAccounts_returnsBalancedReport() {
        // Given
        GenerateReportRequest request = GenerateReportRequest.builder()
                .endDate(LocalDate.of(2025, 3, 31))
                .build();

        Account assetAccount = Account.builder()
                .id(1L)
                .code("1000")
                .name("Cash")
                .accountType(AccountType.ASSET)
                .normalBalance(NormalBalance.DEBIT)
                .openingBalance(new BigDecimal("5000.00"))
                .allowsDirectPosting(true)
                .accountLevel(1)
                .tenantId(TENANT_ID)
                .build();

        Account revenueAccount = Account.builder()
                .id(2L)
                .code("4000")
                .name("Sales Revenue")
                .accountType(AccountType.REVENUE)
                .normalBalance(NormalBalance.CREDIT)
                .openingBalance(BigDecimal.ZERO)
                .allowsDirectPosting(true)
                .accountLevel(1)
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(fiscalYearRepository.findByDateAndTenantId(any(LocalDate.class), eq(TENANT_ID))).thenReturn(Optional.empty());
        when(accountRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(assetAccount, revenueAccount));

        // Asset: cumulative through asOfDate
        when(journalLineRepository.sumDebitByAccountAsOf(eq(1L), eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(new BigDecimal("10000.00"));
        when(journalLineRepository.sumCreditByAccountAsOf(eq(1L), eq(TENANT_ID), any(LocalDate.class)))
                .thenReturn(new BigDecimal("3000.00"));

        // Revenue: within fiscal year
        when(journalLineRepository.sumDebitByAccountAndDateRange(eq(2L), eq(TENANT_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(BigDecimal.ZERO);
        when(journalLineRepository.sumCreditByAccountAndDateRange(eq(2L), eq(TENANT_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("12000.00"));

        // When
        TrialBalanceReportDTO result = financialReportService.generateTrialBalanceReport(request);

        // Then
        assertNotNull(result);
        assertNotNull(result.getMetadata());
        assertEquals("Trial Balance", result.getMetadata().getReportName());
        assertNotNull(result.getAccounts());
        assertFalse(result.getAccounts().isEmpty());
        assertNotNull(result.getTotals());
    }

    @Test
    void generateTrialBalanceReport_noAccounts_returnsEmptyReport() {
        // Given
        GenerateReportRequest request = GenerateReportRequest.builder().build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());
        when(fiscalYearRepository.findByDateAndTenantId(any(LocalDate.class), eq(TENANT_ID))).thenReturn(Optional.empty());
        when(accountRepository.findAllByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

        // When
        TrialBalanceReportDTO result = financialReportService.generateTrialBalanceReport(request);

        // Then
        assertNotNull(result);
        assertTrue(result.getAccounts().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getTotals().getTotalDebits());
        assertEquals(BigDecimal.ZERO, result.getTotals().getTotalCredits());
        assertTrue(result.getTotals().isBalanced());
    }

    // ====== generateIncomeStatement ======

    @Test
    void generateIncomeStatement_withRevenueAndExpense_returnsStatement() {
        // Given
        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 3, 31))
                .build();

        Account revenueAccount = Account.builder()
                .id(1L)
                .code("4000")
                .name("Sales Revenue")
                .accountType(AccountType.REVENUE)
                .normalBalance(NormalBalance.CREDIT)
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        Account expenseAccount = Account.builder()
                .id(2L)
                .code("5000")
                .name("Cost of Goods Sold")
                .accountType(AccountType.EXPENSE)
                .normalBalance(NormalBalance.DEBIT)
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(new java.util.ArrayList<>(List.of(revenueAccount, expenseAccount)));

        // Revenue: credits - debits
        when(journalLineRepository.sumDebitByAccountAndDateRange(eq(1L), eq(TENANT_ID), any(), any())).thenReturn(BigDecimal.ZERO);
        when(journalLineRepository.sumCreditByAccountAndDateRange(eq(1L), eq(TENANT_ID), any(), any())).thenReturn(new BigDecimal("50000.00"));

        // Expense: debits - credits
        when(journalLineRepository.sumDebitByAccountAndDateRange(eq(2L), eq(TENANT_ID), any(), any())).thenReturn(new BigDecimal("30000.00"));
        when(journalLineRepository.sumCreditByAccountAndDateRange(eq(2L), eq(TENANT_ID), any(), any())).thenReturn(BigDecimal.ZERO);

        // When
        IncomeStatementDTO result = financialReportService.generateIncomeStatement(request);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getRevenueItems().size());
        assertEquals(1, result.getExpenseItems().size());
        assertEquals(new BigDecimal("50000.00"), result.getSummary().getTotalRevenue());
        assertEquals(new BigDecimal("30000.00"), result.getSummary().getTotalExpenses());
        assertEquals(new BigDecimal("20000.00"), result.getSummary().getNetIncome());
    }

    @Test
    void generateIncomeStatement_noActivity_returnsEmptyStatement() {
        // Given
        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 3, 31))
                .build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(accountRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

        // When
        IncomeStatementDTO result = financialReportService.generateIncomeStatement(request);

        // Then
        assertNotNull(result);
        assertTrue(result.getRevenueItems().isEmpty());
        assertTrue(result.getExpenseItems().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getSummary().getNetIncome());
    }

    // ====== generateARAgingReport ======

    @Test
    void generateARAgingReport_withInvoices_returnsAgingDetails() {
        // Given
        GenerateReportRequest request = GenerateReportRequest.builder()
                .endDate(LocalDate.of(2025, 3, 31))
                .build();

        Customer customer = Customer.builder()
                .id(1L)
                .code("CUST-001")
                .name("Test Customer")
                .phone("123456")
                .creditLimit(new BigDecimal("100000"))
                .build();

        ARInvoice invoice = mock(ARInvoice.class);
        when(invoice.getCustomer()).thenReturn(customer);
        when(invoice.getBalanceDue()).thenReturn(new BigDecimal("5000.00"));
        when(invoice.getExchangeRate()).thenReturn(null);
        when(invoice.getDueDate()).thenReturn(LocalDate.of(2025, 2, 15));
        when(invoice.getInvoiceDate()).thenReturn(LocalDate.of(2025, 1, 15));

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findUnpaidInvoicesAsOf(eq(TENANT_ID), any(LocalDate.class), anyList()))
                .thenReturn(List.of(invoice));
        when(customerRepository.findAllById(anyCollection())).thenReturn(List.of(customer));

        // When
        AgingReportDTO result = financialReportService.generateARAgingReport(request);

        // Then
        assertNotNull(result);
        assertEquals("Accounts Receivable Aging Report", result.getMetadata().getReportName());
        assertEquals("AR", result.getMetadata().getReportType());
        assertFalse(result.getDetails().isEmpty());
        assertEquals(1, result.getSummary().getTotalAccounts());
    }

    @Test
    void generateARAgingReport_noInvoices_returnsEmptyReport() {
        // Given
        GenerateReportRequest request = GenerateReportRequest.builder().build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());
        when(arInvoiceRepository.findUnpaidInvoicesAsOf(eq(TENANT_ID), any(LocalDate.class), anyList()))
                .thenReturn(Collections.emptyList());

        // When
        AgingReportDTO result = financialReportService.generateARAgingReport(request);

        // Then
        assertNotNull(result);
        assertTrue(result.getDetails().isEmpty());
        assertEquals(0, result.getSummary().getTotalAccounts());
        assertEquals(BigDecimal.ZERO, result.getSummary().getTotalOutstanding());
    }

    // ====== generateAPAgingReport ======

    @Test
    void generateAPAgingReport_withInvoices_returnsAgingDetails() {
        // Given
        GenerateReportRequest request = GenerateReportRequest.builder()
                .endDate(LocalDate.of(2025, 3, 31))
                .build();

        Vendor vendor = Vendor.builder()
                .id(1L)
                .code("VEND-001")
                .name("Test Vendor")
                .phone("654321")
                .creditLimit(new BigDecimal("200000"))
                .build();

        APInvoice invoice = mock(APInvoice.class);
        when(invoice.getVendorId()).thenReturn(1L);
        when(invoice.getBalanceDue()).thenReturn(new BigDecimal("8000.00"));
        when(invoice.getExchangeRate()).thenReturn(null);
        when(invoice.getDueDate()).thenReturn(LocalDate.of(2025, 2, 1));
        when(invoice.getInvoiceDate()).thenReturn(LocalDate.of(2025, 1, 1));

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findUnpaidInvoicesAsOf(eq(TENANT_ID), any(LocalDate.class), anyList()))
                .thenReturn(List.of(invoice));
        when(vendorRepository.findAllById(anyCollection())).thenReturn(List.of(vendor));

        // When
        AgingReportDTO result = financialReportService.generateAPAgingReport(request);

        // Then
        assertNotNull(result);
        assertEquals("Accounts Payable Aging Report", result.getMetadata().getReportName());
        assertEquals("AP", result.getMetadata().getReportType());
        assertFalse(result.getDetails().isEmpty());
        assertEquals(1, result.getSummary().getTotalAccounts());
    }

    @Test
    void generateAPAgingReport_noInvoices_returnsEmptyReport() {
        // Given
        GenerateReportRequest request = GenerateReportRequest.builder().build();

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(tenantRepository.findById(TENANT_ID)).thenReturn(Optional.empty());
        when(apInvoiceRepository.findUnpaidInvoicesAsOf(eq(TENANT_ID), any(LocalDate.class), anyList()))
                .thenReturn(Collections.emptyList());

        // When
        AgingReportDTO result = financialReportService.generateAPAgingReport(request);

        // Then
        assertNotNull(result);
        assertTrue(result.getDetails().isEmpty());
        assertEquals(0, result.getSummary().getTotalAccounts());
        assertEquals(BigDecimal.ZERO, result.getSummary().getTotalOutstanding());
    }
}
