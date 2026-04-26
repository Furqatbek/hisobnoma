package com.hisobnoma.platform.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.hr.dto.CreateEmployeeRequest;
import com.hisobnoma.platform.hr.dto.EmployeeDto;
import com.hisobnoma.platform.hr.service.EmployeeService;
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
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EmployeeService employeeService;

    @Test
    @WithMockUser(authorities = "HR_EMPLOYEE_READ")
    void getAll_withReadPermission_returns200() throws Exception {
        EmployeeDto dto = EmployeeDto.builder()
                .id(1L)
                .employeeCode("EMP-001")
                .fullName("Valiyev Ali")
                .active(true)
                .build();
        PageResponse<EmployeeDto> page = PageResponse.of(List.of(dto), 0, 10, 1);
        when(employeeService.getAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/hr/employees"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].employeeCode").value("EMP-001"));
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/employees"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAll_wrongPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/employees"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HR_EMPLOYEE_READ")
    void getActive_withReadPermission_returns200() throws Exception {
        EmployeeDto dto = EmployeeDto.builder()
                .id(1L)
                .employeeCode("EMP-001")
                .active(true)
                .build();
        when(employeeService.getActive()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/hr/employees/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employeeCode").value("EMP-001"));
    }

    @Test
    @WithMockUser(authorities = "HR_EMPLOYEE_READ")
    void getById_withReadPermission_returns200() throws Exception {
        EmployeeDto dto = EmployeeDto.builder()
                .id(1L)
                .employeeCode("EMP-001")
                .fullName("Valiyev Ali")
                .build();
        when(employeeService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/hr/employees/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Valiyev Ali"));
    }

    @Test
    @WithMockUser(authorities = "HR_EMPLOYEE_READ")
    void search_withReadPermission_returns200() throws Exception {
        EmployeeDto dto = EmployeeDto.builder()
                .id(1L)
                .employeeCode("EMP-001")
                .fullName("Valiyev Ali")
                .build();
        PageResponse<EmployeeDto> page = PageResponse.of(List.of(dto), 0, 10, 1);
        when(employeeService.search(eq("ali"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/hr/employees/search").param("q", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].fullName").value("Valiyev Ali"));
    }

    @Test
    @WithMockUser(authorities = "HR_EMPLOYEE_WRITE")
    void create_withWritePermission_returns201() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeCode("EMP-002");
        request.setFirstName("Ali");
        request.setLastName("Valiyev");
        request.setHireDate(LocalDate.of(2024, 1, 15));

        EmployeeDto response = EmployeeDto.builder()
                .id(2L)
                .employeeCode("EMP-002")
                .fullName("Valiyev Ali")
                .active(true)
                .build();
        when(employeeService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/hr/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    @WithMockUser(authorities = "HR_EMPLOYEE_READ")
    void create_withReadOnlyPermission_returns403() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeCode("EMP-002");
        request.setFirstName("Ali");
        request.setLastName("Valiyev");
        request.setHireDate(LocalDate.now());

        mockMvc.perform(post("/api/v1/hr/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HR_EMPLOYEE_WRITE")
    void update_withWritePermission_returns200() throws Exception {
        CreateEmployeeRequest request = new CreateEmployeeRequest();
        request.setEmployeeCode("EMP-001");
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setHireDate(LocalDate.now());

        EmployeeDto response = EmployeeDto.builder()
                .id(1L)
                .fullName("Name Updated")
                .build();
        when(employeeService.update(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/hr/employees/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "HR_EMPLOYEE_WRITE")
    void terminate_withWritePermission_returns200() throws Exception {
        EmployeeDto response = EmployeeDto.builder()
                .id(1L)
                .status("TERMINATED")
                .active(false)
                .build();
        when(employeeService.terminate(1L)).thenReturn(response);

        mockMvc.perform(put("/api/v1/hr/employees/1/terminate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINATED"));
    }

    @Test
    @WithMockUser(authorities = "HR_EMPLOYEE_WRITE")
    void delete_withWritePermission_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/hr/employees/1"))
                .andExpect(status().isNoContent());
    }
}
