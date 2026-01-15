package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.inventory.repository.CategoryRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.StockBatchRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.reports.dto.GenerateReportRequest;
import com.hisobnoma.platform.reports.dto.InventoryValuationReportDTO;
import com.hisobnoma.platform.reports.dto.StockOnHandReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for generating inventory reports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class InventoryReportService {

    private final SecurityContextHelper securityContextHelper;
    private final StockRepository stockRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final StockBatchRepository stockBatchRepository;

    /**
     * Generate Stock on Hand Report.
     */
    public StockOnHandReportDTO generateStockOnHandReport(GenerateReportRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        String userName = securityContextHelper.getRequiredCurrentUser().getFullName();

        log.info("Generating Stock on Hand report for tenant {}", tenantId);

        List<Object[]> stockData = stockRepository.getStockOnHandReport(tenantId, request.getLocationId(), request.getCategoryId());

        List<StockOnHandReportDTO.StockItem> items = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalValue = BigDecimal.ZERO;
        int lowStockCount = 0;
        int outOfStockCount = 0;

        for (Object[] row : stockData) {
            BigDecimal qty = (BigDecimal) row[5];
            BigDecimal qtyAvailable = (BigDecimal) row[7];
            BigDecimal unitCost = row[8] != null ? (BigDecimal) row[8] : BigDecimal.ZERO;
            BigDecimal value = qty.multiply(unitCost);
            BigDecimal reorderPoint = row[9] != null ? (BigDecimal) row[9] : BigDecimal.ZERO;

            String status = "IN_STOCK";
            if (qtyAvailable.compareTo(BigDecimal.ZERO) <= 0) {
                status = "OUT_OF_STOCK";
                outOfStockCount++;
            } else if (reorderPoint.compareTo(BigDecimal.ZERO) > 0 && qtyAvailable.compareTo(reorderPoint) <= 0) {
                status = "LOW_STOCK";
                lowStockCount++;
            }

            items.add(StockOnHandReportDTO.StockItem.builder()
                    .productId((Long) row[0])
                    .sku((String) row[1])
                    .productName((String) row[2])
                    .category((String) row[3])
                    .location((String) row[4])
                    .quantityOnHand(qty)
                    .quantityReserved((BigDecimal) row[6])
                    .quantityAvailable(qtyAvailable)
                    .unitCost(unitCost)
                    .totalValue(value)
                    .reorderPoint(reorderPoint)
                    .stockStatus(status)
                    .build());

            totalQuantity = totalQuantity.add(qty);
            totalValue = totalValue.add(value);
        }

        return StockOnHandReportDTO.builder()
                .metadata(StockOnHandReportDTO.ReportMetadata.builder()
                        .reportName("Stock on Hand Report")
                        .generatedAt(Instant.now())
                        .generatedBy(userName)
                        .locationFilter(request.getLocationId() != null ? "Location ID: " + request.getLocationId() : "All Locations")
                        .categoryFilter(request.getCategoryId() != null ? "Category ID: " + request.getCategoryId() : "All Categories")
                        .build())
                .summary(StockOnHandReportDTO.Summary.builder()
                        .totalSkus(items.size())
                        .totalQuantity(totalQuantity)
                        .totalValue(totalValue)
                        .lowStockCount(lowStockCount)
                        .outOfStockCount(outOfStockCount)
                        .build())
                .items(items)
                .build();
    }

    /**
     * Generate Inventory Valuation Report.
     */
    public InventoryValuationReportDTO generateInventoryValuationReport(GenerateReportRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        log.info("Generating Inventory Valuation report for tenant {}", tenantId);

        List<Object[]> valuationData = stockRepository.getInventoryValuationReport(tenantId, request.getCategoryId());

        List<InventoryValuationReportDTO.ValuationItem> items = new ArrayList<>();
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalCostValue = BigDecimal.ZERO;
        BigDecimal totalRetailValue = BigDecimal.ZERO;

        Map<Long, List<Object[]>> byCategory = valuationData.stream()
                .collect(Collectors.groupingBy(row -> (Long) row[3]));

        for (Object[] row : valuationData) {
            BigDecimal qty = (BigDecimal) row[4];
            BigDecimal unitCost = row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO;
            BigDecimal unitRetail = row[6] != null ? (BigDecimal) row[6] : BigDecimal.ZERO;
            BigDecimal totalCost = qty.multiply(unitCost);
            BigDecimal totalRetail = qty.multiply(unitRetail);
            BigDecimal margin = totalRetail.subtract(totalCost);
            BigDecimal marginPercent = totalRetail.compareTo(BigDecimal.ZERO) > 0
                    ? margin.divide(totalRetail, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            items.add(InventoryValuationReportDTO.ValuationItem.builder()
                    .productId((Long) row[0])
                    .sku((String) row[1])
                    .productName((String) row[2])
                    .category((String) row[7])
                    .quantity(qty)
                    .unitCost(unitCost)
                    .totalCost(totalCost)
                    .unitRetail(unitRetail)
                    .totalRetail(totalRetail)
                    .margin(margin)
                    .marginPercent(marginPercent)
                    .build());

            totalQuantity = totalQuantity.add(qty);
            totalCostValue = totalCostValue.add(totalCost);
            totalRetailValue = totalRetailValue.add(totalRetail);
        }

        BigDecimal potentialMargin = totalRetailValue.subtract(totalCostValue);
        BigDecimal marginPercent = totalRetailValue.compareTo(BigDecimal.ZERO) > 0
                ? potentialMargin.divide(totalRetailValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        // Category summaries
        List<InventoryValuationReportDTO.CategorySummary> categorySummaries = new ArrayList<>();
        for (Map.Entry<Long, List<Object[]>> entry : byCategory.entrySet()) {
            List<Object[]> categoryData = entry.getValue();
            BigDecimal catCost = BigDecimal.ZERO;
            BigDecimal catRetail = BigDecimal.ZERO;
            BigDecimal catQty = BigDecimal.ZERO;

            for (Object[] row : categoryData) {
                BigDecimal qty = (BigDecimal) row[4];
                BigDecimal unitCost = row[5] != null ? (BigDecimal) row[5] : BigDecimal.ZERO;
                BigDecimal unitRetail = row[6] != null ? (BigDecimal) row[6] : BigDecimal.ZERO;
                catQty = catQty.add(qty);
                catCost = catCost.add(qty.multiply(unitCost));
                catRetail = catRetail.add(qty.multiply(unitRetail));
            }

            BigDecimal percentOfTotal = totalCostValue.compareTo(BigDecimal.ZERO) > 0
                    ? catCost.divide(totalCostValue, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            categorySummaries.add(InventoryValuationReportDTO.CategorySummary.builder()
                    .categoryId(entry.getKey())
                    .categoryName((String) categoryData.get(0)[7])
                    .skuCount(categoryData.size())
                    .totalQuantity(catQty)
                    .totalCostValue(catCost)
                    .totalRetailValue(catRetail)
                    .percentOfTotal(percentOfTotal)
                    .build());
        }

        return InventoryValuationReportDTO.builder()
                .metadata(InventoryValuationReportDTO.ReportMetadata.builder()
                        .reportName("Inventory Valuation Report")
                        .generatedAt(Instant.now())
                        .asOfDate(LocalDate.now())
                        .valuationMethod("Average Cost")
                        .build())
                .summary(InventoryValuationReportDTO.Summary.builder()
                        .totalSkus(items.size())
                        .totalQuantity(totalQuantity)
                        .totalCostValue(totalCostValue)
                        .totalRetailValue(totalRetailValue)
                        .potentialMargin(potentialMargin)
                        .marginPercent(marginPercent)
                        .build())
                .items(items)
                .byCategory(categorySummaries)
                .build();
    }
}
