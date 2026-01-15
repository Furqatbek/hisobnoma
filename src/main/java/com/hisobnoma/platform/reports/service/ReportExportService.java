package com.hisobnoma.platform.reports.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hisobnoma.platform.reports.dto.*;
import com.hisobnoma.platform.reports.entity.ReportDefinition.ExportFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service for exporting reports to various formats (PDF, Excel, CSV, JSON).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final ObjectMapper objectMapper;

    /**
     * Export report data to the specified format.
     */
    public byte[] exportReport(Object reportData, ExportFormat format, String reportType) throws IOException {
        log.info("Exporting {} report to {} format", reportType, format);

        return switch (format) {
            case EXCEL -> exportToExcel(reportData, reportType);
            case CSV -> exportToCsv(reportData, reportType);
            case JSON -> exportToJson(reportData);
            case PDF -> exportToPdf(reportData, reportType);
        };
    }

    private byte[] exportToJson(Object reportData) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(reportData);
    }

    private byte[] exportToExcel(Object reportData, String reportType) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            if (reportData instanceof StockOnHandReportDTO stockReport) {
                createStockOnHandExcel(workbook, stockReport);
            } else if (reportData instanceof InventoryValuationReportDTO valuationReport) {
                createInventoryValuationExcel(workbook, valuationReport);
            } else if (reportData instanceof SalesSummaryReportDTO salesReport) {
                createSalesSummaryExcel(workbook, salesReport);
            } else if (reportData instanceof TrialBalanceReportDTO trialBalance) {
                createTrialBalanceExcel(workbook, trialBalance);
            } else if (reportData instanceof AgingReportDTO agingReport) {
                createAgingReportExcel(workbook, agingReport);
            } else {
                throw new IllegalArgumentException("Unsupported report type for Excel export: " + reportType);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private byte[] exportToCsv(Object reportData, String reportType) throws IOException {
        StringBuilder csv = new StringBuilder();

        if (reportData instanceof StockOnHandReportDTO stockReport) {
            createStockOnHandCsv(csv, stockReport);
        } else if (reportData instanceof InventoryValuationReportDTO valuationReport) {
            createInventoryValuationCsv(csv, valuationReport);
        } else if (reportData instanceof SalesSummaryReportDTO salesReport) {
            createSalesSummaryCsv(csv, salesReport);
        } else if (reportData instanceof TrialBalanceReportDTO trialBalance) {
            createTrialBalanceCsv(csv, trialBalance);
        } else if (reportData instanceof AgingReportDTO agingReport) {
            createAgingReportCsv(csv, agingReport);
        } else {
            throw new IllegalArgumentException("Unsupported report type for CSV export: " + reportType);
        }

        return csv.toString().getBytes();
    }

    private byte[] exportToPdf(Object reportData, String reportType) throws IOException {
        // PDF export would typically use a library like iText or Apache PDFBox
        // For now, return a placeholder - this can be implemented with proper PDF generation
        log.warn("PDF export not fully implemented, returning JSON as fallback");
        return exportToJson(reportData);
    }

    // Stock On Hand Excel
    private void createStockOnHandExcel(Workbook workbook, StockOnHandReportDTO report) {
        Sheet sheet = workbook.createSheet("Stock On Hand");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // Header info
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue(report.getMetadata().getReportName());

        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("Generated: " + report.getMetadata().getGeneratedAt().toString());
        rowNum++;

        // Summary
        Row summaryTitle = sheet.createRow(rowNum++);
        summaryTitle.createCell(0).setCellValue("Summary");
        sheet.createRow(rowNum++).createCell(0);
        createSummaryRow(sheet, rowNum++, "Total SKUs", String.valueOf(report.getSummary().getTotalSkus()));
        createSummaryRow(sheet, rowNum++, "Total Quantity", report.getSummary().getTotalQuantity().toString());
        createSummaryRow(sheet, rowNum++, "Total Value", report.getSummary().getTotalValue().toString());
        createSummaryRow(sheet, rowNum++, "Low Stock Items", String.valueOf(report.getSummary().getLowStockCount()));
        createSummaryRow(sheet, rowNum++, "Out of Stock Items", String.valueOf(report.getSummary().getOutOfStockCount()));
        rowNum++;

        // Column headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"SKU", "Product Name", "Category", "Location", "Qty On Hand", "Qty Reserved",
                "Qty Available", "Unit Cost", "Total Value", "Reorder Point", "Status"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (StockOnHandReportDTO.StockItem item : report.getItems()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getSku());
            row.createCell(1).setCellValue(item.getProductName());
            row.createCell(2).setCellValue(item.getCategory());
            row.createCell(3).setCellValue(item.getLocation());
            row.createCell(4).setCellValue(item.getQuantityOnHand().doubleValue());
            row.createCell(5).setCellValue(item.getQuantityReserved().doubleValue());
            row.createCell(6).setCellValue(item.getQuantityAvailable().doubleValue());
            Cell unitCostCell = row.createCell(7);
            unitCostCell.setCellValue(item.getUnitCost().doubleValue());
            unitCostCell.setCellStyle(currencyStyle);
            Cell totalValueCell = row.createCell(8);
            totalValueCell.setCellValue(item.getTotalValue().doubleValue());
            totalValueCell.setCellStyle(currencyStyle);
            row.createCell(9).setCellValue(item.getReorderPoint().doubleValue());
            row.createCell(10).setCellValue(item.getStockStatus());
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // Stock On Hand CSV
    private void createStockOnHandCsv(StringBuilder csv, StockOnHandReportDTO report) {
        csv.append("SKU,Product Name,Category,Location,Qty On Hand,Qty Reserved,Qty Available,Unit Cost,Total Value,Reorder Point,Status\n");
        for (StockOnHandReportDTO.StockItem item : report.getItems()) {
            csv.append(escapeCsv(item.getSku())).append(",");
            csv.append(escapeCsv(item.getProductName())).append(",");
            csv.append(escapeCsv(item.getCategory())).append(",");
            csv.append(escapeCsv(item.getLocation())).append(",");
            csv.append(item.getQuantityOnHand()).append(",");
            csv.append(item.getQuantityReserved()).append(",");
            csv.append(item.getQuantityAvailable()).append(",");
            csv.append(item.getUnitCost()).append(",");
            csv.append(item.getTotalValue()).append(",");
            csv.append(item.getReorderPoint()).append(",");
            csv.append(item.getStockStatus()).append("\n");
        }
    }

    // Inventory Valuation Excel
    private void createInventoryValuationExcel(Workbook workbook, InventoryValuationReportDTO report) {
        Sheet sheet = workbook.createSheet("Inventory Valuation");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;

        // Header info
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue(report.getMetadata().getReportName());
        rowNum++;

        // Column headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"SKU", "Product Name", "Category", "Quantity", "Unit Cost", "Total Cost",
                "Unit Retail", "Total Retail", "Margin", "Margin %"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (InventoryValuationReportDTO.ValuationItem item : report.getItems()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(item.getSku());
            row.createCell(1).setCellValue(item.getProductName());
            row.createCell(2).setCellValue(item.getCategory());
            row.createCell(3).setCellValue(item.getQuantity().doubleValue());
            Cell unitCostCell = row.createCell(4);
            unitCostCell.setCellValue(item.getUnitCost().doubleValue());
            unitCostCell.setCellStyle(currencyStyle);
            Cell totalCostCell = row.createCell(5);
            totalCostCell.setCellValue(item.getTotalCost().doubleValue());
            totalCostCell.setCellStyle(currencyStyle);
            Cell unitRetailCell = row.createCell(6);
            unitRetailCell.setCellValue(item.getUnitRetail().doubleValue());
            unitRetailCell.setCellStyle(currencyStyle);
            Cell totalRetailCell = row.createCell(7);
            totalRetailCell.setCellValue(item.getTotalRetail().doubleValue());
            totalRetailCell.setCellStyle(currencyStyle);
            Cell marginCell = row.createCell(8);
            marginCell.setCellValue(item.getMargin().doubleValue());
            marginCell.setCellStyle(currencyStyle);
            row.createCell(9).setCellValue(item.getMarginPercent().doubleValue());
        }

        // Auto-size columns
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // Inventory Valuation CSV
    private void createInventoryValuationCsv(StringBuilder csv, InventoryValuationReportDTO report) {
        csv.append("SKU,Product Name,Category,Quantity,Unit Cost,Total Cost,Unit Retail,Total Retail,Margin,Margin %\n");
        for (InventoryValuationReportDTO.ValuationItem item : report.getItems()) {
            csv.append(escapeCsv(item.getSku())).append(",");
            csv.append(escapeCsv(item.getProductName())).append(",");
            csv.append(escapeCsv(item.getCategory())).append(",");
            csv.append(item.getQuantity()).append(",");
            csv.append(item.getUnitCost()).append(",");
            csv.append(item.getTotalCost()).append(",");
            csv.append(item.getUnitRetail()).append(",");
            csv.append(item.getTotalRetail()).append(",");
            csv.append(item.getMargin()).append(",");
            csv.append(item.getMarginPercent()).append("\n");
        }
    }

    // Sales Summary Excel
    private void createSalesSummaryExcel(Workbook workbook, SalesSummaryReportDTO report) {
        // Summary sheet
        Sheet summarySheet = workbook.createSheet("Summary");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;
        Row titleRow = summarySheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue(report.getMetadata().getReportName());
        rowNum++;

        createSummaryRow(summarySheet, rowNum++, "Gross Sales", report.getSummary().getGrossSales().toString());
        createSummaryRow(summarySheet, rowNum++, "Discounts", report.getSummary().getDiscounts().toString());
        createSummaryRow(summarySheet, rowNum++, "Returns", report.getSummary().getReturns().toString());
        createSummaryRow(summarySheet, rowNum++, "Net Sales", report.getSummary().getNetSales().toString());
        createSummaryRow(summarySheet, rowNum++, "Cost of Goods", report.getSummary().getCostOfGoods().toString());
        createSummaryRow(summarySheet, rowNum++, "Gross Profit", report.getSummary().getGrossProfit().toString());
        createSummaryRow(summarySheet, rowNum++, "Gross Margin %", report.getSummary().getGrossMarginPercent().toString());
        createSummaryRow(summarySheet, rowNum++, "Transaction Count", String.valueOf(report.getSummary().getTransactionCount()));
        createSummaryRow(summarySheet, rowNum++, "Average Transaction", report.getSummary().getAverageTransactionValue().toString());
        createSummaryRow(summarySheet, rowNum++, "Items Sold", String.valueOf(report.getSummary().getItemsSold()));

        // Daily breakdown sheet
        if (report.getDailyBreakdown() != null && !report.getDailyBreakdown().isEmpty()) {
            Sheet dailySheet = workbook.createSheet("Daily Breakdown");
            rowNum = 0;
            Row headerRow = dailySheet.createRow(rowNum++);
            String[] headers = {"Date", "Day", "Net Sales", "Transactions", "Avg Transaction"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (SalesSummaryReportDTO.DailySales daily : report.getDailyBreakdown()) {
                Row row = dailySheet.createRow(rowNum++);
                row.createCell(0).setCellValue(daily.getDate().toString());
                row.createCell(1).setCellValue(daily.getDayOfWeek());
                Cell salesCell = row.createCell(2);
                salesCell.setCellValue(daily.getNetSales().doubleValue());
                salesCell.setCellStyle(currencyStyle);
                row.createCell(3).setCellValue(daily.getTransactionCount());
                Cell avgCell = row.createCell(4);
                avgCell.setCellValue(daily.getAverageTransaction().doubleValue());
                avgCell.setCellStyle(currencyStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                dailySheet.autoSizeColumn(i);
            }
        }

        // Top products sheet
        if (report.getTopProducts() != null && !report.getTopProducts().isEmpty()) {
            Sheet productsSheet = workbook.createSheet("Top Products");
            rowNum = 0;
            Row headerRow = productsSheet.createRow(rowNum++);
            String[] headers = {"SKU", "Product Name", "Qty Sold", "Net Sales", "Profit"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (SalesSummaryReportDTO.ProductSales product : report.getTopProducts()) {
                Row row = productsSheet.createRow(rowNum++);
                row.createCell(0).setCellValue(product.getSku());
                row.createCell(1).setCellValue(product.getProductName());
                row.createCell(2).setCellValue(product.getQuantitySold());
                Cell salesCell = row.createCell(3);
                salesCell.setCellValue(product.getNetSales().doubleValue());
                salesCell.setCellStyle(currencyStyle);
                Cell profitCell = row.createCell(4);
                profitCell.setCellValue(product.getProfit().doubleValue());
                profitCell.setCellStyle(currencyStyle);
            }

            for (int i = 0; i < headers.length; i++) {
                productsSheet.autoSizeColumn(i);
            }
        }

        summarySheet.autoSizeColumn(0);
        summarySheet.autoSizeColumn(1);
    }

    // Sales Summary CSV
    private void createSalesSummaryCsv(StringBuilder csv, SalesSummaryReportDTO report) {
        csv.append("Date,Day,Net Sales,Transactions,Average Transaction\n");
        for (SalesSummaryReportDTO.DailySales daily : report.getDailyBreakdown()) {
            csv.append(daily.getDate()).append(",");
            csv.append(escapeCsv(daily.getDayOfWeek())).append(",");
            csv.append(daily.getNetSales()).append(",");
            csv.append(daily.getTransactionCount()).append(",");
            csv.append(daily.getAverageTransaction()).append("\n");
        }
    }

    // Trial Balance Excel
    private void createTrialBalanceExcel(Workbook workbook, TrialBalanceReportDTO report) {
        Sheet sheet = workbook.createSheet("Trial Balance");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue(report.getMetadata().getReportName());
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("As of: " + report.getMetadata().getAsOfDate().toString());
        rowNum++;

        // Column headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Account Code", "Account Name", "Type", "Debit", "Credit"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (TrialBalanceReportDTO.AccountBalance account : report.getAccounts()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(account.getAccountCode());
            row.createCell(1).setCellValue(account.getAccountName());
            row.createCell(2).setCellValue(account.getAccountType());
            Cell debitCell = row.createCell(3);
            debitCell.setCellValue(account.getDebitBalance().doubleValue());
            debitCell.setCellStyle(currencyStyle);
            Cell creditCell = row.createCell(4);
            creditCell.setCellValue(account.getCreditBalance().doubleValue());
            creditCell.setCellStyle(currencyStyle);
        }

        // Totals row
        rowNum++;
        Row totalsRow = sheet.createRow(rowNum);
        totalsRow.createCell(0).setCellValue("TOTALS");
        Cell totalDebitCell = totalsRow.createCell(3);
        totalDebitCell.setCellValue(report.getTotals().getTotalDebits().doubleValue());
        totalDebitCell.setCellStyle(currencyStyle);
        Cell totalCreditCell = totalsRow.createCell(4);
        totalCreditCell.setCellValue(report.getTotals().getTotalCredits().doubleValue());
        totalCreditCell.setCellStyle(currencyStyle);

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // Trial Balance CSV
    private void createTrialBalanceCsv(StringBuilder csv, TrialBalanceReportDTO report) {
        csv.append("Account Code,Account Name,Type,Debit,Credit\n");
        for (TrialBalanceReportDTO.AccountBalance account : report.getAccounts()) {
            csv.append(escapeCsv(account.getAccountCode())).append(",");
            csv.append(escapeCsv(account.getAccountName())).append(",");
            csv.append(account.getAccountType()).append(",");
            csv.append(account.getDebitBalance()).append(",");
            csv.append(account.getCreditBalance()).append("\n");
        }
        csv.append("TOTALS,,,");
        csv.append(report.getTotals().getTotalDebits()).append(",");
        csv.append(report.getTotals().getTotalCredits()).append("\n");
    }

    // Aging Report Excel
    private void createAgingReportExcel(Workbook workbook, AgingReportDTO report) {
        Sheet sheet = workbook.createSheet(report.getMetadata().getReportType() + " Aging");
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        int rowNum = 0;
        Row titleRow = sheet.createRow(rowNum++);
        titleRow.createCell(0).setCellValue(report.getMetadata().getReportName());
        Row dateRow = sheet.createRow(rowNum++);
        dateRow.createCell(0).setCellValue("As of: " + report.getMetadata().getAsOfDate().toString());
        rowNum++;

        // Summary
        createSummaryRow(sheet, rowNum++, "Total Outstanding", report.getSummary().getTotalOutstanding().toString());
        createSummaryRow(sheet, rowNum++, "Current", report.getSummary().getCurrent().toString());
        createSummaryRow(sheet, rowNum++, "1-30 Days", report.getSummary().getDays1to30().toString());
        createSummaryRow(sheet, rowNum++, "31-60 Days", report.getSummary().getDays31to60().toString());
        createSummaryRow(sheet, rowNum++, "61-90 Days", report.getSummary().getDays61to90().toString());
        createSummaryRow(sheet, rowNum++, "Over 90 Days", report.getSummary().getOver90Days().toString());
        rowNum++;

        // Column headers
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"Code", "Name", "Total", "Current", "1-30 Days", "31-60 Days", "61-90 Days", "Over 90 Days", "Overdue Invoices"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        for (AgingReportDTO.AgingDetail detail : report.getDetails()) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(detail.getEntityCode());
            row.createCell(1).setCellValue(detail.getEntityName());
            Cell totalCell = row.createCell(2);
            totalCell.setCellValue(detail.getTotalOutstanding().doubleValue());
            totalCell.setCellStyle(currencyStyle);
            Cell currentCell = row.createCell(3);
            currentCell.setCellValue(detail.getCurrent().doubleValue());
            currentCell.setCellStyle(currencyStyle);
            Cell d1Cell = row.createCell(4);
            d1Cell.setCellValue(detail.getDays1to30().doubleValue());
            d1Cell.setCellStyle(currencyStyle);
            Cell d2Cell = row.createCell(5);
            d2Cell.setCellValue(detail.getDays31to60().doubleValue());
            d2Cell.setCellStyle(currencyStyle);
            Cell d3Cell = row.createCell(6);
            d3Cell.setCellValue(detail.getDays61to90().doubleValue());
            d3Cell.setCellStyle(currencyStyle);
            Cell d4Cell = row.createCell(7);
            d4Cell.setCellValue(detail.getOver90Days().doubleValue());
            d4Cell.setCellStyle(currencyStyle);
            row.createCell(8).setCellValue(detail.getOverdueInvoices());
        }

        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // Aging Report CSV
    private void createAgingReportCsv(StringBuilder csv, AgingReportDTO report) {
        csv.append("Code,Name,Total,Current,1-30 Days,31-60 Days,61-90 Days,Over 90 Days,Overdue Invoices\n");
        for (AgingReportDTO.AgingDetail detail : report.getDetails()) {
            csv.append(escapeCsv(detail.getEntityCode())).append(",");
            csv.append(escapeCsv(detail.getEntityName())).append(",");
            csv.append(detail.getTotalOutstanding()).append(",");
            csv.append(detail.getCurrent()).append(",");
            csv.append(detail.getDays1to30()).append(",");
            csv.append(detail.getDays31to60()).append(",");
            csv.append(detail.getDays61to90()).append(",");
            csv.append(detail.getOver90Days()).append(",");
            csv.append(detail.getOverdueInvoices()).append("\n");
        }
    }

    // Helper methods
    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private void createSummaryRow(Sheet sheet, int rowNum, String label, String value) {
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
