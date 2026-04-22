package com.hisobnoma.platform.admin.controller;

import com.hisobnoma.platform.admin.dto.DashboardStatsDTO;
import com.hisobnoma.platform.admin.service.AdminDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminDashboardService adminDashboardService;

    @Test
    @WithMockUser(authorities = "ADMIN_DASHBOARD_VIEW")
    void getDashboardStats_authenticatedAdmin_returns200WithStatsBody() throws Exception {
        // Given
        DashboardStatsDTO stats = DashboardStatsDTO.builder()
                .totalUsers(10L)
                .activeUsers(8L)
                .totalProducts(50L)
                .totalSalesToday(new BigDecimal("5000.00"))
                .lowStockProducts(3L)
                .outOfStockProducts(1L)
                .totalInventoryValue(BigDecimal.ZERO)
                .totalReceivables(BigDecimal.ZERO)
                .totalPayables(BigDecimal.ZERO)
                .cashBalance(BigDecimal.ZERO)
                .bankBalance(BigDecimal.ZERO)
                .moduleActivities(Collections.emptyList())
                .topActiveUsers(Collections.emptyList())
                .recentActivities(Collections.emptyList())
                .build();
        when(adminDashboardService.getDashboardStats()).thenReturn(stats);

        // When / Then
        mockMvc.perform(get("/api/v1/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(10))
                .andExpect(jsonPath("$.data.totalProducts").value(50))
                .andExpect(jsonPath("$.data.totalSalesToday").value(5000.00))
                .andExpect(jsonPath("$.data.lowStockProducts").value(3));
    }

    @Test
    void getDashboardStats_unauthenticated_returns401() throws Exception {
        // When / Then — no auth token
        mockMvc.perform(get("/api/v1/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getDashboardStats_noAdminPermission_returns403() throws Exception {
        // When / Then — authenticated but lacks ADMIN_DASHBOARD_VIEW
        mockMvc.perform(get("/api/v1/admin/dashboard/stats"))
                .andExpect(status().isForbidden());
    }
}
