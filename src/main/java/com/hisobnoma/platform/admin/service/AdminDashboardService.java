package com.hisobnoma.platform.admin.service;

import com.hisobnoma.platform.admin.dto.DashboardStatsDTO;
import com.hisobnoma.platform.admin.entity.AuditLog;
import com.hisobnoma.platform.admin.repository.AuditLogRepository;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Instant now = Instant.now();
        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfWeek = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        return DashboardStatsDTO.builder()
                // User statistics
                .totalUsers(getUserCount(tenantId))
                .activeUsers(getActiveUserCount(tenantId))
                .newUsersToday(getNewUserCount(tenantId, startOfToday))
                .newUsersThisWeek(getNewUserCount(tenantId, startOfWeek))
                .newUsersThisMonth(getNewUserCount(tenantId, startOfMonth))

                // Activity statistics
                .totalAuditLogsToday(getAuditLogCount(tenantId, startOfToday))
                .failedLoginsToday(getFailedLoginCount(tenantId, startOfToday))
                .moduleActivities(getModuleActivities(tenantId))
                .topActiveUsers(getTopActiveUsers(tenantId))

                // Recent activities
                .recentActivities(getRecentActivities(tenantId))

                // Placeholder values for sales/purchase/inventory (to be integrated with other modules)
                .totalSalesToday(BigDecimal.ZERO)
                .totalSalesThisWeek(BigDecimal.ZERO)
                .totalSalesThisMonth(BigDecimal.ZERO)
                .salesCountToday(0L)
                .salesCountThisWeek(0L)
                .salesCountThisMonth(0L)
                .totalPurchasesToday(BigDecimal.ZERO)
                .totalPurchasesThisWeek(BigDecimal.ZERO)
                .totalPurchasesThisMonth(BigDecimal.ZERO)
                .totalProducts(0L)
                .activeProducts(0L)
                .lowStockProducts(0L)
                .outOfStockProducts(0L)
                .totalInventoryValue(BigDecimal.ZERO)
                .totalReceivables(BigDecimal.ZERO)
                .totalPayables(BigDecimal.ZERO)
                .cashBalance(BigDecimal.ZERO)
                .bankBalance(BigDecimal.ZERO)

                .build();
    }

    private Long getUserCount(Long tenantId) {
        try {
            return userRepository.countByTenantId(tenantId);
        } catch (Exception e) {
            log.warn("Failed to get user count: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getActiveUserCount(Long tenantId) {
        try {
            return userRepository.countByTenantIdAndEnabledTrue(tenantId);
        } catch (Exception e) {
            log.warn("Failed to get active user count: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getNewUserCount(Long tenantId, Instant since) {
        try {
            return userRepository.countByTenantIdAndCreatedAtAfter(tenantId, since);
        } catch (Exception e) {
            log.warn("Failed to get new user count: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getAuditLogCount(Long tenantId, Instant since) {
        try {
            return auditLogRepository.findByTenantIdAndDateRange(tenantId, since, Instant.now(), PageRequest.of(0, 1))
                    .getTotalElements();
        } catch (Exception e) {
            log.warn("Failed to get audit log count: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getFailedLoginCount(Long tenantId, Instant since) {
        try {
            return auditLogRepository.countFailedLoginsSince(tenantId, since);
        } catch (Exception e) {
            log.warn("Failed to get failed login count: {}", e.getMessage());
            return 0L;
        }
    }

    private List<DashboardStatsDTO.ModuleActivityDTO> getModuleActivities(Long tenantId) {
        try {
            Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
            List<Object[]> results = auditLogRepository.countModuleActivityByTenantIdSince(tenantId, since);
            List<DashboardStatsDTO.ModuleActivityDTO> activities = new ArrayList<>();
            for (Object[] row : results) {
                if (row[0] != null) {
                    activities.add(DashboardStatsDTO.ModuleActivityDTO.builder()
                            .module((String) row[0])
                            .activityCount((Long) row[1])
                            .build());
                }
            }
            return activities;
        } catch (Exception e) {
            log.warn("Failed to get module activities: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<DashboardStatsDTO.UserActivityDTO> getTopActiveUsers(Long tenantId) {
        try {
            Instant since = Instant.now().minus(7, ChronoUnit.DAYS);
            List<Object[]> results = auditLogRepository.findMostActiveUsersByTenantIdSince(tenantId, since);
            List<DashboardStatsDTO.UserActivityDTO> users = new ArrayList<>();
            int count = 0;
            for (Object[] row : results) {
                if (count >= 10) break;
                if (row[0] != null) {
                    users.add(DashboardStatsDTO.UserActivityDTO.builder()
                            .userId((Long) row[0])
                            .username((String) row[1])
                            .activityCount((Long) row[2])
                            .build());
                    count++;
                }
            }
            return users;
        } catch (Exception e) {
            log.warn("Failed to get top active users: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<DashboardStatsDTO.RecentActivityDTO> getRecentActivities(Long tenantId) {
        try {
            List<AuditLog> logs = auditLogRepository.findByTenantId(tenantId, PageRequest.of(0, 20)).getContent();
            List<DashboardStatsDTO.RecentActivityDTO> activities = new ArrayList<>();
            for (AuditLog log : logs) {
                activities.add(DashboardStatsDTO.RecentActivityDTO.builder()
                        .description(log.getDescription())
                        .action(log.getAction().name())
                        .entityType(log.getEntityType())
                        .username(log.getUsername())
                        .timestamp(log.getActionTimestamp().toString())
                        .build());
            }
            return activities;
        } catch (Exception e) {
            log.warn("Failed to get recent activities: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
