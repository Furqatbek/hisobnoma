package com.hisobnoma.platform.mobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Revenue summary DTO for mobile dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RevenueSummaryDTO {

    private BigDecimal todayRevenue;
    private BigDecimal yesterdayRevenue;
    private BigDecimal thisWeekRevenue;
    private BigDecimal lastWeekRevenue;
    private BigDecimal thisMonthRevenue;
    private BigDecimal lastMonthRevenue;

    private BigDecimal todayChangePercent;
    private BigDecimal weekChangePercent;
    private BigDecimal monthChangePercent;

    private Integer todayTransactionCount;
    private Integer thisWeekTransactionCount;
    private Integer thisMonthTransactionCount;

    private BigDecimal averageTransactionValue;

    private List<ChartDataPoint> hourlyRevenue;
    private List<ChartDataPoint> dailyRevenue;
    private List<ChartDataPoint> monthlyRevenue;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChartDataPoint {
        private String label;
        private BigDecimal value;
        private Integer count;
    }
}
