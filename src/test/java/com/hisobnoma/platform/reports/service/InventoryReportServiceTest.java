package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.StockBatchRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.reports.dto.GenerateReportRequest;
import com.hisobnoma.platform.reports.dto.InventoryValuationReportDTO;
import com.hisobnoma.platform.reports.dto.StockOnHandReportDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryReportServiceTest {

    @Mock
    private SecurityContextHelper securityContextHelper;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private StockBatchRepository stockBatchRepository;
    @Mock
    private UserPrincipal userPrincipal;

    @InjectMocks
    private InventoryReportService inventoryReportService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    void generateStockOnHandReport_withStockData_returnsReport() {
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getUsername()).thenReturn("admin");

        Object[] stockRow = new Object[]{
                1L, "SKU-001", "Widget A", "Electronics", "Warehouse 1",
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("90"),
                new BigDecimal("50.00"), new BigDecimal("20")
        };
        when(stockRepository.getStockOnHandReport(eq(TENANT_ID), any(), any()))
                .thenReturn(List.<Object[]>of(stockRow));

        GenerateReportRequest request = GenerateReportRequest.builder().build();
        StockOnHandReportDTO result = inventoryReportService.generateStockOnHandReport(request);

        assertNotNull(result);
        assertEquals("Stock on Hand Report", result.getMetadata().getReportName());
        assertEquals(1, result.getItems().size());
        assertEquals("SKU-001", result.getItems().get(0).getSku());
        assertEquals(new BigDecimal("100"), result.getSummary().getTotalQuantity());
        assertEquals(0, result.getSummary().getLowStockCount());
        assertEquals(0, result.getSummary().getOutOfStockCount());
    }

    @Test
    void generateStockOnHandReport_outOfStock_countsCorrectly() {
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getUsername()).thenReturn("admin");

        Object[] outOfStockRow = new Object[]{
                1L, "SKU-002", "Widget B", "Electronics", "Warehouse 1",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("50.00"), new BigDecimal("10")
        };
        when(stockRepository.getStockOnHandReport(eq(TENANT_ID), any(), any()))
                .thenReturn(List.<Object[]>of(outOfStockRow));

        GenerateReportRequest request = GenerateReportRequest.builder().build();
        StockOnHandReportDTO result = inventoryReportService.generateStockOnHandReport(request);

        assertEquals(1, result.getSummary().getOutOfStockCount());
        assertEquals("OUT_OF_STOCK", result.getItems().get(0).getStockStatus());
    }

    @Test
    void generateStockOnHandReport_lowStock_countsCorrectly() {
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getUsername()).thenReturn("admin");

        Object[] lowStockRow = new Object[]{
                1L, "SKU-003", "Widget C", "Electronics", "Warehouse 1",
                new BigDecimal("5"), BigDecimal.ZERO, new BigDecimal("5"),
                new BigDecimal("50.00"), new BigDecimal("10")
        };
        when(stockRepository.getStockOnHandReport(eq(TENANT_ID), any(), any()))
                .thenReturn(List.<Object[]>of(lowStockRow));

        GenerateReportRequest request = GenerateReportRequest.builder().build();
        StockOnHandReportDTO result = inventoryReportService.generateStockOnHandReport(request);

        assertEquals(1, result.getSummary().getLowStockCount());
        assertEquals("LOW_STOCK", result.getItems().get(0).getStockStatus());
    }

    @Test
    void generateStockOnHandReport_noStock_returnsEmptyReport() {
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getUsername()).thenReturn("admin");
        when(stockRepository.getStockOnHandReport(eq(TENANT_ID), any(), any()))
                .thenReturn(Collections.emptyList());

        GenerateReportRequest request = GenerateReportRequest.builder().build();
        StockOnHandReportDTO result = inventoryReportService.generateStockOnHandReport(request);

        assertNotNull(result);
        assertEquals(0, result.getSummary().getTotalSkus());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void generateInventoryValuationReport_withData_returnsReport() {
        Object[] valuationRow = new Object[]{
                1L, "SKU-001", "Widget A", 10L,
                new BigDecimal("100"), new BigDecimal("50.00"), new BigDecimal("80.00"),
                "Electronics"
        };
        when(stockRepository.getInventoryValuationReport(eq(TENANT_ID), any()))
                .thenReturn(List.<Object[]>of(valuationRow));

        GenerateReportRequest request = GenerateReportRequest.builder().build();
        InventoryValuationReportDTO result = inventoryReportService.generateInventoryValuationReport(request);

        assertNotNull(result);
        assertEquals("Inventory Valuation Report", result.getMetadata().getReportName());
        assertEquals(1, result.getItems().size());
        assertEquals("SKU-001", result.getItems().get(0).getSku());
        assertEquals(new BigDecimal("100"), result.getSummary().getTotalQuantity());
    }

    @Test
    void generateInventoryValuationReport_noData_returnsEmptyReport() {
        when(stockRepository.getInventoryValuationReport(eq(TENANT_ID), any()))
                .thenReturn(Collections.emptyList());

        GenerateReportRequest request = GenerateReportRequest.builder().build();
        InventoryValuationReportDTO result = inventoryReportService.generateInventoryValuationReport(request);

        assertNotNull(result);
        assertEquals(0, result.getSummary().getTotalSkus());
        assertTrue(result.getItems().isEmpty());
    }
}
