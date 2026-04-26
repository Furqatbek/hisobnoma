package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.dto.ARAgingReportDto;
import com.hisobnoma.platform.finance.dto.CustomerBalanceReportDto;
import com.hisobnoma.platform.finance.entity.*;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.ARPaymentRepository;
import com.hisobnoma.platform.finance.repository.CreditNoteRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ARReportServiceTest {

    @Mock
    private ARInvoiceRepository arInvoiceRepository;

    @Mock
    private ARPaymentRepository arPaymentRepository;

    @Mock
    private CreditNoteRepository creditNoteRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ARReportService arReportService;

    private Customer customer;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .code("CUST-001")
                .name("Test Customer")
                .phone("123456")
                .creditLimit(new BigDecimal("200000"))
                .currentBalance(new BigDecimal("50000"))
                .creditHold(false)
                .paymentTermsDays(30)
                .tenantId(TENANT_ID)
                .build();
    }

    // ====== generateAgingReport ======

    @Test
    void generateAgingReport_withUnpaidInvoices_returnsReport() {
        // Given
        LocalDate asOfDate = LocalDate.of(2025, 3, 31);

        ARInvoice invoice = org.mockito.Mockito.mock(ARInvoice.class);
        when(invoice.getCustomer()).thenReturn(customer);
        when(invoice.getBalanceDue()).thenReturn(new BigDecimal("15000"));
        when(invoice.getDueDate()).thenReturn(LocalDate.of(2025, 3, 10));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByTenantIdAndStatusIn(eq(TENANT_ID), anyList()))
                .thenReturn(List.of(invoice));

        // When
        ARAgingReportDto result = arReportService.generateAgingReport(asOfDate);

        // Then
        assertNotNull(result);
        assertEquals(asOfDate, result.getReportDate());
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(new BigDecimal("15000"), result.getGrandTotal());
        assertEquals(1, result.getCustomerCount());
        assertFalse(result.getCustomerAgingList().isEmpty());
        assertEquals("CUST-001", result.getCustomerAgingList().get(0).getCustomerCode());
    }

    @Test
    void generateAgingReport_currentInvoice_classifiedAsCurrent() {
        // Given
        LocalDate asOfDate = LocalDate.of(2025, 3, 31);

        ARInvoice invoice = org.mockito.Mockito.mock(ARInvoice.class);
        when(invoice.getCustomer()).thenReturn(customer);
        when(invoice.getBalanceDue()).thenReturn(new BigDecimal("8000"));
        when(invoice.getDueDate()).thenReturn(LocalDate.of(2025, 4, 15)); // not yet due

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByTenantIdAndStatusIn(eq(TENANT_ID), anyList()))
                .thenReturn(List.of(invoice));

        // When
        ARAgingReportDto result = arReportService.generateAgingReport(asOfDate);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("8000"), result.getTotalCurrent());
        assertEquals(BigDecimal.ZERO, result.getTotal1To30Days());
    }

    @Test
    void generateAgingReport_over90Days_classifiedCorrectly() {
        // Given
        LocalDate asOfDate = LocalDate.of(2025, 3, 31);

        ARInvoice invoice = org.mockito.Mockito.mock(ARInvoice.class);
        when(invoice.getCustomer()).thenReturn(customer);
        when(invoice.getBalanceDue()).thenReturn(new BigDecimal("25000"));
        when(invoice.getDueDate()).thenReturn(LocalDate.of(2024, 12, 1)); // >90 days

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByTenantIdAndStatusIn(eq(TENANT_ID), anyList()))
                .thenReturn(List.of(invoice));

        // When
        ARAgingReportDto result = arReportService.generateAgingReport(asOfDate);

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("25000"), result.getTotalOver90Days());
    }

    @Test
    void generateAgingReport_noInvoices_returnsEmptyReport() {
        // Given
        LocalDate asOfDate = LocalDate.of(2025, 3, 31);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.findByTenantIdAndStatusIn(eq(TENANT_ID), anyList()))
                .thenReturn(Collections.emptyList());

        // When
        ARAgingReportDto result = arReportService.generateAgingReport(asOfDate);

        // Then
        assertNotNull(result);
        assertTrue(result.getCustomerAgingList().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getGrandTotal());
        assertEquals(0, result.getCustomerCount());
    }

    // ====== generateCustomerBalanceReport ======

    @Test
    void generateCustomerBalanceReport_withBalances_returnsReport() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(customer));
        when(arInvoiceRepository.sumBalanceDueByCustomer(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(new BigDecimal("80000"));
        when(creditNoteRepository.sumAvailableCreditByCustomer(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(new BigDecimal("5000"));
        when(arInvoiceRepository.findLatestInvoiceDateByCustomer(TENANT_ID, 1L))
                .thenReturn(LocalDate.of(2025, 3, 15));

        // When
        CustomerBalanceReportDto result = arReportService.generateCustomerBalanceReport();

        // Then
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(new BigDecimal("80000"), result.getTotalReceivables());
        assertEquals(new BigDecimal("5000"), result.getTotalCredits());
        assertEquals(1, result.getCustomerCount());
        assertFalse(result.getCustomerBalances().isEmpty());

        CustomerBalanceReportDto.CustomerBalanceDto balance = result.getCustomerBalances().get(0);
        assertEquals("CUST-001", balance.getCustomerCode());
        assertEquals(new BigDecimal("75000"), balance.getNetBalance());
        assertFalse(balance.isOverCreditLimit());
    }

    @Test
    void generateCustomerBalanceReport_overCreditLimit_flagged() {
        // Given
        customer.setCreditLimit(new BigDecimal("50000"));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(customer));
        when(arInvoiceRepository.sumBalanceDueByCustomer(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(new BigDecimal("60000"));
        when(creditNoteRepository.sumAvailableCreditByCustomer(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(arInvoiceRepository.findLatestInvoiceDateByCustomer(TENANT_ID, 1L))
                .thenReturn(null);

        // When
        CustomerBalanceReportDto result = arReportService.generateCustomerBalanceReport();

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCustomersOverCreditLimit());
        assertTrue(result.getCustomerBalances().get(0).isOverCreditLimit());
        assertEquals(BigDecimal.ZERO, result.getCustomerBalances().get(0).getAvailableCreditLimit());
    }

    @Test
    void generateCustomerBalanceReport_creditHold_counted() {
        // Given
        customer.setCreditHold(true);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(customer));
        when(arInvoiceRepository.sumBalanceDueByCustomer(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(new BigDecimal("10000"));
        when(creditNoteRepository.sumAvailableCreditByCustomer(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(arInvoiceRepository.findLatestInvoiceDateByCustomer(TENANT_ID, 1L))
                .thenReturn(null);

        // When
        CustomerBalanceReportDto result = arReportService.generateCustomerBalanceReport();

        // Then
        assertNotNull(result);
        assertEquals(1, result.getCustomersOnCreditHold());
        assertTrue(result.getCustomerBalances().get(0).isOnCreditHold());
    }

    @Test
    void generateCustomerBalanceReport_noCustomers_returnsEmptyReport() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

        // When
        CustomerBalanceReportDto result = arReportService.generateCustomerBalanceReport();

        // Then
        assertNotNull(result);
        assertTrue(result.getCustomerBalances().isEmpty());
        assertEquals(0, result.getCustomerCount());
    }

    // ====== getCustomerBalance ======

    @Test
    void getCustomerBalance_found_returnsBalance() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(arInvoiceRepository.sumBalanceDueByCustomer(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(new BigDecimal("30000"));
        when(creditNoteRepository.sumAvailableCreditByCustomer(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(new BigDecimal("5000"));

        // When
        CustomerBalanceReportDto.CustomerBalanceDto result = arReportService.getCustomerBalance(1L);

        // Then
        assertNotNull(result);
        assertEquals("CUST-001", result.getCustomerCode());
        assertEquals(new BigDecimal("30000"), result.getOutstandingInvoices());
        assertEquals(new BigDecimal("5000"), result.getAvailableCredits());
        assertEquals(new BigDecimal("25000"), result.getNetBalance());
    }

    @Test
    void getCustomerBalance_notFound_throwsRuntimeException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> arReportService.getCustomerBalance(999L));
    }

    @Test
    void getCustomerBalance_nullBalances_defaultsToZero() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(customerRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(customer));
        when(arInvoiceRepository.sumBalanceDueByCustomer(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(null);
        when(creditNoteRepository.sumAvailableCreditByCustomer(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(null);

        // When
        CustomerBalanceReportDto.CustomerBalanceDto result = arReportService.getCustomerBalance(1L);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getOutstandingInvoices());
        assertEquals(BigDecimal.ZERO, result.getAvailableCredits());
        assertEquals(BigDecimal.ZERO, result.getNetBalance());
    }
}
