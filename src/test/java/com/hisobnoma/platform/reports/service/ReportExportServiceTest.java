package com.hisobnoma.platform.reports.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.reports.dto.*;
import com.hisobnoma.platform.reports.entity.ReportDefinition.ExportFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ReportExportService reportExportService;

    private StockOnHandReportDTO stockOnHandReport;
    private InventoryValuationReportDTO inventoryValuationReport;
    private TrialBalanceReportDTO trialBalanceReport;
    private AgingReportDTO agingReport;
    private SalesSummaryReportDTO salesSummaryReport;

    @BeforeEach
    void setUp() {
        stockOnHandReport = StockOnHandReportDTO.builder()
                .metadata(StockOnHandReportDTO.ReportMetadata.builder()
                        .reportName("Stock on Hand Report")
                        .generatedAt(Instant.now())
                        .generatedBy("admin")
                        .locationFilter("All Locations")
                        .categoryFilter("All Categories")
                        .build())
                .summary(StockOnHandReportDTO.Summary.builder()
                        .totalSkus(1)
                        .totalQuantity(new BigDecimal("100"))
                        .totalValue(new BigDecimal("5000"))
                        .lowStockCount(0)
                        .outOfStockCount(0)
                        .build())
                .items(List.of(StockOnHandReportDTO.StockItem.builder()
                        .productId(1L)
                        .sku("SKU-001")
                        .productName("Test Product")
                        .category("Electronics")
                        .location("Warehouse 1")
                        .quantityOnHand(new BigDecimal("100"))
                        .quantityReserved(new BigDecimal("10"))
                        .quantityAvailable(new BigDecimal("90"))
                        .unitCost(new BigDecimal("50.00"))
                        .totalValue(new BigDecimal("5000.00"))
                        .reorderPoint(new BigDecimal("20"))
                        .stockStatus("IN_STOCK")
                        .build()))
                .build();

        inventoryValuationReport = InventoryValuationReportDTO.builder()
                .metadata(InventoryValuationReportDTO.ReportMetadata.builder()
                        .reportName("Inventory Valuation Report")
                        .generatedAt(Instant.now())
                        .asOfDate(LocalDate.now())
                        .valuationMethod("Average Cost")
                        .build())
                .summary(InventoryValuationReportDTO.Summary.builder()
                        .totalSkus(1)
                        .totalQuantity(new BigDecimal("50"))
                        .totalCostValue(new BigDecimal("2500"))
                        .totalRetailValue(new BigDecimal("5000"))
                        .potentialMargin(new BigDecimal("2500"))
                        .marginPercent(new BigDecimal("50.00"))
                        .build())
                .items(List.of(InventoryValuationReportDTO.ValuationItem.builder()
                        .productId(1L)
                        .sku("SKU-001")
                        .productName("Test Product")
                        .category("Electronics")
                        .quantity(new BigDecimal("50"))
                        .unitCost(new BigDecimal("50.00"))
                        .totalCost(new BigDecimal("2500.00"))
                        .unitRetail(new BigDecimal("100.00"))
                        .totalRetail(new BigDecimal("5000.00"))
                        .margin(new BigDecimal("2500.00"))
                        .marginPercent(new BigDecimal("50.00"))
                        .build()))
                .byCategory(List.of())
                .build();

        trialBalanceReport = TrialBalanceReportDTO.builder()
                .metadata(TrialBalanceReportDTO.ReportMetadata.builder()
                        .reportName("Trial Balance")
                        .generatedAt(Instant.now())
                        .asOfDate(LocalDate.now())
                        .fiscalYear("2025")
                        .period("MARCH")
                        .build())
                .accounts(List.of(TrialBalanceReportDTO.AccountBalance.builder()
                        .accountId(1L)
                        .accountCode("1000")
                        .accountName("Cash")
                        .accountType("ASSET")
                        .debitBalance(new BigDecimal("10000"))
                        .creditBalance(BigDecimal.ZERO)
                        .level(1)
                        .isHeader(false)
                        .build()))
                .totals(TrialBalanceReportDTO.Totals.builder()
                        .totalDebits(new BigDecimal("10000"))
                        .totalCredits(new BigDecimal("10000"))
                        .isBalanced(true)
                        .difference(BigDecimal.ZERO)
                        .build())
                .build();

        agingReport = AgingReportDTO.builder()
                .metadata(AgingReportDTO.ReportMetadata.builder()
                        .reportName("AR Aging Report")
                        .reportType("AR")
                        .generatedAt(Instant.now())
                        .asOfDate(LocalDate.now())
                        .build())
                .summary(AgingReportDTO.Summary.builder()
                        .totalOutstanding(new BigDecimal("5000"))
                        .current(new BigDecimal("3000"))
                        .days1to30(new BigDecimal("1000"))
                        .days31to60(new BigDecimal("500"))
                        .days61to90(new BigDecimal("300"))
                        .over90Days(new BigDecimal("200"))
                        .totalAccounts(1)
                        .overdueAccounts(1)
                        .build())
                .details(List.of(AgingReportDTO.AgingDetail.builder()
                        .entityId(1L)
                        .entityCode("CUST-001")
                        .entityName("Test Customer")
                        .totalOutstanding(new BigDecimal("5000"))
                        .current(new BigDecimal("3000"))
                        .days1to30(new BigDecimal("1000"))
                        .days31to60(new BigDecimal("500"))
                        .days61to90(new BigDecimal("300"))
                        .over90Days(new BigDecimal("200"))
                        .overdueInvoices(3)
                        .build()))
                .build();

        salesSummaryReport = SalesSummaryReportDTO.builder()
                .metadata(SalesSummaryReportDTO.ReportMetadata.builder()
                        .reportName("Sales Summary Report")
                        .generatedAt(Instant.now())
                        .startDate(LocalDate.now().minusDays(30))
                        .endDate(LocalDate.now())
                        .build())
                .summary(SalesSummaryReportDTO.Summary.builder()
                        .grossSales(new BigDecimal("100000"))
                        .discounts(new BigDecimal("5000"))
                        .returns(new BigDecimal("2000"))
                        .netSales(new BigDecimal("93000"))
                        .costOfGoods(new BigDecimal("60000"))
                        .grossProfit(new BigDecimal("33000"))
                        .grossMarginPercent(new BigDecimal("35.48"))
                        .transactionCount(150)
                        .averageTransactionValue(new BigDecimal("620.00"))
                        .itemsSold(500)
                        .build())
                .dailyBreakdown(List.of(SalesSummaryReportDTO.DailySales.builder()
                        .date(LocalDate.now())
                        .dayOfWeek("Monday")
                        .netSales(new BigDecimal("3000"))
                        .transactionCount(5)
                        .averageTransaction(new BigDecimal("600"))
                        .build()))
                .topProducts(List.of(SalesSummaryReportDTO.ProductSales.builder()
                        .productId(1L)
                        .sku("SKU-001")
                        .productName("Top Product")
                        .quantitySold(100)
                        .netSales(new BigDecimal("50000"))
                        .profit(new BigDecimal("20000"))
                        .build()))
                .byCategory(List.of())
                .byPaymentMethod(List.of())
                .build();
    }

    // ====== exportReport - Excel ======

    @Test
    void exportReport_stockOnHand_excel_returnsBytes() throws IOException {
        // When
        byte[] result = reportExportService.exportReport(stockOnHandReport, ExportFormat.EXCEL, "STOCK_ON_HAND");

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportReport_inventoryValuation_excel_returnsBytes() throws IOException {
        // When
        byte[] result = reportExportService.exportReport(inventoryValuationReport, ExportFormat.EXCEL, "INVENTORY_VALUATION");

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportReport_trialBalance_excel_returnsBytes() throws IOException {
        // When
        byte[] result = reportExportService.exportReport(trialBalanceReport, ExportFormat.EXCEL, "TRIAL_BALANCE");

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportReport_aging_excel_returnsBytes() throws IOException {
        // When
        byte[] result = reportExportService.exportReport(agingReport, ExportFormat.EXCEL, "AR_AGING");

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportReport_salesSummary_excel_returnsBytes() throws IOException {
        // When
        byte[] result = reportExportService.exportReport(salesSummaryReport, ExportFormat.EXCEL, "SALES_SUMMARY");

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void exportReport_unsupportedType_excel_throwsIllegalArgument() {
        // When/Then
        assertThrows(IllegalArgumentException.class,
                () -> reportExportService.exportReport("unsupported", ExportFormat.EXCEL, "UNKNOWN"));
    }

    // ====== exportReport - CSV ======

    @Test
    void exportReport_stockOnHand_csv_returnsBytes() throws IOException {
        // When
        byte[] result = reportExportService.exportReport(stockOnHandReport, ExportFormat.CSV, "STOCK_ON_HAND");

        // Then
        assertNotNull(result);
        String csv = new String(result);
        assertTrue(csv.contains("SKU"));
        assertTrue(csv.contains("SKU-001"));
    }

    @Test
    void exportReport_inventoryValuation_csv_returnsBytes() throws IOException {
        // When
        byte[] result = reportExportService.exportReport(inventoryValuationReport, ExportFormat.CSV, "INVENTORY_VALUATION");

        // Then
        assertNotNull(result);
        String csv = new String(result);
        assertTrue(csv.contains("SKU"));
    }

    @Test
    void exportReport_trialBalance_csv_returnsBytes() throws IOException {
        // When
        byte[] result = reportExportService.exportReport(trialBalanceReport, ExportFormat.CSV, "TRIAL_BALANCE");

        // Then
        assertNotNull(result);
        String csv = new String(result);
        assertTrue(csv.contains("Account Code"));
        assertTrue(csv.contains("TOTALS"));
    }

    @Test
    void exportReport_aging_csv_returnsBytes() throws IOException {
        // When
        byte[] result = reportExportService.exportReport(agingReport, ExportFormat.CSV, "AR_AGING");

        // Then
        assertNotNull(result);
        String csv = new String(result);
        assertTrue(csv.contains("CUST-001"));
    }

    @Test
    void exportReport_unsupportedType_csv_throwsIllegalArgument() {
        // When/Then
        assertThrows(IllegalArgumentException.class,
                () -> reportExportService.exportReport("unsupported", ExportFormat.CSV, "UNKNOWN"));
    }

    // ====== exportReport - JSON ======

    @Test
    void exportReport_json_returnsBytes() throws IOException {
        // When
        byte[] result = reportExportService.exportReport(stockOnHandReport, ExportFormat.JSON, "STOCK_ON_HAND");

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    // ====== exportReport - PDF ======

    @Test
    void exportReport_pdf_returnsFallbackJson() throws IOException {
        // When (PDF falls back to JSON)
        byte[] result = reportExportService.exportReport(stockOnHandReport, ExportFormat.PDF, "STOCK_ON_HAND");

        // Then
        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
