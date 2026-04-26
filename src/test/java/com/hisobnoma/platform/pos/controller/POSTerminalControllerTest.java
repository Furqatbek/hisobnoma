package com.hisobnoma.platform.pos.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
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

    private RequestPostProcessor userWithPermission(String... permissions) {
        UserPrincipal principal = new UserPrincipal(
                1L, "admin", "pw", 1L, true, true,
                Arrays.stream(permissions)
                        .map(SimpleGrantedAuthority::new)
                        .toList());
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ==================== GET /api/v1/pos/terminals ====================

    @Test
    void getAll_authenticated_returns200() throws Exception {
        POSTerminalDto dto = POSTerminalDto.builder().id(1L).name("Terminal-1").terminalCode("T-001").build();
        when(terminalService.findAll(any())).thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/v1/pos/terminals").with(userWithPermission("POS_TERMINAL_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/terminals/{id} ====================

    @Test
    void getById_authenticated_returns200() throws Exception {
        POSTerminalDto dto = POSTerminalDto.builder().id(1L).name("Terminal-1").terminalCode("T-001").build();
        when(terminalService.findById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/terminals/1").with(userWithPermission("POS_TERMINAL_READ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.terminalCode").value("T-001"));
    }

    @Test
    void getById_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getById_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals/1").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/terminals/code/{code} ====================

    @Test
    void getByCode_authenticated_returns200() throws Exception {
        POSTerminalDto dto = POSTerminalDto.builder().id(1L).name("Terminal-1").terminalCode("T-001").build();
        when(terminalService.findByCode("T-001")).thenReturn(dto);

        mockMvc.perform(get("/api/v1/pos/terminals/code/T-001").with(userWithPermission("POS_TERMINAL_READ")))
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
    void getByLocation_authenticated_returns200() throws Exception {
        when(terminalService.findByLocation(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/terminals/location/1").with(userWithPermission("POS_TERMINAL_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getByLocation_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals/location/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/pos/terminals/active ====================

    @Test
    void getActiveTerminals_authenticated_returns200() throws Exception {
        when(terminalService.findActiveTerminals()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/pos/terminals/active").with(userWithPermission("POS_TERMINAL_READ")))
                .andExpect(status().isOk());
    }

    @Test
    void getActiveTerminals_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/pos/terminals/active"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/pos/terminals ====================

    @Test
    void create_authenticated_returns201() throws Exception {
        CreateTerminalRequest request = new CreateTerminalRequest();
        POSTerminalDto dto = POSTerminalDto.builder().id(1L).name("Terminal-1").terminalCode("T-001").build();
        when(terminalService.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/pos/terminals")
                        .with(userWithPermission("POS_TERMINAL_CREATE"))
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
    void create_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/pos/terminals")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/pos/terminals/{id} ====================

    @Test
    void update_authenticated_returns200() throws Exception {
        CreateTerminalRequest request = new CreateTerminalRequest();
        POSTerminalDto dto = POSTerminalDto.builder().id(1L).name("Updated").terminalCode("T-001").build();
        when(terminalService.update(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/pos/terminals/1")
                        .with(userWithPermission("POS_TERMINAL_UPDATE"))
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
    void update_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/terminals/1")
                        .with(userWithPermission("SOME_OTHER_PERMISSION"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/pos/terminals/{id}/activate ====================

    @Test
    void activate_authenticated_returns200() throws Exception {
        doNothing().when(terminalService).activate(1L);

        mockMvc.perform(put("/api/v1/pos/terminals/1/activate").with(userWithPermission("POS_TERMINAL_UPDATE")))
                .andExpect(status().isOk());
    }

    @Test
    void activate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/terminals/1/activate"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/pos/terminals/{id}/deactivate ====================

    @Test
    void deactivate_authenticated_returns200() throws Exception {
        doNothing().when(terminalService).deactivate(1L);

        mockMvc.perform(put("/api/v1/pos/terminals/1/deactivate").with(userWithPermission("POS_TERMINAL_UPDATE")))
                .andExpect(status().isOk());
    }

    @Test
    void deactivate_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/pos/terminals/1/deactivate"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/pos/terminals/{id} ====================

    @Test
    void delete_authenticated_returns200() throws Exception {
        doNothing().when(terminalService).delete(1L);

        mockMvc.perform(delete("/api/v1/pos/terminals/1").with(userWithPermission("POS_TERMINAL_DELETE")))
                .andExpect(status().isOk());
    }

    @Test
    void delete_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/terminals/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/pos/terminals/1").with(userWithPermission("SOME_OTHER_PERMISSION")))
                .andExpect(status().isForbidden());
    }
}
