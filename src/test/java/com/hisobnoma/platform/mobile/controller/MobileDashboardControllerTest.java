package com.hisobnoma.platform.mobile.controller;

import com.hisobnoma.platform.mobile.dto.FinancialSummaryDTO;
import com.hisobnoma.platform.mobile.dto.InventorySummaryDTO;
import com.hisobnoma.platform.mobile.dto.RevenueSummaryDTO;
import com.hisobnoma.platform.mobile.service.MobileDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MobileDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MobileDashboardService dashboardService;

    // ==================== GET /api/v1/mobile/dashboard/revenue ====================

    @Test
    @WithMockUser(authorities = "MOBILE_DASHBOARD_VIEW")
    void getRevenueSummary_authenticated_returns200() throws Exception {
        RevenueSummaryDTO dto = new RevenueSummaryDTO();
        when(dashboardService.getRevenueSummary()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/mobile/dashboard/revenue"))
                .andExpect(status().isOk());
    }

    @Test
    void getRevenueSummary_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/revenue"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getRevenueSummary_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/revenue"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/dashboard/revenue/chart ====================

    @Test
    @WithMockUser(authorities = "MOBILE_DASHBOARD_VIEW")
    void getRevenueChartData_authenticated_returns200() throws Exception {
        RevenueSummaryDTO dto = new RevenueSummaryDTO();
        when(dashboardService.getRevenueChartData(any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/mobile/dashboard/revenue/chart"))
                .andExpect(status().isOk());
    }

    @Test
    void getRevenueChartData_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/revenue/chart"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getRevenueChartData_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/revenue/chart"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/dashboard/inventory ====================

    @Test
    @WithMockUser(authorities = "MOBILE_DASHBOARD_VIEW")
    void getInventorySummary_authenticated_returns200() throws Exception {
        InventorySummaryDTO dto = new InventorySummaryDTO();
        when(dashboardService.getInventorySummary()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/mobile/dashboard/inventory"))
                .andExpect(status().isOk());
    }

    @Test
    void getInventorySummary_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/inventory"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getInventorySummary_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/inventory"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/dashboard/finance ====================

    @Test
    @WithMockUser(authorities = "MOBILE_DASHBOARD_VIEW")
    void getFinancialSummary_authenticated_returns200() throws Exception {
        FinancialSummaryDTO dto = new FinancialSummaryDTO();
        when(dashboardService.getFinancialSummary()).thenReturn(dto);

        mockMvc.perform(get("/api/v1/mobile/dashboard/finance"))
                .andExpect(status().isOk());
    }

    @Test
    void getFinancialSummary_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/finance"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getFinancialSummary_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/dashboard/finance"))
                .andExpect(status().isForbidden());
    }
}
