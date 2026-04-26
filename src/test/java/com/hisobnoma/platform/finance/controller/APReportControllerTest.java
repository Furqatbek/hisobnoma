package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.finance.dto.APAgingReportDto;
import com.hisobnoma.platform.finance.dto.VendorBalanceReportDto;
import com.hisobnoma.platform.finance.dto.VendorBalanceReportDto.VendorBalanceDto;
import com.hisobnoma.platform.finance.service.APReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class APReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private APReportService apReportService;

    // ==================== GET /api/v1/ap/reports/aging ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AP_VIEW")
    void getAgingReport_authenticated_returns200() throws Exception {
        // Given
        APAgingReportDto report = APAgingReportDto.builder()
                .reportDate(LocalDate.now())
                .totalPayable(new BigDecimal("50000.00"))
                .current(new BigDecimal("20000.00"))
                .overdue1To30(new BigDecimal("15000.00"))
                .overdue31To60(new BigDecimal("10000.00"))
                .overdue61To90(new BigDecimal("3000.00"))
                .overdueOver90(new BigDecimal("2000.00"))
                .build();
        when(apReportService.generateAgingReport()).thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/ap/reports/aging"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPayable").value(50000.00));
    }

    @Test
    void getAgingReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/ap/reports/aging"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAgingReport_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/ap/reports/aging"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/ap/reports/vendor-balance ====================

    @Test
    @WithMockUser(authorities = "FINANCE_VENDOR_STATEMENT")
    void getVendorBalanceReport_authenticated_returns200() throws Exception {
        // Given
        VendorBalanceReportDto report = VendorBalanceReportDto.builder()
                .reportDate(LocalDate.now())
                .totalPayable(new BigDecimal("50000.00"))
                .totalPayments(new BigDecimal("30000.00"))
                .netBalance(new BigDecimal("20000.00"))
                .vendorCount(5)
                .build();
        when(apReportService.generateVendorBalanceReport()).thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/ap/reports/vendor-balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vendorCount").value(5));
    }

    @Test
    void getVendorBalanceReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/ap/reports/vendor-balance"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getVendorBalanceReport_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/ap/reports/vendor-balance"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/ap/reports/vendor/{vendorId}/statement ====================

    @Test
    @WithMockUser(authorities = "FINANCE_VENDOR_STATEMENT")
    void getVendorStatement_authenticated_returns200() throws Exception {
        // Given
        VendorBalanceDto statement = VendorBalanceDto.builder()
                .vendorId(1L)
                .vendorCode("VEND-001")
                .vendorName("Test Vendor")
                .totalInvoiced(new BigDecimal("10000.00"))
                .totalPaid(new BigDecimal("8000.00"))
                .currentBalance(new BigDecimal("2000.00"))
                .openInvoiceCount(2)
                .build();
        when(apReportService.getVendorStatement(1L)).thenReturn(statement);

        // When/Then
        mockMvc.perform(get("/api/v1/ap/reports/vendor/1/statement"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.vendorCode").value("VEND-001"));
    }

    @Test
    void getVendorStatement_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/ap/reports/vendor/1/statement"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getVendorStatement_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/ap/reports/vendor/1/statement"))
                .andExpect(status().isForbidden());
    }
}
