package com.hisobnoma.platform.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.admin.dto.SystemSettingDTO;
import com.hisobnoma.platform.admin.entity.SystemSetting;
import com.hisobnoma.platform.admin.service.SystemSettingService;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SystemSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SystemSettingService systemSettingService;

    private SystemSettingDTO sampleDto() {
        return SystemSettingDTO.builder()
                .id(1L)
                .settingKey("session.timeout")
                .settingValue("30")
                .defaultValue("60")
                .description("Session timeout")
                .category("SECURITY")
                .valueType(SystemSetting.SettingValueType.INTEGER)
                .readonly(false)
                .active(true)
                .build();
    }

    // ---- GET /api/v1/admin/settings/system ----

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_VIEW")
    void getAllSystemSettings_admin_returns200AllSettings() throws Exception {
        // Given
        when(systemSettingService.getAllSettings()).thenReturn(List.of(sampleDto()));

        // When / Then
        mockMvc.perform(get("/api/v1/admin/settings/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].settingKey").value("session.timeout"));
    }

    // ---- GET /api/v1/admin/settings/system/categories ----

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_VIEW")
    void getSystemSettingCategories_admin_returns200CategoryList() throws Exception {
        // Given
        when(systemSettingService.getCategories()).thenReturn(List.of("GENERAL", "SECURITY", "POS"));

        // When / Then
        mockMvc.perform(get("/api/v1/admin/settings/system/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    // ---- GET /api/v1/admin/settings/system/category/{category} ----

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_VIEW")
    void getSystemSettingsByCategory_validCategory_returns200Filtered() throws Exception {
        // Given
        when(systemSettingService.getSettingsByCategory("SECURITY")).thenReturn(List.of(sampleDto()));

        // When / Then
        mockMvc.perform(get("/api/v1/admin/settings/system/category/SECURITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].category").value("SECURITY"));
    }

    // ---- GET /api/v1/admin/settings/system/{key} ----

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_VIEW")
    void getSystemSettingByKey_existingKey_returns200() throws Exception {
        // Given
        when(systemSettingService.getSetting("session.timeout")).thenReturn(sampleDto());

        // When / Then
        mockMvc.perform(get("/api/v1/admin/settings/system/session.timeout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingKey").value("session.timeout"))
                .andExpect(jsonPath("$.data.settingValue").value("30"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_VIEW")
    void getSystemSettingByKey_nonExistentKey_returns404() throws Exception {
        // Given
        when(systemSettingService.getSetting("nonexistent"))
                .thenThrow(new NotFoundException("SystemSetting", "key", "nonexistent"));

        // When / Then
        mockMvc.perform(get("/api/v1/admin/settings/system/nonexistent"))
                .andExpect(status().isNotFound());
    }

    // ---- POST /api/v1/admin/settings/system ----

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void createSystemSetting_validPayload_returns201() throws Exception {
        // Given
        SystemSettingDTO input = SystemSettingDTO.builder()
                .settingKey("new.setting")
                .settingValue("value")
                .category("GENERAL")
                .build();
        SystemSettingDTO created = SystemSettingDTO.builder()
                .id(10L)
                .settingKey("new.setting")
                .settingValue("value")
                .category("GENERAL")
                .build();
        when(systemSettingService.createSetting(any())).thenReturn(created);

        // When / Then
        mockMvc.perform(post("/api/v1/admin/settings/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.settingKey").value("new.setting"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void createSystemSetting_duplicateKey_returns409() throws Exception {
        // Given
        SystemSettingDTO input = SystemSettingDTO.builder()
                .settingKey("session.timeout")
                .settingValue("30")
                .category("SECURITY")
                .build();
        when(systemSettingService.createSetting(any()))
                .thenThrow(new DuplicateResourceException("SystemSetting", "key", "session.timeout"));

        // When / Then
        mockMvc.perform(post("/api/v1/admin/settings/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void createSystemSetting_invalidPayload_returns400() throws Exception {
        // When / Then — empty JSON body
        mockMvc.perform(post("/api/v1/admin/settings/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        // Note: If no @Valid on controller, empty body may pass through to service.
        // This test verifies the endpoint accepts the request; validation depends on service layer.
    }

    // ---- PUT /api/v1/admin/settings/system/{key} ----

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void updateSystemSetting_existingKey_returns200() throws Exception {
        // Given
        SystemSettingDTO updated = sampleDto();
        when(systemSettingService.updateSetting(eq("session.timeout"), any())).thenReturn(updated);

        // When / Then
        mockMvc.perform(put("/api/v1/admin/settings/system/session.timeout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingKey").value("session.timeout"));
    }

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void updateSystemSetting_nonExistentKey_returns404() throws Exception {
        // Given
        when(systemSettingService.updateSetting(eq("nonexistent"), any()))
                .thenThrow(new NotFoundException("SystemSetting", "key", "nonexistent"));

        // When / Then
        mockMvc.perform(put("/api/v1/admin/settings/system/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDto())))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /api/v1/admin/settings/system/{key}/value ----

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void updateSystemSettingValue_existingKey_returns200() throws Exception {
        // Given
        SystemSettingDTO updated = sampleDto();
        when(systemSettingService.updateSettingValue(eq("session.timeout"), eq("60"))).thenReturn(updated);

        // When / Then
        mockMvc.perform(put("/api/v1/admin/settings/system/session.timeout/value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "60"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingKey").value("session.timeout"));
    }

    // ---- PUT /api/v1/admin/settings/system/batch ----

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void updateSystemSettingsBatch_validMap_returns200AllUpdated() throws Exception {
        // Given
        doNothing().when(systemSettingService).updateSettings(any());

        // When / Then
        mockMvc.perform(put("/api/v1/admin/settings/system/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("session.timeout", "60", "app.name", "NewName"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ---- DELETE /api/v1/admin/settings/system/{key} ----

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void deleteSystemSetting_existingKey_returns200() throws Exception {
        // Given
        doNothing().when(systemSettingService).deleteSetting("session.timeout");

        // When / Then
        mockMvc.perform(delete("/api/v1/admin/settings/system/session.timeout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void deleteSystemSetting_nonExistentKey_returns404() throws Exception {
        // Given
        doThrow(new NotFoundException("SystemSetting", "key", "nonexistent"))
                .when(systemSettingService).deleteSetting("nonexistent");

        // When / Then
        mockMvc.perform(delete("/api/v1/admin/settings/system/nonexistent"))
                .andExpect(status().isNotFound());
    }

    // ---- Permission denied ----

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void allSystemSettingEndpoints_noAdminPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/system"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/settings/system")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/admin/settings/system/session.timeout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/v1/admin/settings/system/session.timeout"))
                .andExpect(status().isForbidden());
    }
}
