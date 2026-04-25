package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.pos.entity.POSTransaction;
import com.hisobnoma.platform.pos.entity.POSTransactionLine;
import com.hisobnoma.platform.pos.entity.TransactionStatus;
import com.hisobnoma.platform.pos.entity.TransactionType;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import com.hisobnoma.platform.reports.dto.GenerateReportRequest;
import com.hisobnoma.platform.reports.dto.SalesSummaryReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating sales reports.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SalesReportService {

    private final SecurityContextHelper securityContextHelper;
    private final POSTransactionRepository transactionRepository;
    private final TenantRepository tenantRepository;

    private ZoneId resolveTenantZone(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .map(Tenant::getTimezone)
                .filter(tz -> tz != null && !tz.isBlank())
                .map(tz -> {
                    try {
                        return ZoneId.of(tz);
                    } catch (Exception e) {
                        log.warn("Invalid tenant timezone '{}', falling back to UTC", tz);
                        return ZoneOffset.UTC;
                    }
                })
                .orElse(ZoneOffset.UTC);
    }

    /**
     * Generate Sales Summary Report.
     */
    public SalesSummaryReportDTO generateSalesSummaryReport(GenerateReportRequest request) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        String userName = securityContextHelper.getRequiredCurrentUser().getUsername();

        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now().minusDays(30);
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : LocalDate.now();

        log.info("Generating Sales Summary report for tenant {} from {} to {}", tenantId, startDate, endDate);

        ZoneId zone = resolveTenantZone(tenantId);
        Instant startInstant = startDate.atStartOfDay(zone).toInstant();
        Instant endInstant = endDate.plusDays(1).atStartOfDay(zone).toInstant();

        // Get all completed sales transactions in the date range
        List<POSTransaction> salesTransactions = transactionRepository.findByTypeAndDateRangeAndTenantId(
                TransactionType.SALE, startInstant, endInstant, tenantId);

        // Get all completed returns in the date range
        List<POSTransaction> returnTransactions = transactionRepository.findByTypeAndDateRangeAndTenantId(
                TransactionType.RETURN, startInstant, endInstant, tenantId);

        // Calculate summary — all amounts exclude tax for consistency
        BigDecimal grossSales = salesTransactions.stream()
                .flatMap(t -> t.getLines().stream())
                .map(POSTransactionLine::getGrossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal lineDiscounts = salesTransactions.stream()
                .flatMap(t -> t.getLines().stream())
                .map(line -> line.getDiscountAmount() != null ? line.getDiscountAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal transactionDiscounts = salesTransactions.stream()
                .map(t -> t.getDiscountAmount() != null ? t.getDiscountAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discounts = lineDiscounts.add(transactionDiscounts);

        BigDecimal returns = returnTransactions.stream()
                .flatMap(t -> t.getLines().stream())
                .map(line -> line.getNetAmount().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netSales = grossSales.subtract(discounts).subtract(returns);

        // Calculate cost of goods sold
        BigDecimal costOfGoods = salesTransactions.stream()
                .flatMap(t -> t.getLines().stream())
                .map(line -> {
                    BigDecimal cost = line.getCostPrice() != null ? line.getCostPrice() : BigDecimal.ZERO;
                    return cost.multiply(line.getQuantity());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Subtract returned items cost
        BigDecimal returnedCost = returnTransactions.stream()
                .flatMap(t -> t.getLines().stream())
                .map(line -> {
                    BigDecimal cost = line.getCostPrice() != null ? line.getCostPrice() : BigDecimal.ZERO;
                    return cost.multiply(line.getQuantity().abs());
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        costOfGoods = costOfGoods.subtract(returnedCost);

        BigDecimal grossProfit = netSales.subtract(costOfGoods);
        BigDecimal grossMarginPercent = netSales.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.divide(netSales, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;

        int transactionCount = salesTransactions.size();
        BigDecimal averageTransactionValue = transactionCount > 0
                ? netSales.divide(BigDecimal.valueOf(transactionCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int itemsSold = salesTransactions.stream()
                .mapToInt(t -> t.getItemCount() != null ? t.getItemCount() : 0)
                .sum();

        // Build daily breakdown
        List<SalesSummaryReportDTO.DailySales> dailyBreakdown = buildDailyBreakdown(salesTransactions, startDate, endDate, zone);

        // Build category breakdown — use line-level net total as denominator so percentages sum to ~100%
        BigDecimal totalLineNet = salesTransactions.stream()
                .flatMap(t -> t.getLines().stream())
                .map(POSTransactionLine::getNetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<SalesSummaryReportDTO.CategorySales> byCategory = buildCategoryBreakdown(salesTransactions, totalLineNet);

        // Build top products
        List<SalesSummaryReportDTO.ProductSales> topProducts = buildTopProducts(salesTransactions, 10);

        // Build payment method breakdown
        List<SalesSummaryReportDTO.PaymentMethodSales> byPaymentMethod = buildPaymentMethodBreakdown(salesTransactions);

        return SalesSummaryReportDTO.builder()
                .metadata(SalesSummaryReportDTO.ReportMetadata.builder()
                        .reportName("Sales Summary Report")
                        .generatedAt(Instant.now())
                        .startDate(startDate)
                        .endDate(endDate)
                        .locationFilter(request.getLocationId() != null ? "Location ID: " + request.getLocationId() : "All Locations")
                        .build())
                .summary(SalesSummaryReportDTO.Summary.builder()
                        .grossSales(grossSales)
                        .discounts(discounts)
                        .returns(returns)
                        .netSales(netSales)
                        .costOfGoods(costOfGoods)
                        .grossProfit(grossProfit)
                        .grossMarginPercent(grossMarginPercent)
                        .transactionCount(transactionCount)
                        .averageTransactionValue(averageTransactionValue)
                        .itemsSold(itemsSold)
                        .build())
                .dailyBreakdown(dailyBreakdown)
                .byCategory(byCategory)
                .topProducts(topProducts)
                .byPaymentMethod(byPaymentMethod)
                .build();
    }

    private List<SalesSummaryReportDTO.DailySales> buildDailyBreakdown(List<POSTransaction> transactions, LocalDate startDate, LocalDate endDate, ZoneId zone) {
        Map<LocalDate, List<POSTransaction>> byDate = transactions.stream()
                .collect(Collectors.groupingBy(t ->
                        t.getCompletedAt().atZone(zone).toLocalDate()));

        List<SalesSummaryReportDTO.DailySales> result = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<POSTransaction> dayTransactions = byDate.getOrDefault(date, Collections.emptyList());

            BigDecimal dayLineNet = dayTransactions.stream()
                    .flatMap(t -> t.getLines().stream())
                    .map(POSTransactionLine::getNetAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dayTxnDiscount = dayTransactions.stream()
                    .map(t -> t.getDiscountAmount() != null ? t.getDiscountAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal dayNetSales = dayLineNet.subtract(dayTxnDiscount);

            int count = dayTransactions.size();
            BigDecimal avgTransaction = count > 0
                    ? dayNetSales.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            result.add(SalesSummaryReportDTO.DailySales.builder()
                    .date(date)
                    .dayOfWeek(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH))
                    .netSales(dayNetSales)
                    .transactionCount(count)
                    .averageTransaction(avgTransaction)
                    .build());
        }
        return result;
    }

    private List<SalesSummaryReportDTO.CategorySales> buildCategoryBreakdown(List<POSTransaction> transactions, BigDecimal totalNetSales) {
        Map<String, BigDecimal> salesByCategory = new HashMap<>();
        Map<String, Integer> itemsByCategory = new HashMap<>();
        Map<String, Long> categoryIds = new HashMap<>();

        for (POSTransaction tx : transactions) {
            for (POSTransactionLine line : tx.getLines()) {
                String categoryName = line.getProduct() != null && line.getProduct().getCategory() != null
                        ? line.getProduct().getCategory().getName()
                        : "Uncategorized";
                Long categoryId = line.getProduct() != null && line.getProduct().getCategory() != null
                        ? line.getProduct().getCategory().getId()
                        : null;

                BigDecimal lineNet = line.getNetAmount();
                int qty = line.getQuantity().intValue();

                salesByCategory.merge(categoryName, lineNet, BigDecimal::add);
                itemsByCategory.merge(categoryName, qty, Integer::sum);
                if (categoryId != null) {
                    categoryIds.putIfAbsent(categoryName, categoryId);
                }
            }
        }

        List<SalesSummaryReportDTO.CategorySales> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : salesByCategory.entrySet()) {
            String categoryName = entry.getKey();
            BigDecimal netSales = entry.getValue();
            BigDecimal percentOfTotal = totalNetSales.compareTo(BigDecimal.ZERO) > 0
                    ? netSales.divide(totalNetSales, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            result.add(SalesSummaryReportDTO.CategorySales.builder()
                    .categoryId(categoryIds.get(categoryName))
                    .categoryName(categoryName)
                    .netSales(netSales)
                    .itemsSold(itemsByCategory.get(categoryName))
                    .percentOfTotal(percentOfTotal)
                    .build());
        }

        result.sort((a, b) -> b.getNetSales().compareTo(a.getNetSales()));
        return result;
    }

    private List<SalesSummaryReportDTO.ProductSales> buildTopProducts(List<POSTransaction> transactions, int limit) {
        Map<Long, BigDecimal> salesByProduct = new HashMap<>();
        Map<Long, Integer> qtyByProduct = new HashMap<>();
        Map<Long, BigDecimal> profitByProduct = new HashMap<>();
        Map<Long, String> productSkus = new HashMap<>();
        Map<Long, String> productNames = new HashMap<>();

        for (POSTransaction tx : transactions) {
            for (POSTransactionLine line : tx.getLines()) {
                if (line.getProduct() == null) continue;

                Long productId = line.getProduct().getId();
                BigDecimal lineNet = line.getNetAmount();
                int qty = line.getQuantity().intValue();
                BigDecimal profit = line.getProfit();

                salesByProduct.merge(productId, lineNet, BigDecimal::add);
                qtyByProduct.merge(productId, qty, Integer::sum);
                profitByProduct.merge(productId, profit, BigDecimal::add);
                productSkus.putIfAbsent(productId, line.getProduct().getSku());
                productNames.putIfAbsent(productId, line.getProductName());
            }
        }

        return salesByProduct.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .map(entry -> {
                    Long productId = entry.getKey();
                    return SalesSummaryReportDTO.ProductSales.builder()
                            .productId(productId)
                            .sku(productSkus.get(productId))
                            .productName(productNames.get(productId))
                            .quantitySold(qtyByProduct.get(productId))
                            .netSales(entry.getValue())
                            .profit(profitByProduct.get(productId))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<SalesSummaryReportDTO.PaymentMethodSales> buildPaymentMethodBreakdown(List<POSTransaction> transactions) {
        Map<String, BigDecimal> amountByMethod = new HashMap<>();
        Map<String, Integer> countByMethod = new HashMap<>();

        for (POSTransaction tx : transactions) {
            tx.getPayments().forEach(payment -> {
                String method = payment.getPaymentType() != null
                        ? payment.getPaymentType().name()
                        : "UNKNOWN";
                amountByMethod.merge(method, payment.getAmount(), BigDecimal::add);
                countByMethod.merge(method, 1, Integer::sum);
            });
        }

        BigDecimal totalAmount = amountByMethod.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SalesSummaryReportDTO.PaymentMethodSales> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : amountByMethod.entrySet()) {
            BigDecimal amount = entry.getValue();
            BigDecimal percentOfTotal = totalAmount.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(totalAmount, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                    : BigDecimal.ZERO;

            result.add(SalesSummaryReportDTO.PaymentMethodSales.builder()
                    .paymentMethod(entry.getKey())
                    .amount(amount)
                    .transactionCount(countByMethod.get(entry.getKey()))
                    .percentOfTotal(percentOfTotal)
                    .build());
        }

        result.sort((a, b) -> b.getAmount().compareTo(a.getAmount()));
        return result;
    }
}
