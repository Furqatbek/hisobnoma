package com.hisobnoma.platform.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.hr.dto.CreateSalaryAdvanceRequest;
import com.hisobnoma.platform.hr.dto.SalaryAdvanceDto;
import com.hisobnoma.platform.hr.service.SalaryAdvanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
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
class SalaryAdvanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SalaryAdvanceService advanceService;

    @Test
    @WithMockUser(authorities = "HR_SALARY_READ")
    void getByPeriod_withReadPermission_returns200() throws Exception {
        SalaryAdvanceDto dto = SalaryAdvanceDto.builder()
                .id(1L)
                .employeeId(1L)
                .amount(new BigDecimal("1000000"))
                .periodYear(2024)
                .periodMonth(1)
                .status("GIVEN")
                .build();
        when(advanceService.getByPeriod(2024, 1)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/hr/advances/period")
                        .param("year", "2024")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("GIVEN"));
    }

    @Test
    void getByPeriod_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/advances/period")
                        .param("year", "2024")
                        .param("month", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getByPeriod_wrongPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/advances/period")
                        .param("year", "2024")
                        .param("month", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_READ")
    void getByEmployee_withReadPermission_returns200() throws Exception {
        SalaryAdvanceDto dto = SalaryAdvanceDto.builder()
                .id(1L)
                .employeeId(1L)
                .amount(new BigDecimal("1000000"))
                .status("GIVEN")
                .build();
        when(advanceService.getByEmployee(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/hr/advances/employee/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(1));
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_READ")
    void getUndeductedTotal_withReadPermission_returns200() throws Exception {
        when(advanceService.getUndeductedTotal(1L, 2024, 1)).thenReturn(new BigDecimal("2000000"));

        mockMvc.perform(get("/api/v1/hr/advances/employee/1/total")
                        .param("year", "2024")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(2000000));
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_WRITE")
    void create_withWritePermission_returns201() throws Exception {
        CreateSalaryAdvanceRequest request = new CreateSalaryAdvanceRequest();
        request.setEmployeeId(1L);
        request.setAmount(new BigDecimal("1000000"));
        request.setPeriodYear(2024);
        request.setPeriodMonth(1);
        request.setAdvanceDate(LocalDate.of(2024, 1, 10));

        SalaryAdvanceDto response = SalaryAdvanceDto.builder()
                .id(1L)
                .employeeId(1L)
                .amount(new BigDecimal("1000000"))
                .status("GIVEN")
                .build();
        when(advanceService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/hr/advances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_READ")
    void create_withReadOnlyPermission_returns403() throws Exception {
        CreateSalaryAdvanceRequest request = new CreateSalaryAdvanceRequest();
        request.setEmployeeId(1L);
        request.setAmount(new BigDecimal("1000000"));
        request.setPeriodYear(2024);
        request.setPeriodMonth(1);

        mockMvc.perform(post("/api/v1/hr/advances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_WRITE")
    void cancel_withWritePermission_returns200() throws Exception {
        SalaryAdvanceDto response = SalaryAdvanceDto.builder()
                .id(1L)
                .status("CANCELLED")
                .build();
        when(advanceService.cancel(1L)).thenReturn(response);

        mockMvc.perform(put("/api/v1/hr/advances/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
