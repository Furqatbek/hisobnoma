package com.hisobnoma.platform.mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MobileDashboardControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Mobile Dashboard Tenant").code("FULLFLOW_MOBDASH").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("mobdashuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());
    }

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                    new SimpleGrantedAuthority("MOBILE_DASHBOARD_VIEW"),
                    new SimpleGrantedAuthority("REPORTS_SALES_VIEW"),
                    new SimpleGrantedAuthority("INVENTORY_STOCK_READ"),
                    new SimpleGrantedAuthority("FINANCE_REPORTS_VIEW"),
                    new SimpleGrantedAuthority("ADMIN_DASHBOARD_VIEW")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("SOME_OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ==================== GET /api/v1/mobile/dashboard/revenue ====================

    @Test
    void getRevenueSummary_returnsZerosWithNoData() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/revenue").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayRevenue").value(0))
                .andExpect(jsonPath("$.data.yesterdayRevenue").value(0))
                .andExpect(jsonPath("$.data.thisWeekRevenue").value(0))
                .andExpect(jsonPath("$.data.lastWeekRevenue").value(0))
                .andExpect(jsonPath("$.data.thisMonthRevenue").value(0))
                .andExpect(jsonPath("$.data.lastMonthRevenue").value(0))
                .andExpect(jsonPath("$.data.todayChangePercent").value(0))
                .andExpect(jsonPath("$.data.weekChangePercent").value(0))
                .andExpect(jsonPath("$.data.monthChangePercent").value(0))
                .andExpect(jsonPath("$.data.todayTransactionCount").value(0))
                .andExpect(jsonPath("$.data.thisWeekTransactionCount").value(0))
                .andExpect(jsonPath("$.data.thisMonthTransactionCount").value(0))
                .andExpect(jsonPath("$.data.averageTransactionValue").value(0));
    }

    @Test
    void getRevenueSummary_forbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/revenue").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/dashboard/revenue/chart ====================

    @Test
    void getRevenueChart_daily_returnsChartData() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/revenue/chart")
                        .param("period", "daily")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyRevenue").isArray())
                .andExpect(jsonPath("$.data.dailyRevenue").isNotEmpty());
    }

    @Test
    void getRevenueChart_hourly_returnsChartData() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/revenue/chart")
                        .param("period", "hourly")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hourlyRevenue").isArray())
                .andExpect(jsonPath("$.data.hourlyRevenue").isNotEmpty());
    }

    @Test
    void getRevenueChart_monthly_returnsChartData() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/revenue/chart")
                        .param("period", "monthly")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.monthlyRevenue").isArray())
                .andExpect(jsonPath("$.data.monthlyRevenue").isNotEmpty());
    }

    // ==================== GET /api/v1/mobile/dashboard/inventory ====================

    @Test
    void getInventorySummary_returnsZerosWithNoData() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/inventory").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSkuCount").value(0))
                .andExpect(jsonPath("$.data.activeSkuCount").value(0))
                .andExpect(jsonPath("$.data.totalInventoryValue").value(0))
                .andExpect(jsonPath("$.data.lowStockCount").value(0))
                .andExpect(jsonPath("$.data.outOfStockCount").value(0))
                .andExpect(jsonPath("$.data.expiringCount").value(0));
    }

    @Test
    void getInventorySummary_forbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/inventory").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/dashboard/finance ====================

    @Test
    void getFinancialSummary_returnsZerosWithNoData() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/finance").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalBankBalance").value(0))
                .andExpect(jsonPath("$.data.totalCashBalance").value(0))
                .andExpect(jsonPath("$.data.arOutstanding").value(0))
                .andExpect(jsonPath("$.data.apOutstanding").value(0))
                .andExpect(jsonPath("$.data.todayRevenue").value(0));
    }
}
