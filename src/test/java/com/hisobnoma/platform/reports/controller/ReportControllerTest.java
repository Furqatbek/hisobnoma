package com.hisobnoma.platform.reports.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.reports.dto.*;
import com.hisobnoma.platform.reports.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReportService reportService;

    @MockBean
    private InventoryReportService inventoryReportService;

    @MockBean
    private SalesReportService salesReportService;

    @MockBean
    private FinancialReportService financialReportService;

    @MockBean
    private SalaryReportService salaryReportService;

    @MockBean
    private ReportExportService exportService;

    // ==================== GET /api/v1/reports/definitions ====================

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void listReportDefinitions_authenticated_returns200() throws Exception {
        when(reportService.getReportDefinitions(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/reports/definitions"))
                .andExpect(status().isOk());
    }

    @Test
    void listReportDefinitions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/definitions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void listReportDefinitions_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/definitions"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/reports/definitions/{id} ====================

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void getReportDefinition_authenticated_returns200() throws Exception {
        ReportDefinitionDTO dto = new ReportDefinitionDTO();
        when(reportService.getReportDefinition(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/reports/definitions/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getReportDefinition_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/definitions/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getReportDefinition_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/definitions/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/reports/inventory/stock-on-hand ====================

    @Test
    @WithMockUser(authorities = "REPORT_INVENTORY_VIEW")
    void generateStockOnHandReport_authenticated_returns200() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();
        StockOnHandReportDTO report = new StockOnHandReportDTO();
        when(inventoryReportService.generateStockOnHandReport(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/reports/inventory/stock-on-hand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void generateStockOnHandReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/inventory/stock-on-hand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void generateStockOnHandReport_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/inventory/stock-on-hand")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/reports/inventory/valuation ====================

    @Test
    @WithMockUser(authorities = "REPORT_INVENTORY_VIEW")
    void generateInventoryValuationReport_authenticated_returns200() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();
        InventoryValuationReportDTO report = new InventoryValuationReportDTO();
        when(inventoryReportService.generateInventoryValuationReport(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/reports/inventory/valuation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void generateInventoryValuationReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/inventory/valuation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/reports/sales/summary ====================

    @Test
    @WithMockUser(authorities = "REPORT_SALES_VIEW")
    void generateSalesSummaryReport_authenticated_returns200() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();
        SalesSummaryReportDTO report = new SalesSummaryReportDTO();
        when(salesReportService.generateSalesSummaryReport(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/reports/sales/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void generateSalesSummaryReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/sales/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void generateSalesSummaryReport_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/sales/summary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/reports/financial/trial-balance ====================

    @Test
    @WithMockUser(authorities = "REPORT_FINANCIAL_VIEW")
    void generateTrialBalanceReport_authenticated_returns200() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();
        TrialBalanceReportDTO report = new TrialBalanceReportDTO();
        when(financialReportService.generateTrialBalanceReport(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/reports/financial/trial-balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void generateTrialBalanceReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/financial/trial-balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void generateTrialBalanceReport_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/financial/trial-balance")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/reports/financial/income-statement ====================

    @Test
    @WithMockUser(authorities = "REPORT_FINANCIAL_VIEW")
    void generateIncomeStatement_authenticated_returns200() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();
        IncomeStatementDTO report = new IncomeStatementDTO();
        when(financialReportService.generateIncomeStatement(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/reports/financial/income-statement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void generateIncomeStatement_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/financial/income-statement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/reports/financial/ar-aging ====================

    @Test
    @WithMockUser(authorities = "REPORT_FINANCIAL_VIEW")
    void generateARAgingReport_authenticated_returns200() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();
        AgingReportDTO report = new AgingReportDTO();
        when(financialReportService.generateARAgingReport(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/reports/financial/ar-aging")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void generateARAgingReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/financial/ar-aging")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/reports/financial/ap-aging ====================

    @Test
    @WithMockUser(authorities = "REPORT_FINANCIAL_VIEW")
    void generateAPAgingReport_authenticated_returns200() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();
        AgingReportDTO report = new AgingReportDTO();
        when(financialReportService.generateAPAgingReport(any())).thenReturn(report);

        mockMvc.perform(post("/api/v1/reports/financial/ap-aging")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void generateAPAgingReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/financial/ap-aging")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/reports/hr/salary ====================

    @Test
    @WithMockUser(authorities = "REPORT_FINANCIAL_VIEW")
    void generateSalaryReport_authenticated_returns200() throws Exception {
        SalaryReportDTO report = new SalaryReportDTO();
        when(salaryReportService.generateSalaryReport(2026, 4)).thenReturn(report);

        mockMvc.perform(get("/api/v1/reports/hr/salary")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isOk());
    }

    @Test
    void generateSalaryReport_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/hr/salary")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void generateSalaryReport_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/hr/salary")
                        .param("year", "2026")
                        .param("month", "4"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/reports/schedules ====================

    @Test
    @WithMockUser(authorities = "REPORT_SCHEDULE_VIEW")
    void listSchedules_authenticated_returns200() throws Exception {
        when(reportService.getSchedules(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/reports/schedules"))
                .andExpect(status().isOk());
    }

    @Test
    void listSchedules_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/schedules"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void listSchedules_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/schedules"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/reports/schedules ====================

    @Test
    @WithMockUser(authorities = "REPORT_SCHEDULE_MANAGE")
    void createSchedule_authenticated_returns200() throws Exception {
        ReportScheduleDTO dto = new ReportScheduleDTO();
        when(reportService.createSchedule(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/reports/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void createSchedule_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createSchedule_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/reports/schedules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/reports/schedules/{id} ====================

    @Test
    @WithMockUser(authorities = "REPORT_SCHEDULE_MANAGE")
    void deleteSchedule_authenticated_returns200() throws Exception {
        doNothing().when(reportService).deleteSchedule(1L);

        mockMvc.perform(delete("/api/v1/reports/schedules/1"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteSchedule_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/reports/schedules/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void deleteSchedule_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/reports/schedules/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/reports/schedules/{id}/toggle ====================

    @Test
    @WithMockUser(authorities = "REPORT_SCHEDULE_MANAGE")
    void toggleSchedule_authenticated_returns200() throws Exception {
        ReportScheduleDTO dto = new ReportScheduleDTO();
        when(reportService.toggleSchedule(1L, true)).thenReturn(dto);

        mockMvc.perform(put("/api/v1/reports/schedules/1/toggle")
                        .param("active", "true"))
                .andExpect(status().isOk());
    }

    @Test
    void toggleSchedule_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/reports/schedules/1/toggle")
                        .param("active", "true"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/reports/executions ====================

    @Test
    @WithMockUser(authorities = "REPORT_VIEW")
    void listExecutions_authenticated_returns200() throws Exception {
        when(reportService.getExecutions(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/reports/executions"))
                .andExpect(status().isOk());
    }

    @Test
    void listExecutions_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/executions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void listExecutions_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/reports/executions"))
                .andExpect(status().isForbidden());
    }
}
