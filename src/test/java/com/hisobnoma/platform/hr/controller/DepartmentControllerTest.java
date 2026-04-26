package com.hisobnoma.platform.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.hr.dto.DepartmentDto;
import com.hisobnoma.platform.hr.service.DepartmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DepartmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DepartmentService departmentService;

    @Test
    @WithMockUser(authorities = "HR_DEPARTMENT_READ")
    void getAll_withReadPermission_returns200() throws Exception {
        DepartmentDto dto = DepartmentDto.builder()
                .id(1L)
                .code("IT")
                .name("IT Department")
                .active(true)
                .build();
        when(departmentService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/hr/departments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("IT Department"));
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/departments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAll_wrongPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/departments"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HR_DEPARTMENT_READ")
    void getById_withReadPermission_returns200() throws Exception {
        DepartmentDto dto = DepartmentDto.builder()
                .id(1L)
                .code("IT")
                .name("IT Department")
                .active(true)
                .build();
        when(departmentService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/hr/departments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("IT Department"));
    }

    @Test
    @WithMockUser(authorities = "HR_DEPARTMENT_WRITE")
    void create_withWritePermission_returns201() throws Exception {
        DepartmentDto request = DepartmentDto.builder()
                .code("IT")
                .name("IT Department")
                .build();
        DepartmentDto response = DepartmentDto.builder()
                .id(1L)
                .code("IT")
                .name("IT Department")
                .active(true)
                .build();
        when(departmentService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/hr/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "HR_DEPARTMENT_READ")
    void create_withReadOnlyPermission_returns403() throws Exception {
        DepartmentDto request = DepartmentDto.builder()
                .code("IT")
                .name("IT Department")
                .build();

        mockMvc.perform(post("/api/v1/hr/departments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HR_DEPARTMENT_WRITE")
    void update_withWritePermission_returns200() throws Exception {
        DepartmentDto request = DepartmentDto.builder()
                .name("Updated IT")
                .build();
        DepartmentDto response = DepartmentDto.builder()
                .id(1L)
                .code("IT")
                .name("Updated IT")
                .active(true)
                .build();
        when(departmentService.update(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/hr/departments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated IT"));
    }

    @Test
    @WithMockUser(authorities = "HR_DEPARTMENT_WRITE")
    void delete_withWritePermission_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/hr/departments/1"))
                .andExpect(status().isNoContent());
    }
}
