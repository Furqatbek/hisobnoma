package com.hisobnoma.platform.finance.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.finance.dto.CreateFiscalYearRequest;
import com.hisobnoma.platform.finance.dto.FiscalPeriodDto;
import com.hisobnoma.platform.finance.dto.FiscalYearDto;
import com.hisobnoma.platform.finance.entity.PeriodStatus;
import com.hisobnoma.platform.finance.service.FiscalPeriodService;
import com.hisobnoma.platform.common.dto.PageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FiscalPeriodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private FiscalPeriodService fiscalPeriodService;

    // ==================== GET /api/v1/finance/fiscal/years ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_VIEW")
    void getFiscalYears_authenticated_returns200() throws Exception {
        // Given
        FiscalYearDto dto = FiscalYearDto.builder()
                .id(1L)
                .year(2026)
                .name("FY 2026")
                .status(PeriodStatus.OPEN)
                .current(true)
                .build();

        PageResponse<FiscalYearDto> pageResponse = PageResponse.of(List.of(dto), 0, 20, 1);
        when(fiscalPeriodService.getFiscalYears(any())).thenReturn(pageResponse);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/fiscal/years"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].year").value(2026));
    }

    @Test
    void getFiscalYears_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getFiscalYears_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/fiscal/years/all ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_VIEW")
    void getAllFiscalYears_returns200() throws Exception {
        // Given
        FiscalYearDto dto = FiscalYearDto.builder()
                .id(1L).year(2026).name("FY 2026").build();
        when(fiscalPeriodService.getAllFiscalYears()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/fiscal/years/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].year").value(2026));
    }

    // ==================== GET /api/v1/finance/fiscal/years/current ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_VIEW")
    void getCurrentFiscalYear_returns200() throws Exception {
        // Given
        FiscalYearDto dto = FiscalYearDto.builder()
                .id(1L).year(2026).name("FY 2026").current(true).build();
        when(fiscalPeriodService.getCurrentFiscalYear()).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/fiscal/years/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(true));
    }

    // ==================== GET /api/v1/finance/fiscal/years/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_VIEW")
    void getFiscalYear_found_returns200() throws Exception {
        // Given
        FiscalYearDto dto = FiscalYearDto.builder()
                .id(1L)
                .year(2026)
                .name("FY 2026")
                .status(PeriodStatus.OPEN)
                .build();
        when(fiscalPeriodService.getFiscalYear(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/fiscal/years/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("FY 2026"));
    }

    @Test
    void getFiscalYear_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getFiscalYear_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/finance/fiscal/years/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/fiscal/years/{id}/details ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_VIEW")
    void getFiscalYearWithPeriods_returns200() throws Exception {
        // Given
        FiscalYearDto dto = FiscalYearDto.builder()
                .id(1L).year(2026).name("FY 2026")
                .periods(List.of(FiscalPeriodDto.builder()
                        .id(1L).periodNumber(1).name("January 2026").build()))
                .build();
        when(fiscalPeriodService.getFiscalYearWithPeriods(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/fiscal/years/1/details"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periods[0].name").value("January 2026"));
    }

    // ==================== GET /api/v1/finance/fiscal/years/by-date ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_VIEW")
    void getFiscalYearByDate_returns200() throws Exception {
        // Given
        FiscalYearDto dto = FiscalYearDto.builder()
                .id(1L).year(2026).name("FY 2026").build();
        when(fiscalPeriodService.getFiscalYearByDate(any(LocalDate.class))).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/fiscal/years/by-date")
                        .param("date", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.year").value(2026));
    }

    // ==================== POST /api/v1/finance/fiscal/years ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_MANAGE")
    void createFiscalYear_valid_returns201() throws Exception {
        // Given
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2027)
                .name("FY 2027")
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2027, 12, 31))
                .build();

        FiscalYearDto dto = FiscalYearDto.builder()
                .id(2L)
                .year(2027)
                .name("FY 2027")
                .status(PeriodStatus.OPEN)
                .build();
        when(fiscalPeriodService.createFiscalYear(any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/fiscal/years")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.year").value(2027));
    }

    @Test
    void createFiscalYear_unauthenticated_returns403() throws Exception {
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2027).name("FY 2027")
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2027, 12, 31)).build();

        mockMvc.perform(post("/api/v1/finance/fiscal/years")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void createFiscalYear_noPermission_returns403() throws Exception {
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2027).name("FY 2027")
                .startDate(LocalDate.of(2027, 1, 1))
                .endDate(LocalDate.of(2027, 12, 31)).build();

        mockMvc.perform(post("/api/v1/finance/fiscal/years")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/finance/fiscal/years/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_MANAGE")
    void updateFiscalYear_valid_returns200() throws Exception {
        // Given
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2026)
                .name("FY 2026 Updated")
                .startDate(LocalDate.of(2026, 1, 1))
                .endDate(LocalDate.of(2026, 12, 31))
                .build();

        FiscalYearDto dto = FiscalYearDto.builder()
                .id(1L)
                .year(2026)
                .name("FY 2026 Updated")
                .build();
        when(fiscalPeriodService.updateFiscalYear(any(), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/fiscal/years/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("FY 2026 Updated"));
    }

    // ==================== PUT /api/v1/finance/fiscal/years/{id}/set-current ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_MANAGE")
    void setCurrentFiscalYear_returns200() throws Exception {
        // Given
        FiscalYearDto dto = FiscalYearDto.builder()
                .id(1L).year(2026).current(true).build();
        when(fiscalPeriodService.setCurrentFiscalYear(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(put("/api/v1/finance/fiscal/years/1/set-current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.current").value(true));
    }

    // ==================== POST /api/v1/finance/fiscal/years/{id}/close ====================

    @Test
    @WithMockUser(authorities = "FINANCE_YEAR_CLOSE")
    void closeFiscalYear_returns200() throws Exception {
        // Given
        FiscalYearDto dto = FiscalYearDto.builder()
                .id(1L).year(2025).closed(true).status(PeriodStatus.CLOSED).build();
        when(fiscalPeriodService.closeFiscalYear(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/fiscal/years/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.closed").value(true));
    }

    @Test
    void closeFiscalYear_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/finance/fiscal/years/1/close"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void closeFiscalYear_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/finance/fiscal/years/1/close"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/finance/fiscal/years/{yearId}/periods ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_VIEW")
    void getPeriodsByFiscalYear_returns200() throws Exception {
        // Given
        FiscalPeriodDto dto = FiscalPeriodDto.builder()
                .id(1L).periodNumber(1).name("January 2026")
                .status(PeriodStatus.OPEN).build();
        when(fiscalPeriodService.getPeriodsByFiscalYear(1L)).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/fiscal/years/1/periods"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("January 2026"));
    }

    // ==================== GET /api/v1/finance/fiscal/periods/{id} ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_VIEW")
    void getPeriod_found_returns200() throws Exception {
        // Given
        FiscalPeriodDto dto = FiscalPeriodDto.builder()
                .id(1L).periodNumber(1).name("January 2026")
                .status(PeriodStatus.OPEN).build();
        when(fiscalPeriodService.getPeriod(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/fiscal/periods/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodNumber").value(1));
    }

    // ==================== GET /api/v1/finance/fiscal/periods/by-date ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_VIEW")
    void getPeriodByDate_returns200() throws Exception {
        // Given
        FiscalPeriodDto dto = FiscalPeriodDto.builder()
                .id(1L).periodNumber(6).name("June 2026").build();
        when(fiscalPeriodService.getPeriodByDate(any(LocalDate.class))).thenReturn(dto);

        // When/Then
        mockMvc.perform(get("/api/v1/finance/fiscal/periods/by-date")
                        .param("date", "2026-06-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodNumber").value(6));
    }

    // ==================== GET /api/v1/finance/fiscal/periods/open ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_VIEW")
    void getOpenPeriods_returns200() throws Exception {
        // Given
        FiscalPeriodDto dto = FiscalPeriodDto.builder()
                .id(1L).periodNumber(4).name("April 2026")
                .status(PeriodStatus.OPEN).build();
        when(fiscalPeriodService.getOpenPeriods()).thenReturn(List.of(dto));

        // When/Then
        mockMvc.perform(get("/api/v1/finance/fiscal/periods/open"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    // ==================== POST /api/v1/finance/fiscal/periods/{id}/close ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_CLOSE")
    void closePeriod_returns200() throws Exception {
        // Given
        FiscalPeriodDto dto = FiscalPeriodDto.builder()
                .id(1L).periodNumber(1).status(PeriodStatus.CLOSED).build();
        when(fiscalPeriodService.closePeriod(1L)).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/fiscal/periods/1/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closePeriod_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/finance/fiscal/periods/1/close"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void closePeriod_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/finance/fiscal/periods/1/close"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/finance/fiscal/periods/{id}/reopen ====================

    @Test
    @WithMockUser(authorities = "FINANCE_PERIOD_REOPEN")
    void reopenPeriod_returns200() throws Exception {
        // Given
        FiscalPeriodDto dto = FiscalPeriodDto.builder()
                .id(1L).periodNumber(1).status(PeriodStatus.OPEN)
                .reopenReason("Correction needed").build();
        when(fiscalPeriodService.reopenPeriod(eq(1L), any())).thenReturn(dto);

        // When/Then
        mockMvc.perform(post("/api/v1/finance/fiscal/periods/1/reopen")
                        .param("reason", "Correction needed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void reopenPeriod_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/finance/fiscal/periods/1/reopen")
                        .param("reason", "Correction needed"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void reopenPeriod_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/finance/fiscal/periods/1/reopen")
                        .param("reason", "Correction needed"))
                .andExpect(status().isForbidden());
    }
}
