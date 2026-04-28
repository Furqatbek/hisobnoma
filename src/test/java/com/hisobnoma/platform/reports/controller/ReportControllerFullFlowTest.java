package com.hisobnoma.platform.reports.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.reports.dto.GenerateReportRequest;
import com.hisobnoma.platform.reports.dto.ReportScheduleDTO;
import com.hisobnoma.platform.reports.entity.ReportDefinition;
import com.hisobnoma.platform.reports.entity.ReportDefinition.ExportFormat;
import com.hisobnoma.platform.reports.entity.ReportDefinition.ReportCategory;
import com.hisobnoma.platform.reports.entity.ReportExecution;
import com.hisobnoma.platform.reports.entity.ReportSchedule;
import com.hisobnoma.platform.reports.repository.ReportDefinitionRepository;
import com.hisobnoma.platform.reports.repository.ReportExecutionRepository;
import com.hisobnoma.platform.reports.repository.ReportScheduleRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReportControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/reports";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;
    @Autowired private ReportDefinitionRepository reportDefinitionRepository;
    @Autowired private ReportScheduleRepository reportScheduleRepository;
    @Autowired private ReportExecutionRepository reportExecutionRepository;

    private Tenant tenant;
    private User user;
    private ReportDefinition inventoryDef;
    private ReportDefinition financialDef;
    private ReportSchedule schedule;
    private ReportExecution execution;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Full Flow Report Tenant").code("FULLFLOW_REPORT").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("reportfullflowuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        inventoryDef = ReportDefinition.builder()
                .code("STOCK_ON_HAND")
                .name("Stock On Hand Report")
                .description("Shows current stock levels")
                .category(ReportCategory.INVENTORY)
                .supportedFormats(List.of(ExportFormat.EXCEL, ExportFormat.PDF, ExportFormat.CSV))
                .systemReport(true)
                .active(true)
                .sortOrder(1)
                .build();
        inventoryDef.setTenantId(tenant.getId());
        inventoryDef = reportDefinitionRepository.saveAndFlush(inventoryDef);

        financialDef = ReportDefinition.builder()
                .code("TRIAL_BALANCE")
                .name("Trial Balance Report")
                .description("Shows trial balance for accounting period")
                .category(ReportCategory.FINANCIAL)
                .supportedFormats(List.of(ExportFormat.EXCEL, ExportFormat.PDF))
                .systemReport(true)
                .active(true)
                .sortOrder(2)
                .build();
        financialDef.setTenantId(tenant.getId());
        financialDef = reportDefinitionRepository.saveAndFlush(financialDef);

        schedule = ReportSchedule.builder()
                .reportDefinition(inventoryDef)
                .name("Daily Stock Report")
                .frequency(ReportSchedule.ScheduleFrequency.DAILY)
                .runTime(LocalTime.of(8, 0))
                .exportFormat(ExportFormat.EXCEL)
                .deliveryMethod(ReportSchedule.DeliveryMethod.STORE_ONLY)
                .active(true)
                .createdByUserId(user.getId())
                .nextRunAt(Instant.now().plusSeconds(86400))
                .build();
        schedule.setTenantId(tenant.getId());
        schedule = reportScheduleRepository.saveAndFlush(schedule);

        execution = ReportExecution.builder()
                .reportDefinition(inventoryDef)
                .executedByUserId(user.getId())
                .source(ReportExecution.ExecutionSource.MANUAL)
                .status(ReportExecution.ExecutionStatus.COMPLETED)
                .exportFormat(ExportFormat.EXCEL)
                .startedAt(Instant.now().minusSeconds(60))
                .completedAt(Instant.now())
                .durationMs(1500L)
                .rowCount(25)
                .build();
        execution.setTenantId(tenant.getId());
        execution = reportExecutionRepository.saveAndFlush(execution);

        entityManager.clear();
    }

    // ===================== Auth helpers =====================

    private RequestPostProcessor fullAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("REPORT_VIEW"),
                        new SimpleGrantedAuthority("REPORT_INVENTORY_VIEW"),
                        new SimpleGrantedAuthority("REPORT_SALES_VIEW"),
                        new SimpleGrantedAuthority("REPORT_FINANCIAL_VIEW"),
                        new SimpleGrantedAuthority("REPORT_SCHEDULE_VIEW"),
                        new SimpleGrantedAuthority("REPORT_SCHEDULE_MANAGE"),
                        new SimpleGrantedAuthority("HR_SALARY_READ")));
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

    // ===================== Report Definitions =====================

    @Test
    void listDefinitions_returnsPaginated() throws Exception {
        mockMvc.perform(get(BASE_URL + "/definitions").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page.totalElements").value(2));
    }

    @Test
    void getDefinition_byId_returnsDefinition() throws Exception {
        mockMvc.perform(get(BASE_URL + "/definitions/{id}", inventoryDef.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(inventoryDef.getId().intValue()))
                .andExpect(jsonPath("$.data.code").value("STOCK_ON_HAND"))
                .andExpect(jsonPath("$.data.name").value("Stock On Hand Report"))
                .andExpect(jsonPath("$.data.category").value("INVENTORY"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void getDefinition_byCode_returnsDefinition() throws Exception {
        mockMvc.perform(get(BASE_URL + "/definitions/code/{code}", "TRIAL_BALANCE").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("TRIAL_BALANCE"))
                .andExpect(jsonPath("$.data.name").value("Trial Balance Report"))
                .andExpect(jsonPath("$.data.category").value("FINANCIAL"));
    }

    @Test
    void listByCategory_returnsFiltered() throws Exception {
        mockMvc.perform(get(BASE_URL + "/definitions/category/INVENTORY").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].code").value("STOCK_ON_HAND"))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    // ===================== Inventory Reports =====================

    @Test
    void generateStockOnHand_returnsReport() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();

        mockMvc.perform(post(BASE_URL + "/inventory/stock-on-hand")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metadata").exists())
                .andExpect(jsonPath("$.data.summary").exists());
    }

    @Test
    void exportStockOnHand_returnsBinaryData() throws Exception {
        GenerateReportRequest request = GenerateReportRequest.builder()
                .exportFormat(ExportFormat.EXCEL)
                .build();

        mockMvc.perform(post(BASE_URL + "/inventory/stock-on-hand/export")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(result -> {
                    byte[] body = result.getResponse().getContentAsByteArray();
                    assert body.length > 0 : "Export response body should not be empty";
                });
    }

    @Test
    void generateInventoryValuation_returnsReport() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();

        mockMvc.perform(post(BASE_URL + "/inventory/valuation")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metadata").exists())
                .andExpect(jsonPath("$.data.summary").exists());
    }

    // ===================== Sales Reports =====================

    @Test
    void generateSalesSummary_returnsReport() throws Exception {
        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build();

        mockMvc.perform(post(BASE_URL + "/sales/summary")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metadata").exists())
                .andExpect(jsonPath("$.data.summary").exists());
    }

    @Test
    void exportSalesSummary_returnsBinaryData() throws Exception {
        GenerateReportRequest request = GenerateReportRequest.builder()
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .exportFormat(ExportFormat.EXCEL)
                .build();

        mockMvc.perform(post(BASE_URL + "/sales/summary/export")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Disposition"))
                .andExpect(result -> {
                    byte[] body = result.getResponse().getContentAsByteArray();
                    assert body.length > 0 : "Export response body should not be empty";
                });
    }

    // ===================== Financial Reports =====================

    @Test
    void generateTrialBalance_returnsReport() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();

        mockMvc.perform(post(BASE_URL + "/financial/trial-balance")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metadata").exists())
                .andExpect(jsonPath("$.data.accounts").exists())
                .andExpect(jsonPath("$.data.totals").exists());
    }

    @Test
    void generateIncomeStatement_returnsReport() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();

        mockMvc.perform(post(BASE_URL + "/financial/income-statement")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metadata").exists());
    }

    @Test
    void generateARAgingReport_returnsReport() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();

        mockMvc.perform(post(BASE_URL + "/financial/ar-aging")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metadata").exists());
    }

    @Test
    void generateAPAgingReport_returnsReport() throws Exception {
        GenerateReportRequest request = new GenerateReportRequest();

        mockMvc.perform(post(BASE_URL + "/financial/ap-aging")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metadata").exists());
    }

    // ===================== HR Reports =====================

    @Test
    void generateSalaryReport_returnsReport() throws Exception {
        mockMvc.perform(get(BASE_URL + "/hr/salary")
                        .param("year", "2026")
                        .param("month", "4")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.metadata").exists());
    }

    // ===================== Schedules =====================

    @Test
    void listSchedules_returnsPaginated() throws Exception {
        mockMvc.perform(get(BASE_URL + "/schedules").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    void getSchedule_byId_returnsSchedule() throws Exception {
        mockMvc.perform(get(BASE_URL + "/schedules/{id}", schedule.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(schedule.getId().intValue()))
                .andExpect(jsonPath("$.data.name").value("Daily Stock Report"))
                .andExpect(jsonPath("$.data.frequency").value("DAILY"))
                .andExpect(jsonPath("$.data.exportFormat").value("EXCEL"))
                .andExpect(jsonPath("$.data.active").value(true));
    }

    @Test
    void createSchedule_validRequest_returnsCreated() throws Exception {
        ReportScheduleDTO dto = ReportScheduleDTO.builder()
                .reportDefinitionId(inventoryDef.getId())
                .name("Weekly Inventory Report")
                .frequency(ReportSchedule.ScheduleFrequency.DAILY)
                .exportFormat(ExportFormat.EXCEL)
                .deliveryMethod(ReportSchedule.DeliveryMethod.STORE_ONLY)
                .build();

        mockMvc.perform(post(BASE_URL + "/schedules")
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report schedule created successfully"))
                .andExpect(jsonPath("$.data.name").value("Weekly Inventory Report"))
                .andExpect(jsonPath("$.data.frequency").value("DAILY"))
                .andExpect(jsonPath("$.data.exportFormat").value("EXCEL"))
                .andExpect(jsonPath("$.data.id").isNumber());
    }

    @Test
    void updateSchedule_validRequest_returnsUpdated() throws Exception {
        ReportScheduleDTO dto = ReportScheduleDTO.builder()
                .name("Updated Schedule Name")
                .frequency(ReportSchedule.ScheduleFrequency.DAILY)
                .exportFormat(ExportFormat.EXCEL)
                .deliveryMethod(ReportSchedule.DeliveryMethod.STORE_ONLY)
                .build();

        mockMvc.perform(put(BASE_URL + "/schedules/{id}", schedule.getId())
                        .with(fullAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report schedule updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Schedule Name"));
    }

    @Test
    void deleteSchedule_returnsSuccess() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/schedules/{id}", schedule.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report schedule deleted successfully"));
    }

    @Test
    void toggleSchedule_disables() throws Exception {
        mockMvc.perform(put(BASE_URL + "/schedules/{id}/toggle", schedule.getId())
                        .param("active", "false")
                        .with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.active").value(false));
    }

    // ===================== Executions =====================

    @Test
    void listExecutions_returnsPaginated() throws Exception {
        mockMvc.perform(get(BASE_URL + "/executions").with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.page.totalElements").value(1));
    }

    @Test
    void getExecution_byId_returnsExecution() throws Exception {
        mockMvc.perform(get(BASE_URL + "/executions/{id}", execution.getId()).with(fullAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(execution.getId().intValue()))
                .andExpect(jsonPath("$.data.source").value("MANUAL"))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.exportFormat").value("EXCEL"));
    }

    // ===================== Permission checks =====================

    @Test
    void definitions_forbidden_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/definitions").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }

    @Test
    void schedules_forbidden_returns403() throws Exception {
        ReportScheduleDTO dto = ReportScheduleDTO.builder()
                .reportDefinitionId(inventoryDef.getId())
                .name("Forbidden Schedule")
                .frequency(ReportSchedule.ScheduleFrequency.DAILY)
                .exportFormat(ExportFormat.EXCEL)
                .build();

        mockMvc.perform(post(BASE_URL + "/schedules")
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }
}
