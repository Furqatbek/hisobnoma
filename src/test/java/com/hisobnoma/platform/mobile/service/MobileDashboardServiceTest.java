package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.repository.APInvoiceRepository;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.StockBatchRepository;
import com.hisobnoma.platform.inventory.repository.StockMovementRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.mobile.dto.FinancialSummaryDTO;
import com.hisobnoma.platform.mobile.dto.InventorySummaryDTO;
import com.hisobnoma.platform.mobile.dto.RevenueSummaryDTO;
import com.hisobnoma.platform.pos.repository.POSTransactionLineRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileDashboardServiceTest {

    @Mock
    private SecurityContextHelper securityContextHelper;

    @Mock
    private POSTransactionRepository transactionRepository;

    @Mock
    private POSTransactionLineRepository transactionLineRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockBatchRepository stockBatchRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ARInvoiceRepository arInvoiceRepository;

    @Mock
    private APInvoiceRepository apInvoiceRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private MobileDashboardService mobileDashboardService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
    }

    // ====== getRevenueSummary ======

    @Test
    void getRevenueSummary_returnsCompleteSummary() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("50000.00"));
        when(transactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(10);

        // When
        RevenueSummaryDTO result = mobileDashboardService.getRevenueSummary();

        // Then
        assertNotNull(result);
        assertNotNull(result.getTodayRevenue());
        assertNotNull(result.getTodayTransactionCount());
    }

    @Test
    void getRevenueSummary_handlesNullValues() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(null);
        when(transactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(null);

        // When
        RevenueSummaryDTO result = mobileDashboardService.getRevenueSummary();

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTodayRevenue());
        assertEquals(0, result.getTodayTransactionCount());
        assertEquals(BigDecimal.ZERO, result.getAverageTransactionValue());
    }

    // ====== getRevenueChartData ======

    @Test
    void getRevenueChartData_hourly_returns24Points() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(5);

        // When
        RevenueSummaryDTO result = mobileDashboardService.getRevenueChartData("hourly");

        // Then
        assertNotNull(result);
        assertNotNull(result.getHourlyRevenue());
        assertEquals(24, result.getHourlyRevenue().size());
        assertNull(result.getDailyRevenue());
    }

    @Test
    void getRevenueChartData_daily_returns30Points() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("5000.00"));
        when(transactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(20);

        // When
        RevenueSummaryDTO result = mobileDashboardService.getRevenueChartData("daily");

        // Then
        assertNotNull(result);
        assertNotNull(result.getDailyRevenue());
        assertEquals(30, result.getDailyRevenue().size());
        assertNull(result.getHourlyRevenue());
    }

    @Test
    void getRevenueChartData_monthly_returns12Points() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("100000.00"));
        when(transactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(100);

        // When
        RevenueSummaryDTO result = mobileDashboardService.getRevenueChartData("monthly");

        // Then
        assertNotNull(result);
        assertNotNull(result.getMonthlyRevenue());
        assertEquals(12, result.getMonthlyRevenue().size());
        assertNull(result.getHourlyRevenue());
    }

    @Test
    void getRevenueChartData_handlesNullRevenueValues() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(transactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(null);
        when(transactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(null);

        // When
        RevenueSummaryDTO result = mobileDashboardService.getRevenueChartData("daily");

        // Then
        assertNotNull(result);
        assertNotNull(result.getDailyRevenue());
        assertEquals(BigDecimal.ZERO, result.getDailyRevenue().get(0).getValue());
        assertEquals(0, result.getDailyRevenue().get(0).getCount());
    }

    // ====== getInventorySummary ======

    @Test
    void getInventorySummary_returnsCompleteSummary() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.countByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(500);
        when(stockRepository.calculateTotalInventoryValue(TENANT_ID)).thenReturn(new BigDecimal("1000000.00"));
        when(stockRepository.countLowStockProducts(TENANT_ID)).thenReturn(15);
        when(stockRepository.countOutOfStockProducts(TENANT_ID)).thenReturn(3);
        when(stockBatchRepository.countExpiringBatches(eq(TENANT_ID), any(LocalDate.class))).thenReturn(7);

        // When
        InventorySummaryDTO result = mobileDashboardService.getInventorySummary();

        // Then
        assertNotNull(result);
        assertEquals(500, result.getTotalSkuCount());
        assertEquals(new BigDecimal("1000000.00"), result.getTotalInventoryValue());
        assertEquals(15, result.getLowStockCount());
        assertEquals(3, result.getOutOfStockCount());
        assertEquals(7, result.getExpiringCount());
    }

    @Test
    void getInventorySummary_handlesNullValues() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(productRepository.countByTenantIdAndActiveTrue(TENANT_ID)).thenReturn(null);
        when(stockRepository.calculateTotalInventoryValue(TENANT_ID)).thenReturn(null);
        when(stockRepository.countLowStockProducts(TENANT_ID)).thenReturn(null);
        when(stockRepository.countOutOfStockProducts(TENANT_ID)).thenReturn(null);
        when(stockBatchRepository.countExpiringBatches(eq(TENANT_ID), any(LocalDate.class))).thenReturn(null);

        // When
        InventorySummaryDTO result = mobileDashboardService.getInventorySummary();

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalSkuCount());
        assertEquals(BigDecimal.ZERO, result.getTotalInventoryValue());
        assertEquals(0, result.getLowStockCount());
        assertEquals(0, result.getOutOfStockCount());
        assertEquals(0, result.getExpiringCount());
    }

    // ====== getFinancialSummary ======

    @Test
    void getFinancialSummary_returnsCompleteSummary() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.sumOutstandingBalance(eq(TENANT_ID), any())).thenReturn(new BigDecimal("200000.00"));
        when(apInvoiceRepository.sumOutstandingBalance(eq(TENANT_ID), any())).thenReturn(new BigDecimal("100000.00"));
        when(bankAccountRepository.sumTotalBalance(TENANT_ID)).thenReturn(new BigDecimal("500000.00"));

        // When
        FinancialSummaryDTO result = mobileDashboardService.getFinancialSummary();

        // Then
        assertNotNull(result);
        assertEquals(new BigDecimal("500000.00"), result.getTotalBankBalance());
        assertEquals(new BigDecimal("200000.00"), result.getArOutstanding());
        assertEquals(new BigDecimal("100000.00"), result.getApOutstanding());
        // netCashPosition = bank - ap + ar = 500000 - 100000 + 200000 = 600000
        assertEquals(new BigDecimal("600000.00"), result.getNetCashPosition());
    }

    @Test
    void getFinancialSummary_handlesNullValues() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(arInvoiceRepository.sumOutstandingBalance(eq(TENANT_ID), any())).thenReturn(null);
        when(apInvoiceRepository.sumOutstandingBalance(eq(TENANT_ID), any())).thenReturn(null);
        when(bankAccountRepository.sumTotalBalance(TENANT_ID)).thenReturn(null);

        // When
        FinancialSummaryDTO result = mobileDashboardService.getFinancialSummary();

        // Then
        assertNotNull(result);
        assertEquals(BigDecimal.ZERO, result.getTotalBankBalance());
        assertEquals(BigDecimal.ZERO, result.getArOutstanding());
        assertEquals(BigDecimal.ZERO, result.getApOutstanding());
        assertEquals(BigDecimal.ZERO, result.getNetCashPosition());
    }
}
