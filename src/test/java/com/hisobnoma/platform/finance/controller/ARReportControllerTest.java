package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.finance.dto.ARAgingReportDto;
import com.hisobnoma.platform.finance.dto.CustomerBalanceReportDto;
import com.hisobnoma.platform.finance.dto.CustomerBalanceReportDto.CustomerBalanceDto;
import com.hisobnoma.platform.finance.service.ARReportService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ARReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ARReportService arReportService;

    // ==================== GET /api/v1/finance/ar-reports/aging ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getAgingReport_authenticated_returns200() throws Exception {
        // Given
        ARAgingReportDto report = ARAgingReportDto.builder()
                .reportDate(LocalDate.now())
                .totalCurrent(new BigDecimal("20000.00"))
                .total1To30Days(new BigDecimal("15000.00"))
                .total31To60Days(new BigDecimal("10000.00"))
                .total61To90Days(new BigDecimal("3000.00"))
                .totalOver90Days(new BigDecimal("2000.00"))
                .grandTotal(new BigDecimal("50000.00"))
                .customerCount(10)
                .build();
        when(arReportService.generateAgingReport(any(LocalDate.class))).thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/ar-reports/aging"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.grandTotal").value(50000.00));
    }

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getAgingReport_withAsOfDate_returns200() throws Exception {
        // Given
        ARAgingReportDto report = ARAgingReportDto.builder()
                .reportDate(LocalDate.of(2026, 1, 31))
                .grandTotal(new BigDecimal("45000.00"))
                .customerCount(8)
                .build();
        when(arReportService.generateAgingReport(LocalDate.of(2026, 1, 31))).thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/ar-reports/aging")
                        .param("asOfDate", "2026-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerCount").value(8));
    }

    @Test
    void getAgingReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-reports/aging"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAgingReport_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-reports/aging"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/ar-reports/customer-balance ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCustomerBalanceReport_authenticated_returns200() throws Exception {
        // Given
        CustomerBalanceReportDto report = CustomerBalanceReportDto.builder()
                .reportDate(LocalDate.now())
                .totalReceivables(new BigDecimal("100000.00"))
                .totalCredits(new BigDecimal("5000.00"))
                .totalNetBalance(new BigDecimal("95000.00"))
                .customerCount(15)
                .customersOverCreditLimit(2)
                .customersOnCreditHold(1)
                .build();
        when(arReportService.generateCustomerBalanceReport()).thenReturn(report);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/ar-reports/customer-balance"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerCount").value(15));
    }

    @Test
    void getCustomerBalanceReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-reports/customer-balance"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCustomerBalanceReport_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-reports/customer-balance"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/ar-reports/customer-balance/{customerId} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_AR_READ")
    void getCustomerBalance_authenticated_returns200() throws Exception {
        // Given
        CustomerBalanceDto balance = CustomerBalanceDto.builder()
                .customerId(1L)
                .customerCode("CUST-001")
                .customerName("Test Customer")
                .outstandingInvoices(new BigDecimal("10000.00"))
                .availableCredits(new BigDecimal("500.00"))
                .netBalance(new BigDecimal("9500.00"))
                .creditLimit(new BigDecimal("50000.00"))
                .overCreditLimit(false)
                .build();
        when(arReportService.getCustomerBalance(1L)).thenReturn(balance);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/ar-reports/customer-balance/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerCode").value("CUST-001"));
    }

    @Test
    void getCustomerBalance_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-reports/customer-balance/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getCustomerBalance_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/ar-reports/customer-balance/1"))
                .andExpect(status().isForbidden());
    }
}
