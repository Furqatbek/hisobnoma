package com.hisobnoma.platform.mobile.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.mobile.dto.AlertPreferenceDTO;
import com.hisobnoma.platform.mobile.dto.MobileAlertDTO;
import com.hisobnoma.platform.mobile.service.MobileAlertService;
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
class MobileAlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MobileAlertService alertService;

    // ==================== GET /api/v1/mobile/alerts ====================

    @Test
    @WithMockUser(authorities = "MOBILE_ALERTS_VIEW")
    void getAlerts_authenticated_returns200() throws Exception {
        when(alertService.getAlerts(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/mobile/alerts"))
                .andExpect(status().isOk());
    }

    @Test
    void getAlerts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAlerts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/alerts/unread ====================

    @Test
    @WithMockUser(authorities = "MOBILE_ALERTS_VIEW")
    void getUnreadAlerts_authenticated_returns200() throws Exception {
        when(alertService.getUnreadAlerts(any())).thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/mobile/alerts/unread"))
                .andExpect(status().isOk());
    }

    @Test
    void getUnreadAlerts_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts/unread"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getUnreadAlerts_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts/unread"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/alerts/count ====================

    @Test
    @WithMockUser(authorities = "MOBILE_ALERTS_VIEW")
    void getUnreadCount_authenticated_returns200() throws Exception {
        when(alertService.getUnreadCount()).thenReturn(5L);

        mockMvc.perform(get("/api/v1/mobile/alerts/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    void getUnreadCount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts/count"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getUnreadCount_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts/count"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/mobile/alerts/{id}/read ====================

    @Test
    @WithMockUser(authorities = "MOBILE_ALERTS_VIEW")
    void markAsRead_authenticated_returns200() throws Exception {
        doNothing().when(alertService).markAsRead(1L);

        mockMvc.perform(put("/api/v1/mobile/alerts/1/read"))
                .andExpect(status().isOk());
    }

    @Test
    void markAsRead_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/mobile/alerts/1/read"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void markAsRead_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/mobile/alerts/1/read"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/mobile/alerts/read-all ====================

    @Test
    @WithMockUser(authorities = "MOBILE_ALERTS_VIEW")
    void markAllAsRead_authenticated_returns200() throws Exception {
        doNothing().when(alertService).markAllAsRead();

        mockMvc.perform(put("/api/v1/mobile/alerts/read-all"))
                .andExpect(status().isOk());
    }

    @Test
    void markAllAsRead_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/mobile/alerts/read-all"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/mobile/alerts/settings ====================

    @Test
    @WithMockUser(authorities = "MOBILE_ALERTS_VIEW")
    void getAlertSettings_authenticated_returns200() throws Exception {
        when(alertService.getAlertPreferences()).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/mobile/alerts/settings"))
                .andExpect(status().isOk());
    }

    @Test
    void getAlertSettings_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts/settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getAlertSettings_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/mobile/alerts/settings"))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/v1/mobile/alerts/settings/{alertType} ====================

    @Test
    @WithMockUser(authorities = "MOBILE_ALERTS_MANAGE")
    void updateAlertSettings_authenticated_returns200() throws Exception {
        AlertPreferenceDTO dto = new AlertPreferenceDTO();
        when(alertService.updateAlertPreference(any(), any())).thenReturn(dto);

        mockMvc.perform(put("/api/v1/mobile/alerts/settings/LOW_STOCK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk());
    }

    @Test
    void updateAlertSettings_unauthenticated_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/mobile/alerts/settings/LOW_STOCK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void updateAlertSettings_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/v1/mobile/alerts/settings/LOW_STOCK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
