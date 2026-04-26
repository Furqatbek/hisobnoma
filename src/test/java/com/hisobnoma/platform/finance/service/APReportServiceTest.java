package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.dto.APAgingReportDto;
import com.hisobnoma.platform.finance.dto.VendorBalanceReportDto;
import com.hisobnoma.platform.finance.entity.APInvoice;
import com.hisobnoma.platform.finance.entity.APInvoiceStatus;
import com.hisobnoma.platform.finance.entity.APPaymentStatus;
import com.hisobnoma.platform.finance.repository.APInvoiceRepository;
import com.hisobnoma.platform.finance.repository.APPaymentRepository;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
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
class APReportServiceTest {

    @Mock
    private APInvoiceRepository apInvoiceRepository;

    @Mock
    private APPaymentRepository apPaymentRepository;

    @Mock
    private VendorRepository vendorRepository;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private APReportService apReportService;

    private Vendor vendor;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder()
                .id(1L)
                .code("VEND-001")
                .name("Test Vendor")
                .contactPerson("John Doe")
                .email("vendor@test.com")
                .phone("123456")
                .creditLimit(new BigDecimal("500000"))
                .tenantId(TENANT_ID)
                .build();
    }

    // ====== generateAgingReport ======

    @Test
    void generateAgingReport_withUnpaidInvoices_returnsReport() {
        // Given
        APInvoice invoice = mock(APInvoice.class);
        when(invoice.getVendorId()).thenReturn(1L);
        when(invoice.getBalanceDue()).thenReturn(new BigDecimal("10000"));
        when(invoice.getDaysOverdue()).thenReturn(15);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findUnpaidInvoices(eq(TENANT_ID), anyList()))
                .thenReturn(List.of(invoice));
        when(vendorRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(vendor));

        // When
        APAgingReportDto result = apReportService.generateAgingReport();

        // Then
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(new BigDecimal("10000"), result.getTotalPayable());
        assertEquals(1, result.getVendorAging().size());
        assertEquals("VEND-001", result.getVendorAging().get(0).getVendorCode());
        assertEquals(new BigDecimal("10000"), result.getVendorAging().get(0).getOverdue1To30());
    }

    @Test
    void generateAgingReport_currentInvoice_classifiedAsCurrent() {
        // Given
        APInvoice invoice = mock(APInvoice.class);
        when(invoice.getVendorId()).thenReturn(1L);
        when(invoice.getBalanceDue()).thenReturn(new BigDecimal("5000"));
        when(invoice.getDaysOverdue()).thenReturn(-5); // not yet due

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findUnpaidInvoices(eq(TENANT_ID), anyList()))
                .thenReturn(List.of(invoice));
        when(vendorRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(vendor));

        // When
        APAgingReportDto result = apReportService.generateAgingReport();

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("5000"), result.getCurrent());
        assertEquals(BigDecimal.ZERO, result.getOverdue1To30());
    }

    @Test
    void generateAgingReport_over90DaysInvoice_classifiedCorrectly() {
        // Given
        APInvoice invoice = mock(APInvoice.class);
        when(invoice.getVendorId()).thenReturn(1L);
        when(invoice.getBalanceDue()).thenReturn(new BigDecimal("20000"));
        when(invoice.getDaysOverdue()).thenReturn(120);

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findUnpaidInvoices(eq(TENANT_ID), anyList()))
                .thenReturn(List.of(invoice));
        when(vendorRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(vendor));

        // When
        APAgingReportDto result = apReportService.generateAgingReport();

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("20000"), result.getOverdueOver90());
    }

    @Test
    void generateAgingReport_noInvoices_returnsEmptyReport() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(apInvoiceRepository.findUnpaidInvoices(eq(TENANT_ID), anyList()))
                .thenReturn(Collections.emptyList());
        when(vendorRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(vendor));

        // When
        APAgingReportDto result = apReportService.generateAgingReport();

        // Then
        assertNotNull(result);
        assertTrue(result.getVendorAging().isEmpty());
        assertEquals(BigDecimal.ZERO, result.getTotalPayable());
    }

    // ====== generateVendorBalanceReport ======

    @Test
    void generateVendorBalanceReport_withBalances_returnsReport() {
        // Given
        APInvoice openInvoice = mock(APInvoice.class);
        when(openInvoice.getInvoiceDate()).thenReturn(LocalDate.of(2025, 3, 1));

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(vendor));
        when(apInvoiceRepository.sumBalanceDueByVendor(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(new BigDecimal("100000"));
        when(apPaymentRepository.sumPaymentsByVendorAndStatus(TENANT_ID, 1L, APPaymentStatus.COMPLETED))
                .thenReturn(new BigDecimal("50000"));
        when(apInvoiceRepository.findUnpaidInvoicesByVendor(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(List.of(openInvoice));

        // When
        VendorBalanceReportDto result = apReportService.generateVendorBalanceReport();

        // Then
        assertNotNull(result);
        assertEquals(TENANT_ID, result.getTenantId());
        assertEquals(new BigDecimal("100000"), result.getTotalPayable());
        assertEquals(new BigDecimal("50000"), result.getTotalPayments());
        assertEquals(1, result.getVendorCount());
        assertFalse(result.getVendorBalances().isEmpty());

        VendorBalanceReportDto.VendorBalanceDto balance = result.getVendorBalances().get(0);
        assertEquals("VEND-001", balance.getVendorCode());
        assertEquals(new BigDecimal("100000"), balance.getCurrentBalance());
        assertEquals(new BigDecimal("400000"), balance.getAvailableCredit());
    }

    @Test
    void generateVendorBalanceReport_noVendors_returnsEmptyReport() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(Collections.emptyList());

        // When
        VendorBalanceReportDto result = apReportService.generateVendorBalanceReport();

        // Then
        assertNotNull(result);
        assertTrue(result.getVendorBalances().isEmpty());
        assertEquals(0, result.getVendorCount());
    }

    @Test
    void generateVendorBalanceReport_zeroBalance_filteredOut() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(vendor));
        when(apInvoiceRepository.sumBalanceDueByVendor(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(BigDecimal.ZERO);
        when(apPaymentRepository.sumPaymentsByVendorAndStatus(TENANT_ID, 1L, APPaymentStatus.COMPLETED))
                .thenReturn(BigDecimal.ZERO);
        when(apInvoiceRepository.findUnpaidInvoicesByVendor(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(Collections.emptyList());

        // When
        VendorBalanceReportDto result = apReportService.generateVendorBalanceReport();

        // Then
        assertNotNull(result);
        assertTrue(result.getVendorBalances().isEmpty());
    }

    // ====== getVendorStatement ======

    @Test
    void getVendorStatement_found_returnsBalance() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(apInvoiceRepository.sumBalanceDueByVendor(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(new BigDecimal("80000"));
        when(apPaymentRepository.sumPaymentsByVendorAndStatus(TENANT_ID, 1L, APPaymentStatus.COMPLETED))
                .thenReturn(new BigDecimal("40000"));
        when(apInvoiceRepository.findUnpaidInvoicesByVendor(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(Collections.emptyList());

        // When
        VendorBalanceReportDto.VendorBalanceDto result = apReportService.getVendorStatement(1L);

        // Then
        assertNotNull(result);
        assertEquals("VEND-001", result.getVendorCode());
        assertEquals(new BigDecimal("80000"), result.getCurrentBalance());
        assertEquals(new BigDecimal("40000"), result.getTotalPaid());
        assertEquals(new BigDecimal("120000"), result.getTotalInvoiced());
    }

    @Test
    void getVendorStatement_notFound_throwsRuntimeException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(RuntimeException.class, () -> apReportService.getVendorStatement(999L));
    }

    @Test
    void getVendorStatement_nullBalances_defaultsToZero() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(apInvoiceRepository.sumBalanceDueByVendor(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(null);
        when(apPaymentRepository.sumPaymentsByVendorAndStatus(TENANT_ID, 1L, APPaymentStatus.COMPLETED))
                .thenReturn(null);
        when(apInvoiceRepository.findUnpaidInvoicesByVendor(eq(TENANT_ID), eq(1L), anyList()))
                .thenReturn(Collections.emptyList());

        // When
        VendorBalanceReportDto.VendorBalanceDto result = apReportService.getVendorStatement(1L);

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getCurrentBalance());
        assertEquals(BigDecimal.ZERO, result.getTotalPaid());
    }

    private static APInvoice mock(Class<APInvoice> clazz) {
        return org.mockito.Mockito.mock(clazz);
    }
}
