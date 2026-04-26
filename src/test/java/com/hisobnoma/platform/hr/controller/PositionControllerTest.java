package com.hisobnoma.platform.hr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.hr.dto.PositionDto;
import com.hisobnoma.platform.hr.service.PositionService;
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
class PositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PositionService positionService;

    @Test
    @WithMockUser(authorities = "HR_POSITION_READ")
    void getAll_withReadPermission_returns200() throws Exception {
        PositionDto dto = PositionDto.builder()
                .id(1L)
                .code("DEV")
                .name("Developer")
                .baseSalary(new BigDecimal("5000000"))
                .active(true)
                .build();
        when(positionService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v1/hr/positions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Developer"));
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/positions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAll_wrongPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/hr/positions"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HR_POSITION_READ")
    void getById_withReadPermission_returns200() throws Exception {
        PositionDto dto = PositionDto.builder()
                .id(1L)
                .code("DEV")
                .name("Developer")
                .build();
        when(positionService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/hr/positions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Developer"));
    }

    @Test
    @WithMockUser(authorities = "HR_POSITION_WRITE")
    void create_withWritePermission_returns201() throws Exception {
        PositionDto request = PositionDto.builder()
                .code("DEV")
                .name("Developer")
                .build();
        PositionDto response = PositionDto.builder()
                .id(1L)
                .code("DEV")
                .name("Developer")
                .active(true)
                .build();
        when(positionService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/hr/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(authorities = "HR_POSITION_READ")
    void create_withReadOnlyPermission_returns403() throws Exception {
        PositionDto request = PositionDto.builder()
                .code("DEV")
                .name("Developer")
                .build();

        mockMvc.perform(post("/api/v1/hr/positions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "HR_POSITION_WRITE")
    void update_withWritePermission_returns200() throws Exception {
        PositionDto request = PositionDto.builder()
                .name("Senior Developer")
                .build();
        PositionDto response = PositionDto.builder()
                .id(1L)
                .code("DEV")
                .name("Senior Developer")
                .active(true)
                .build();
        when(positionService.update(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/hr/positions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Senior Developer"));
    }

    @Test
    @WithMockUser(authorities = "HR_POSITION_WRITE")
    void delete_withWritePermission_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/hr/positions/1"))
                .andExpect(status().isNoContent());
    }
}
