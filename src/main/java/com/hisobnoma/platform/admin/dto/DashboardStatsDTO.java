package com.hisobnoma.platform.admin.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {

    // User statistics
    private Long totalUsers;
    private Long activeUsers;
    private Long newUsersToday;
    private Long newUsersThisWeek;
    private Long newUsersThisMonth;

    // Sales statistics
    private BigDecimal totalSalesToday;
    private BigDecimal totalSalesThisWeek;
    private BigDecimal totalSalesThisMonth;
    private Long salesCountToday;
    private Long salesCountThisWeek;
    private Long salesCountThisMonth;

    // Purchase statistics
    private BigDecimal totalPurchasesToday;
    private BigDecimal totalPurchasesThisWeek;
    private BigDecimal totalPurchasesThisMonth;

    // Inventory statistics
    private Long totalProducts;
    private Long activeProducts;
    private Long lowStockProducts;
    private Long outOfStockProducts;
    private BigDecimal totalInventoryValue;

    // Finance statistics
    private BigDecimal totalReceivables;
    private BigDecimal totalPayables;
    private BigDecimal cashBalance;
    private BigDecimal bankBalance;

    // Web catalog statistics
    private Long catalogLiveCount;
    private Long catalogDraftCount;

    // Activity statistics
    private Long totalAuditLogsToday;
    private Long failedLoginsToday;
    private List<ModuleActivityDTO> moduleActivities;
    private List<UserActivityDTO> topActiveUsers;

    // Recent activities
    private List<RecentActivityDTO> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ModuleActivityDTO {
        private String module;
        private Long activityCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserActivityDTO {
        private Long userId;
        private String username;
        private Long activityCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivityDTO {
        private String description;
        private String action;
        private String entityType;
        private String username;
        private String timestamp;
    }
}
