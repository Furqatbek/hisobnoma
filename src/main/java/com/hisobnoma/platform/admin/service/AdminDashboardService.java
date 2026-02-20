package com.hisobnoma.platform.admin.service;

import com.hisobnoma.platform.admin.dto.DashboardStatsDTO;
import com.hisobnoma.platform.admin.entity.AuditLog;
import com.hisobnoma.platform.admin.repository.AuditLogRepository;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.entity.APInvoiceStatus;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import com.hisobnoma.platform.finance.entity.BankAccountType;
import com.hisobnoma.platform.finance.repository.APInvoiceRepository;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.PurchaseOrderRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
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
    private final POSTransactionRepository posTransactionRepository;
    private final ProductRepository productRepository;
    private final StockRepository stockRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final ARInvoiceRepository arInvoiceRepository;
    private final APInvoiceRepository apInvoiceRepository;
    private final BankAccountRepository bankAccountRepository;

    @Transactional(readOnly = true)
    public DashboardStatsDTO getDashboardStats() {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Instant now = Instant.now();
        Instant startOfToday = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfWeek = LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant();

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(7);
        LocalDate monthStart = today.withDayOfMonth(1);

        return DashboardStatsDTO.builder()
                // User statistics
                .totalUsers(getUserCount(tenantId))
                .activeUsers(getActiveUserCount(tenantId))
                .newUsersToday(getNewUserCount(tenantId, startOfToday))
                .newUsersThisWeek(getNewUserCount(tenantId, startOfWeek))
                .newUsersThisMonth(getNewUserCount(tenantId, startOfMonth))

                // Sales statistics
                .totalSalesToday(getSalesTotal(tenantId, startOfToday, now))
                .totalSalesThisWeek(getSalesTotal(tenantId, startOfWeek, now))
                .totalSalesThisMonth(getSalesTotal(tenantId, startOfMonth, now))
                .salesCountToday(getSalesCount(tenantId, startOfToday, now))
                .salesCountThisWeek(getSalesCount(tenantId, startOfWeek, now))
                .salesCountThisMonth(getSalesCount(tenantId, startOfMonth, now))

                // Purchase statistics
                .totalPurchasesToday(getPurchaseTotal(tenantId, today, today))
                .totalPurchasesThisWeek(getPurchaseTotal(tenantId, weekAgo, today))
                .totalPurchasesThisMonth(getPurchaseTotal(tenantId, monthStart, today))

                // Inventory statistics
                .totalProducts(getProductCount(tenantId))
                .activeProducts(getActiveProductCount(tenantId))
                .lowStockProducts(getLowStockCount(tenantId))
                .outOfStockProducts(getOutOfStockCount(tenantId))
                .totalInventoryValue(getInventoryValue(tenantId))

                // Finance statistics
                .totalReceivables(getReceivables(tenantId))
                .totalPayables(getPayables(tenantId))
                .cashBalance(getCashBalance(tenantId))
                .bankBalance(getBankBalance(tenantId))

                // Activity statistics
                .totalAuditLogsToday(getAuditLogCount(tenantId, startOfToday))
                .failedLoginsToday(getFailedLoginCount(tenantId, startOfToday))
                .moduleActivities(getModuleActivities(tenantId))
                .topActiveUsers(getTopActiveUsers(tenantId))

                // Recent activities
                .recentActivities(getRecentActivities(tenantId))

                .build();
    }

    // ---- User stats ----

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

    // ---- Sales stats ----

    private BigDecimal getSalesTotal(Long tenantId, Instant startDate, Instant endDate) {
        try {
            BigDecimal total = posTransactionRepository.sumCompletedSalesByDateRange(tenantId, startDate, endDate);
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to get sales total: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    private Long getSalesCount(Long tenantId, Instant startDate, Instant endDate) {
        try {
            Integer count = posTransactionRepository.countCompletedSalesByDateRange(tenantId, startDate, endDate);
            return count != null ? count.longValue() : 0L;
        } catch (Exception e) {
            log.warn("Failed to get sales count: {}", e.getMessage());
            return 0L;
        }
    }

    // ---- Purchase stats ----

    private BigDecimal getPurchaseTotal(Long tenantId, LocalDate startDate, LocalDate endDate) {
        try {
            BigDecimal total = purchaseOrderRepository.sumTotalByDateRange(tenantId, startDate, endDate);
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.warn("Failed to get purchase total: {}", e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // ---- Inventory stats ----

    private Long getProductCount(Long tenantId) {
        try {
            return productRepository.countByTenantId(tenantId);
        } catch (Exception e) {
            log.warn("Failed to get product count: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getActiveProductCount(Long tenantId) {
        try {
            return productRepository.countActiveByTenantId(tenantId);
        } catch (Exception e) {
            log.warn("Failed to get active product count: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getLowStockCount(Long tenantId) {
        try {
            Integer count = stockRepository.countLowStockProducts(tenantId);
            return count != null ? count.longValue() : 0L;
        } catch (Exception e) {
            log.warn("Failed to get low stock count: {}", e.getMessage());
            return 0L;
        }
    }

    private Long getOutOfStockCount(Long tenantId) {
        try {
            Integer count = stockRepository.countOutOfStockProducts(tenantId);
            return count != null ? count.longValue() : 0L;
        } catch (Exception e) {
            log.warn("Failed to get out of stock count: {}", e.getMessage());
            return 0L;
        }
    }

    private BigDecimal getInventoryValue(Long tenantId) {
        try {
            BigDecimal value = stockRepository.calculateTotalInventoryValue(tenantId);
            return value != null ? value : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Failed to get inventory value for tenant {}: {}", tenantId, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    // ---- Finance stats ----

    private BigDecimal getReceivables(Long tenantId) {
        try {
            List<ARInvoiceStatus> excludedStatuses = List.of(ARInvoiceStatus.CANCELLED, ARInvoiceStatus.PAID);
            BigDecimal total = arInvoiceRepository.sumTotalBalanceDue(tenantId, excludedStatuses);
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Failed to get receivables for tenant {}: {}", tenantId, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getPayables(Long tenantId) {
        try {
            List<APInvoiceStatus> excludedStatuses = List.of(APInvoiceStatus.CANCELLED, APInvoiceStatus.PAID);
            BigDecimal total = apInvoiceRepository.sumTotalBalanceDue(tenantId, excludedStatuses);
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Failed to get payables for tenant {}: {}", tenantId, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getCashBalance(Long tenantId) {
        try {
            List<BankAccountType> cashTypes = List.of(BankAccountType.CASH, BankAccountType.PETTY_CASH);
            BigDecimal total = bankAccountRepository.sumBalanceByTypes(tenantId, cashTypes);
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Failed to get cash balance for tenant {}: {}", tenantId, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getBankBalance(Long tenantId) {
        try {
            List<BankAccountType> cashTypes = List.of(BankAccountType.CASH, BankAccountType.PETTY_CASH);
            BigDecimal total = bankAccountRepository.sumBalanceExcludingTypes(tenantId, cashTypes);
            return total != null ? total : BigDecimal.ZERO;
        } catch (Exception e) {
            log.error("Failed to get bank balance for tenant {}: {}", tenantId, e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    // ---- Activity stats ----

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
            for (AuditLog auditLog : logs) {
                activities.add(DashboardStatsDTO.RecentActivityDTO.builder()
                        .description(auditLog.getDescription())
                        .action(auditLog.getAction().name())
                        .entityType(auditLog.getEntityType())
                        .username(auditLog.getUsername())
                        .timestamp(auditLog.getActionTimestamp().toString())
                        .build());
            }
            return activities;
        } catch (Exception e) {
            log.warn("Failed to get recent activities: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}
