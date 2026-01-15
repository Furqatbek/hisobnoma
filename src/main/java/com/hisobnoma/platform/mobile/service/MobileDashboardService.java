package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.repository.*;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.inventory.repository.StockBatchRepository;
import com.hisobnoma.platform.inventory.repository.StockMovementRepository;
import com.hisobnoma.platform.mobile.dto.*;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionLineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for mobile dashboard data aggregation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MobileDashboardService {

    private final SecurityContextHelper securityContextHelper;
    private final POSTransactionRepository transactionRepository;
    private final POSTransactionLineRepository transactionLineRepository;
    private final StockRepository stockRepository;
    private final StockBatchRepository stockBatchRepository;
    private final StockMovementRepository stockMovementRepository;
    private final ProductRepository productRepository;
    private final ARInvoiceRepository arInvoiceRepository;
    private final APInvoiceRepository apInvoiceRepository;
    private final BankAccountRepository bankAccountRepository;

    /**
     * Get revenue summary for mobile dashboard.
     */
    public RevenueSummaryDTO getRevenueSummary() {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        LocalDate lastWeekStart = weekStart.minusWeeks(1);
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate lastMonthStart = monthStart.minusMonths(1);

        Instant todayStart = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant todayEnd = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant yesterdayStart = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant yesterdayEnd = today.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant weekStartInstant = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant lastWeekStartInstant = lastWeekStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant lastWeekEndInstant = weekStartInstant;
        Instant monthStartInstant = monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant lastMonthStartInstant = lastMonthStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant lastMonthEndInstant = monthStartInstant;

        BigDecimal todayRevenue = transactionRepository.sumCompletedSalesByDateRange(tenantId, todayStart, todayEnd);
        BigDecimal yesterdayRevenue = transactionRepository.sumCompletedSalesByDateRange(tenantId, yesterdayStart, yesterdayEnd);
        BigDecimal thisWeekRevenue = transactionRepository.sumCompletedSalesByDateRange(tenantId, weekStartInstant, todayEnd);
        BigDecimal lastWeekRevenue = transactionRepository.sumCompletedSalesByDateRange(tenantId, lastWeekStartInstant, lastWeekEndInstant);
        BigDecimal thisMonthRevenue = transactionRepository.sumCompletedSalesByDateRange(tenantId, monthStartInstant, todayEnd);
        BigDecimal lastMonthRevenue = transactionRepository.sumCompletedSalesByDateRange(tenantId, lastMonthStartInstant, lastMonthEndInstant);

        todayRevenue = todayRevenue != null ? todayRevenue : BigDecimal.ZERO;
        yesterdayRevenue = yesterdayRevenue != null ? yesterdayRevenue : BigDecimal.ZERO;
        thisWeekRevenue = thisWeekRevenue != null ? thisWeekRevenue : BigDecimal.ZERO;
        lastWeekRevenue = lastWeekRevenue != null ? lastWeekRevenue : BigDecimal.ZERO;
        thisMonthRevenue = thisMonthRevenue != null ? thisMonthRevenue : BigDecimal.ZERO;
        lastMonthRevenue = lastMonthRevenue != null ? lastMonthRevenue : BigDecimal.ZERO;

        Integer todayCount = transactionRepository.countCompletedSalesByDateRange(tenantId, todayStart, todayEnd);
        Integer weekCount = transactionRepository.countCompletedSalesByDateRange(tenantId, weekStartInstant, todayEnd);
        Integer monthCount = transactionRepository.countCompletedSalesByDateRange(tenantId, monthStartInstant, todayEnd);

        return RevenueSummaryDTO.builder()
                .todayRevenue(todayRevenue)
                .yesterdayRevenue(yesterdayRevenue)
                .thisWeekRevenue(thisWeekRevenue)
                .lastWeekRevenue(lastWeekRevenue)
                .thisMonthRevenue(thisMonthRevenue)
                .lastMonthRevenue(lastMonthRevenue)
                .todayChangePercent(calculateChangePercent(todayRevenue, yesterdayRevenue))
                .weekChangePercent(calculateChangePercent(thisWeekRevenue, lastWeekRevenue))
                .monthChangePercent(calculateChangePercent(thisMonthRevenue, lastMonthRevenue))
                .todayTransactionCount(todayCount != null ? todayCount : 0)
                .thisWeekTransactionCount(weekCount != null ? weekCount : 0)
                .thisMonthTransactionCount(monthCount != null ? monthCount : 0)
                .averageTransactionValue(calculateAverageTransaction(thisMonthRevenue, monthCount))
                .build();
    }

    /**
     * Get revenue chart data.
     */
    public RevenueSummaryDTO getRevenueChartData(String period) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        List<RevenueSummaryDTO.ChartDataPoint> chartData = new ArrayList<>();

        LocalDate today = LocalDate.now();

        switch (period.toLowerCase()) {
            case "hourly" -> {
                // Last 24 hours by hour
                for (int i = 23; i >= 0; i--) {
                    Instant start = today.atStartOfDay(ZoneId.systemDefault()).toInstant().minusSeconds(i * 3600L);
                    Instant end = start.plusSeconds(3600);
                    BigDecimal revenue = transactionRepository.sumCompletedSalesByDateRange(tenantId, start, end);
                    Integer count = transactionRepository.countCompletedSalesByDateRange(tenantId, start, end);
                    chartData.add(RevenueSummaryDTO.ChartDataPoint.builder()
                            .label(String.format("%02d:00", (24 - i) % 24))
                            .value(revenue != null ? revenue : BigDecimal.ZERO)
                            .count(count != null ? count : 0)
                            .build());
                }
            }
            case "daily" -> {
                // Last 30 days
                for (int i = 29; i >= 0; i--) {
                    LocalDate date = today.minusDays(i);
                    Instant start = date.atStartOfDay(ZoneId.systemDefault()).toInstant();
                    Instant end = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant();
                    BigDecimal revenue = transactionRepository.sumCompletedSalesByDateRange(tenantId, start, end);
                    Integer count = transactionRepository.countCompletedSalesByDateRange(tenantId, start, end);
                    chartData.add(RevenueSummaryDTO.ChartDataPoint.builder()
                            .label(date.toString())
                            .value(revenue != null ? revenue : BigDecimal.ZERO)
                            .count(count != null ? count : 0)
                            .build());
                }
            }
            case "monthly" -> {
                // Last 12 months
                for (int i = 11; i >= 0; i--) {
                    LocalDate monthStart = today.minusMonths(i).withDayOfMonth(1);
                    LocalDate monthEnd = monthStart.plusMonths(1);
                    Instant start = monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant();
                    Instant end = monthEnd.atStartOfDay(ZoneId.systemDefault()).toInstant();
                    BigDecimal revenue = transactionRepository.sumCompletedSalesByDateRange(tenantId, start, end);
                    Integer count = transactionRepository.countCompletedSalesByDateRange(tenantId, start, end);
                    chartData.add(RevenueSummaryDTO.ChartDataPoint.builder()
                            .label(monthStart.getMonth().toString().substring(0, 3) + " " + monthStart.getYear())
                            .value(revenue != null ? revenue : BigDecimal.ZERO)
                            .count(count != null ? count : 0)
                            .build());
                }
            }
        }

        return RevenueSummaryDTO.builder()
                .dailyRevenue(period.equalsIgnoreCase("daily") ? chartData : null)
                .hourlyRevenue(period.equalsIgnoreCase("hourly") ? chartData : null)
                .monthlyRevenue(period.equalsIgnoreCase("monthly") ? chartData : null)
                .build();
    }

    /**
     * Get inventory summary for mobile dashboard.
     */
    public InventorySummaryDTO getInventorySummary() {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        Integer totalSku = productRepository.countByTenantIdAndActiveTrue(tenantId);
        Integer activeSku = productRepository.countByTenantIdAndActiveTrue(tenantId);
        BigDecimal inventoryValue = stockRepository.calculateTotalInventoryValue(tenantId);
        Integer lowStockCount = stockRepository.countLowStockProducts(tenantId);
        Integer outOfStockCount = stockRepository.countOutOfStockProducts(tenantId);

        LocalDate expiryThreshold = LocalDate.now().plusDays(30);
        Integer expiringCount = stockBatchRepository.countExpiringBatches(tenantId, expiryThreshold);

        return InventorySummaryDTO.builder()
                .totalSkuCount(totalSku != null ? totalSku : 0)
                .activeSkuCount(activeSku != null ? activeSku : 0)
                .totalInventoryValue(inventoryValue != null ? inventoryValue : BigDecimal.ZERO)
                .lowStockCount(lowStockCount != null ? lowStockCount : 0)
                .outOfStockCount(outOfStockCount != null ? outOfStockCount : 0)
                .expiringCount(expiringCount != null ? expiringCount : 0)
                .build();
    }

    /**
     * Get financial summary for mobile dashboard.
     */
    public FinancialSummaryDTO getFinancialSummary() {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        BigDecimal arOutstanding = arInvoiceRepository.sumOutstandingBalance(tenantId);
        BigDecimal apOutstanding = apInvoiceRepository.sumOutstandingBalance(tenantId);
        BigDecimal bankBalance = bankAccountRepository.sumTotalBalance(tenantId);

        arOutstanding = arOutstanding != null ? arOutstanding : BigDecimal.ZERO;
        apOutstanding = apOutstanding != null ? apOutstanding : BigDecimal.ZERO;
        bankBalance = bankBalance != null ? bankBalance : BigDecimal.ZERO;

        return FinancialSummaryDTO.builder()
                .totalBankBalance(bankBalance)
                .totalCashBalance(BigDecimal.ZERO) // Would need cash drawer integration
                .arOutstanding(arOutstanding)
                .apOutstanding(apOutstanding)
                .netCashPosition(bankBalance.subtract(apOutstanding).add(arOutstanding))
                .build();
    }

    private BigDecimal calculateChangePercent(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return current != null && current.compareTo(BigDecimal.ZERO) > 0
                    ? new BigDecimal("100") : BigDecimal.ZERO;
        }
        return current.subtract(previous)
                .divide(previous, 2, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    private BigDecimal calculateAverageTransaction(BigDecimal totalRevenue, Integer count) {
        if (count == null || count == 0) {
            return BigDecimal.ZERO;
        }
        return totalRevenue.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }
}
