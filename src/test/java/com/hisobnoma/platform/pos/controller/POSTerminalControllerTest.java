package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.pos.dto.CreateTerminalRequest;
import com.hisobnoma.platform.pos.dto.POSTerminalDto;
import com.hisobnoma.platform.pos.service.POSTerminalService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class POSTerminalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private POSTerminalService terminalService;

    // ==================== GET /api/v1/pos/terminals ====================

    @Test
    @WithMockUser(authorities = "POS_TERMINAL_READ")
    void getAll_authenticated_returns200() throws Exception {
        POSTerminalDto dto = POSTerminalDto.builder().id(1L).name("Terminal-1").terminalCode("T-001").build();
        when(terminalService.findAll(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/pos/terminals"))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAll_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/terminals/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_TERMINAL_READ")
    void getById_authenticated_returns200() throws Exception {
        POSTerminalDto dto = POSTerminalDto.builder().id(1L).name("Terminal-1").terminalCode("T-001").build();
        when(terminalService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/terminals/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.terminalCode").value("T-001"));
    }

    @Test
    void getById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/terminals/code/{code} ====================

    @Test
    @WithMockUser(authorities = "POS_TERMINAL_READ")
    void getByCode_authenticated_returns200() throws Exception {
        POSTerminalDto dto = POSTerminalDto.builder().id(1L).name("Terminal-1").terminalCode("T-001").build();
        when(terminalService.findByCode("T-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/terminals/code/T-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.terminalCode").value("T-001"));
    }

    @Test
    void getByCode_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals/code/T-001"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/terminals/location/{locationId} ====================

    @Test
    @WithMockUser(authorities = "POS_TERMINAL_READ")
    void getByLocation_authenticated_returns200() throws Exception {
        when(terminalService.findByLocation(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/terminals/location/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getByLocation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals/location/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/terminals/active ====================

    @Test
    @WithMockUser(authorities = "POS_TERMINAL_READ")
    void getActiveTerminals_authenticated_returns200() throws Exception {
        when(terminalService.findActiveTerminals()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/terminals/active"))
                .andExpect(status().isOk());
    }

    @Test
    void getActiveTerminals_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/terminals ====================

    @Test
    @WithMockUser(authorities = "POS_TERMINAL_CREATE")
    void create_authenticated_returns201() throws Exception {
        CreateTerminalRequest request = new CreateTerminalRequest();
        POSTerminalDto dto = POSTerminalDto.builder().id(1L).name("Terminal-1").terminalCode("T-001").build();
        when(terminalService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/terminals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.terminalCode").value("T-001"));
    }

    @Test
    void create_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/terminals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void create_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/terminals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/pos/terminals/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_TERMINAL_UPDATE")
    void update_authenticated_returns200() throws Exception {
        CreateTerminalRequest request = new CreateTerminalRequest();
        POSTerminalDto dto = POSTerminalDto.builder().id(1L).name("Updated").terminalCode("T-001").build();
        when(terminalService.update(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/pos/terminals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Updated"));
    }

    @Test
    void update_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/terminals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void update_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/terminals/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/pos/terminals/{id}/activate ====================

    @Test
    @WithMockUser(authorities = "POS_TERMINAL_UPDATE")
    void activate_authenticated_returns200() throws Exception {
        doNothing().when(terminalService).activate(1L);

        mockMvc.perform(put("/api/v1/pos/terminals/1/activate"))
                .andExpect(status().isOk());
    }

    @Test
    void activate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/terminals/1/activate"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/pos/terminals/{id}/deactivate ====================

    @Test
    @WithMockUser(authorities = "POS_TERMINAL_UPDATE")
    void deactivate_authenticated_returns200() throws Exception {
        doNothing().when(terminalService).deactivate(1L);

        mockMvc.perform(put("/api/v1/pos/terminals/1/deactivate"))
                .andExpect(status().isOk());
    }

    @Test
    void deactivate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/terminals/1/deactivate"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/pos/terminals/{id} ====================

    @Test
    @WithMockUser(authorities = "POS_TERMINAL_DELETE")
    void delete_authenticated_returns200() throws Exception {
        doNothing().when(terminalService).delete(1L);

        mockMvc.perform(delete("/api/v1/pos/terminals/1"))
                .andExpect(status().isOk());
    }

    @Test
    void delete_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/terminals/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void delete_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/terminals/1"))
                .andExpect(status().isForbidden());
    }
}
