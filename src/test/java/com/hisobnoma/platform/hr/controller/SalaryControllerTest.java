package com.hisobnoma.platform.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.hr.dto.CreateSalaryRecordRequest;
import com.hisobnoma.platform.hr.dto.SalaryRecordDto;
import com.hisobnoma.platform.hr.service.SalaryService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalaryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SalaryService salaryService;

    @Test
    @WithMockUser(authorities = "HR_SALARY_READ")
    void getAll_withReadPermission_returns200() throws Exception {
        SalaryRecordDto dto = SalaryRecordDto.builder()
                .id(1L)
                .employeeId(1L)
                .periodYear(2024)
                .periodMonth(1)
                .baseAmount(new BigDecimal("5000000"))
                .netAmount(new BigDecimal("5300000"))
                .status("PENDING")
                .build();
        PageResponse<SalaryRecordDto> page = PageResponse.of(List.of(dto), 0, 10, 1);
        when(salaryService.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/hr/salary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/salary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAll_wrongPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/salary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_READ")
    void getByPeriod_withReadPermission_returns200() throws Exception {
        SalaryRecordDto dto = SalaryRecordDto.builder()
                .id(1L)
                .periodYear(2024)
                .periodMonth(1)
                .status("PENDING")
                .build();
        PageResponse<SalaryRecordDto> page = PageResponse.of(List.of(dto), 0, 10, 1);
        when(salaryService.getByPeriod(eq(2024), eq(1), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/hr/salary/period")
                        .param("year", "2024")
                        .param("month", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].periodYear").value(2024));
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_READ")
    void getByEmployee_withReadPermission_returns200() throws Exception {
        SalaryRecordDto dto = SalaryRecordDto.builder()
                .id(1L)
                .employeeId(1L)
                .status("PENDING")
                .build();
        when(salaryService.getByEmployee(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/hr/salary/employee/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeId").value(1));
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_READ")
    void getById_withReadPermission_returns200() throws Exception {
        SalaryRecordDto dto = SalaryRecordDto.builder()
                .id(1L)
                .employeeId(1L)
                .status("PENDING")
                .build();
        when(salaryService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/hr/salary/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_WRITE")
    void create_withWritePermission_returns201() throws Exception {
        CreateSalaryRecordRequest request = new CreateSalaryRecordRequest();
        request.setEmployeeId(1L);
        request.setPeriodYear(2024);
        request.setPeriodMonth(1);
        request.setBaseAmount(new BigDecimal("5000000"));

        SalaryRecordDto response = SalaryRecordDto.builder()
                .id(1L)
                .employeeId(1L)
                .periodYear(2024)
                .periodMonth(1)
                .status("PENDING")
                .build();
        when(salaryService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/hr/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_READ")
    void create_withReadOnlyPermission_returns403() throws Exception {
        CreateSalaryRecordRequest request = new CreateSalaryRecordRequest();
        request.setEmployeeId(1L);
        request.setPeriodYear(2024);
        request.setPeriodMonth(1);
        request.setBaseAmount(new BigDecimal("5000000"));

        mockMvc.perform(post("/api/v1/hr/salary")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_WRITE")
    void markPaid_withWritePermission_returns200() throws Exception {
        SalaryRecordDto response = SalaryRecordDto.builder()
                .id(1L)
                .status("PAID")
                .build();
        when(salaryService.markPaid(1L)).thenReturn(response);

        mockMvc.perform(put("/api/v1/hr/salary/1/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));
    }

    @Test
    @WithMockUser(authorities = "HR_SALARY_WRITE")
    void cancel_withWritePermission_returns200() throws Exception {
        SalaryRecordDto response = SalaryRecordDto.builder()
                .id(1L)
                .status("CANCELLED")
                .build();
        when(salaryService.cancel(1L)).thenReturn(response);

        mockMvc.perform(put("/api/v1/hr/salary/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
