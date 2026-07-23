package com.hisobnoma.platform.distribution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * One agent's KPIs for a period: computed actuals alongside the stored target
 * (null target fields when no target is set for the period).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentKpiDto {
    private Long agentId;
    private String agentName;

    // Actuals (computed live)
    private BigDecimal revenue;
    private Integer orders;
    private Integer visits;
    private BigDecimal cashCollected;
    private Integer customersReached;

    // Targets (from the matching period target, if any)
    private BigDecimal targetRevenue;
    private Integer targetOrders;
    private Integer targetVisits;
    private Integer targetNewCustomers;
    private BigDecimal targetCollection;

    /** revenue / targetRevenue as a percentage; null when no revenue target is set. */
    private BigDecimal revenueAchievementPercent;

    /** Orders per 100 visits (null when the agent had no visits in the period). */
    private BigDecimal strikeRatePercent;

    /** Average revenue per order (null when the agent had no orders). */
    private BigDecimal avgDropSize;
}
