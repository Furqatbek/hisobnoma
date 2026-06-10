package com.hisobnoma.platform.admin.service;

import com.hisobnoma.platform.admin.dto.DashboardStatsDTO;
import com.hisobnoma.platform.admin.entity.AuditLog;
import com.hisobnoma.platform.admin.repository.AuditLogRepository;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.finance.repository.APInvoiceRepository;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.PurchaseOrderRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import com.hisobnoma.platform.web.entity.WebCatalogStatus;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebCatalogItemRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private SecurityContextHelper securityContextHelper;
    @Mock
    private POSTransactionRepository posTransactionRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;
    @Mock
    private ARInvoiceRepository arInvoiceRepository;
    @Mock
    private APInvoiceRepository apInvoiceRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private WebCatalogItemRepository webCatalogItemRepository;
    @Mock
    private WebOrderRepository webOrderRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    void getDashboardStats_activeTenant_returnsNonZeroCounts() {
        // Given
        when(userRepository.countByTenantId(TENANT_ID)).thenReturn(15L);
        when(userRepository.countByTenantIdAndEnabledTrue(TENANT_ID)).thenReturn(12L);
        when(userRepository.countByTenantIdAndCreatedAtAfter(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(2L);

        when(posTransactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(), any()))
                .thenReturn(new BigDecimal("50000.00"));
        when(posTransactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(), any()))
                .thenReturn(25);

        when(purchaseOrderRepository.sumTotalByDateRange(eq(TENANT_ID), any(), any()))
                .thenReturn(new BigDecimal("30000.00"));

        when(productRepository.countByTenantId(TENANT_ID)).thenReturn(100L);
        when(productRepository.countActiveByTenantId(TENANT_ID)).thenReturn(90L);
        when(stockRepository.countLowStockProducts(TENANT_ID)).thenReturn(5);
        when(stockRepository.countOutOfStockProducts(TENANT_ID)).thenReturn(2);
        when(stockRepository.calculateTotalInventoryValue(TENANT_ID))
                .thenReturn(new BigDecimal("500000.00"));

        when(arInvoiceRepository.sumTotalBalanceDue(eq(TENANT_ID), anyList()))
                .thenReturn(new BigDecimal("80000.00"));
        when(apInvoiceRepository.sumTotalBalanceDue(eq(TENANT_ID), anyList()))
                .thenReturn(new BigDecimal("60000.00"));
        when(bankAccountRepository.sumBalanceByTypes(eq(TENANT_ID), anyList()))
                .thenReturn(new BigDecimal("100000.00"));
        when(bankAccountRepository.sumBalanceExcludingTypes(eq(TENANT_ID), anyList()))
                .thenReturn(new BigDecimal("200000.00"));

        Page<AuditLog> auditPage = new PageImpl<>(List.of(AuditLog.builder().build()),
                PageRequest.of(0, 1), 10L);
        when(auditLogRepository.findByTenantIdAndDateRange(eq(TENANT_ID), any(), any(), any()))
                .thenReturn(auditPage);
        when(auditLogRepository.countFailedLoginsSince(eq(TENANT_ID), any()))
                .thenReturn(3L);
        List<Object[]> moduleRows1 = new ArrayList<>();
        moduleRows1.add(new Object[]{"INVENTORY", 20L});
        when(auditLogRepository.countModuleActivityByTenantIdSince(eq(TENANT_ID), any()))
                .thenReturn(moduleRows1);
        List<Object[]> userRows1 = new ArrayList<>();
        userRows1.add(new Object[]{1L, "admin", 50L});
        when(auditLogRepository.findMostActiveUsersByTenantIdSince(eq(TENANT_ID), any()))
                .thenReturn(userRows1);
        when(auditLogRepository.findByTenantId(eq(TENANT_ID), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        when(webCatalogItemRepository.countByTenantIdAndStatus(TENANT_ID, WebCatalogStatus.LIVE))
                .thenReturn(4L);
        when(webCatalogItemRepository.countByTenantIdAndStatus(TENANT_ID, WebCatalogStatus.DRAFT))
                .thenReturn(2L);

        when(webOrderRepository.countByTenantIdAndStatus(TENANT_ID, WebOrderStatus.NEW))
                .thenReturn(3L);
        when(webOrderRepository.countByTenantIdAndCreatedAtAfter(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(5L);
        WebOrder recentOrder = WebOrder.builder()
                .orderNumber("WO-000001").status(WebOrderStatus.NEW)
                .customerName("Ali").totalAmount(new BigDecimal("36000"))
                .tenantId(TENANT_ID).build();
        when(webOrderRepository.findRecentByTenant(eq(TENANT_ID), any()))
                .thenReturn(List.of(recentOrder));

        // When
        DashboardStatsDTO result = adminDashboardService.getDashboardStats();

        // Then
        assertNotNull(result);
        assertTrue(result.getTotalUsers() > 0);
        assertTrue(result.getTotalProducts() > 0);
        assertTrue(result.getTotalSalesToday().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(result.getLowStockProducts() > 0);
        assertEquals(4L, result.getCatalogLiveCount());
        assertEquals(2L, result.getCatalogDraftCount());
        assertEquals(3L, result.getNewOnlineOrders());
        assertEquals(5L, result.getOnlineOrdersToday());
        assertEquals(1, result.getRecentOnlineOrders().size());
        assertEquals("WO-000001", result.getRecentOnlineOrders().get(0).getOrderNumber());
    }

    @Test
    void getDashboardStats_newTenant_returnsAllZeroStats() {
        // Given
        when(userRepository.countByTenantId(TENANT_ID)).thenReturn(0L);
        when(userRepository.countByTenantIdAndEnabledTrue(TENANT_ID)).thenReturn(0L);
        when(userRepository.countByTenantIdAndCreatedAtAfter(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(0L);

        when(posTransactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(), any()))
                .thenReturn(null);
        when(posTransactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(), any()))
                .thenReturn(null);

        when(purchaseOrderRepository.sumTotalByDateRange(eq(TENANT_ID), any(), any()))
                .thenReturn(null);

        when(productRepository.countByTenantId(TENANT_ID)).thenReturn(0L);
        when(productRepository.countActiveByTenantId(TENANT_ID)).thenReturn(0L);
        when(stockRepository.countLowStockProducts(TENANT_ID)).thenReturn(null);
        when(stockRepository.countOutOfStockProducts(TENANT_ID)).thenReturn(null);
        when(stockRepository.calculateTotalInventoryValue(TENANT_ID)).thenReturn(null);

        when(arInvoiceRepository.sumTotalBalanceDue(eq(TENANT_ID), anyList())).thenReturn(null);
        when(apInvoiceRepository.sumTotalBalanceDue(eq(TENANT_ID), anyList())).thenReturn(null);
        when(bankAccountRepository.sumBalanceByTypes(eq(TENANT_ID), anyList())).thenReturn(null);
        when(bankAccountRepository.sumBalanceExcludingTypes(eq(TENANT_ID), anyList())).thenReturn(null);

        Page<AuditLog> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1), 0L);
        when(auditLogRepository.findByTenantIdAndDateRange(eq(TENANT_ID), any(), any(), any()))
                .thenReturn(emptyPage);
        when(auditLogRepository.countFailedLoginsSince(eq(TENANT_ID), any())).thenReturn(0L);
        when(auditLogRepository.countModuleActivityByTenantIdSince(eq(TENANT_ID), any()))
                .thenReturn(Collections.emptyList());
        when(auditLogRepository.findMostActiveUsersByTenantIdSince(eq(TENANT_ID), any()))
                .thenReturn(Collections.emptyList());
        when(auditLogRepository.findByTenantId(eq(TENANT_ID), any()))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        // When
        DashboardStatsDTO result = adminDashboardService.getDashboardStats();

        // Then
        assertNotNull(result);
        assertEquals(0L, result.getTotalUsers());
        assertEquals(0L, result.getActiveUsers());
        assertEquals(0L, result.getNewUsersToday());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalSalesToday()));
        assertEquals(0L, result.getSalesCountToday());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalPurchasesToday()));
        assertEquals(0L, result.getTotalProducts());
        assertEquals(0L, result.getLowStockProducts());
        assertEquals(0L, result.getOutOfStockProducts());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalInventoryValue()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalReceivables()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getTotalPayables()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getCashBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getBankBalance()));
        assertEquals(0L, result.getTotalAuditLogsToday());
        assertEquals(0L, result.getFailedLoginsToday());
        assertTrue(result.getModuleActivities().isEmpty());
        assertTrue(result.getTopActiveUsers().isEmpty());
        assertTrue(result.getRecentActivities().isEmpty());
    }

    @Test
    void getDashboardStats_containsAllExpectedFields() {
        // Given
        when(userRepository.countByTenantId(TENANT_ID)).thenReturn(10L);
        when(userRepository.countByTenantIdAndEnabledTrue(TENANT_ID)).thenReturn(8L);
        when(userRepository.countByTenantIdAndCreatedAtAfter(eq(TENANT_ID), any(Instant.class)))
                .thenReturn(1L);

        when(posTransactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(), any()))
                .thenReturn(new BigDecimal("1000.00"));
        when(posTransactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(), any()))
                .thenReturn(5);

        when(purchaseOrderRepository.sumTotalByDateRange(eq(TENANT_ID), any(), any()))
                .thenReturn(new BigDecimal("2000.00"));

        when(productRepository.countByTenantId(TENANT_ID)).thenReturn(50L);
        when(productRepository.countActiveByTenantId(TENANT_ID)).thenReturn(45L);
        when(stockRepository.countLowStockProducts(TENANT_ID)).thenReturn(3);
        when(stockRepository.countOutOfStockProducts(TENANT_ID)).thenReturn(1);
        when(stockRepository.calculateTotalInventoryValue(TENANT_ID))
                .thenReturn(new BigDecimal("100000.00"));

        when(arInvoiceRepository.sumTotalBalanceDue(eq(TENANT_ID), anyList()))
                .thenReturn(new BigDecimal("5000.00"));
        when(apInvoiceRepository.sumTotalBalanceDue(eq(TENANT_ID), anyList()))
                .thenReturn(new BigDecimal("3000.00"));
        when(bankAccountRepository.sumBalanceByTypes(eq(TENANT_ID), anyList()))
                .thenReturn(new BigDecimal("10000.00"));
        when(bankAccountRepository.sumBalanceExcludingTypes(eq(TENANT_ID), anyList()))
                .thenReturn(new BigDecimal("20000.00"));

        Page<AuditLog> auditPage = new PageImpl<>(List.of(AuditLog.builder().build()),
                PageRequest.of(0, 1), 5L);
        when(auditLogRepository.findByTenantIdAndDateRange(eq(TENANT_ID), any(), any(), any()))
                .thenReturn(auditPage);
        when(auditLogRepository.countFailedLoginsSince(eq(TENANT_ID), any())).thenReturn(1L);
        List<Object[]> moduleRows2 = new ArrayList<>();
        moduleRows2.add(new Object[]{"POS", 10L});
        when(auditLogRepository.countModuleActivityByTenantIdSince(eq(TENANT_ID), any()))
                .thenReturn(moduleRows2);
        List<Object[]> userRows2 = new ArrayList<>();
        userRows2.add(new Object[]{2L, "cashier", 30L});
        when(auditLogRepository.findMostActiveUsersByTenantIdSince(eq(TENANT_ID), any()))
                .thenReturn(userRows2);
        when(auditLogRepository.findByTenantId(eq(TENANT_ID), any()))
                .thenReturn(new PageImpl<>(List.of(
                        AuditLog.builder()
                                .description("Product created")
                                .action(AuditLog.AuditAction.CREATE)
                                .entityType("Product")
                                .username("admin")
                                .actionTimestamp(Instant.now())
                                .build()
                )));

        // When
        DashboardStatsDTO result = adminDashboardService.getDashboardStats();

        // Then — every field is non-null
        assertNotNull(result.getTotalUsers());
        assertNotNull(result.getActiveUsers());
        assertNotNull(result.getNewUsersToday());
        assertNotNull(result.getNewUsersThisWeek());
        assertNotNull(result.getNewUsersThisMonth());
        assertNotNull(result.getTotalSalesToday());
        assertNotNull(result.getTotalSalesThisWeek());
        assertNotNull(result.getTotalSalesThisMonth());
        assertNotNull(result.getSalesCountToday());
        assertNotNull(result.getSalesCountThisWeek());
        assertNotNull(result.getSalesCountThisMonth());
        assertNotNull(result.getTotalPurchasesToday());
        assertNotNull(result.getTotalPurchasesThisWeek());
        assertNotNull(result.getTotalPurchasesThisMonth());
        assertNotNull(result.getTotalProducts());
        assertNotNull(result.getActiveProducts());
        assertNotNull(result.getLowStockProducts());
        assertNotNull(result.getOutOfStockProducts());
        assertNotNull(result.getTotalInventoryValue());
        assertNotNull(result.getTotalReceivables());
        assertNotNull(result.getTotalPayables());
        assertNotNull(result.getCashBalance());
        assertNotNull(result.getBankBalance());
        assertNotNull(result.getTotalAuditLogsToday());
        assertNotNull(result.getFailedLoginsToday());
        assertNotNull(result.getModuleActivities());
        assertNotNull(result.getTopActiveUsers());
        assertNotNull(result.getRecentActivities());

        // Verify specific seeded values
        assertEquals(10L, result.getTotalUsers());
        assertEquals(8L, result.getActiveUsers());
        assertEquals(50L, result.getTotalProducts());
        assertEquals(45L, result.getActiveProducts());
        assertEquals(3L, result.getLowStockProducts());

        // Verify nested DTOs
        assertEquals(1, result.getModuleActivities().size());
        assertEquals("POS", result.getModuleActivities().get(0).getModule());
        assertEquals(1, result.getTopActiveUsers().size());
        assertEquals("cashier", result.getTopActiveUsers().get(0).getUsername());
        assertEquals(1, result.getRecentActivities().size());
        assertEquals("Product created", result.getRecentActivities().get(0).getDescription());
    }
}
