package com.hisobnoma.platform.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.admin.dto.TenantSettingDTO;
import com.hisobnoma.platform.admin.service.TenantSettingService;
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
class TenantSettingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TenantSettingService tenantSettingService;

    private TenantSettingDTO sampleDto() {
        return TenantSettingDTO.builder()
                .id(1L)
                .tenantId(1L)
                .settingKey("pos.receipt.header")
                .settingValue("Welcome")
                .description("Receipt header")
                .category("POS")
                .active(true)
                .build();
    }

    // ---- GET /api/v1/admin/settings/tenant ----

    @Test
    @WithMockUser(authorities = "TENANT_SETTINGS_VIEW")
    void getAllTenantSettings_admin_returns200() throws Exception {
        // Given
        when(tenantSettingService.getAllSettings()).thenReturn(List.of(sampleDto()));

        // When / Then
        mockMvc.perform(get("/api/v1/admin/settings/tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].settingKey").value("pos.receipt.header"));
    }

    // ---- GET /api/v1/admin/settings/tenant/categories ----

    @Test
    @WithMockUser(authorities = "TENANT_SETTINGS_VIEW")
    void getTenantSettingCategories_admin_returns200() throws Exception {
        // Given
        when(tenantSettingService.getCategories()).thenReturn(List.of("POS", "GENERAL"));

        // When / Then
        mockMvc.perform(get("/api/v1/admin/settings/tenant/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    // ---- GET /api/v1/admin/settings/tenant/category/{category} ----

    @Test
    @WithMockUser(authorities = "TENANT_SETTINGS_VIEW")
    void getTenantSettingsByCategory_validCategory_returns200() throws Exception {
        // Given
        when(tenantSettingService.getSettingsByCategory("POS")).thenReturn(List.of(sampleDto()));

        // When / Then
        mockMvc.perform(get("/api/v1/admin/settings/tenant/category/POS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].category").value("POS"));
    }

    // ---- GET /api/v1/admin/settings/tenant/map ----

    @Test
    @WithMockUser(authorities = "TENANT_SETTINGS_VIEW")
    void getTenantSettingsAsMap_admin_returns200Map() throws Exception {
        // Given
        when(tenantSettingService.getSettingsAsMap())
                .thenReturn(Map.of("pos.receipt.header", "Welcome", "pos.receipt.footer", "Thank you"));

        // When / Then
        mockMvc.perform(get("/api/v1/admin/settings/tenant/map"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isMap())
                .andExpect(jsonPath("$.data['pos.receipt.header']").value("Welcome"));
    }

    // ---- POST /api/v1/admin/settings/tenant ----

    @Test
    @WithMockUser(authorities = "TENANT_SETTINGS_MANAGE")
    void createTenantSetting_validPayload_returns201() throws Exception {
        // Given
        TenantSettingDTO input = TenantSettingDTO.builder()
                .settingKey("new.key")
                .settingValue("value")
                .category("GENERAL")
                .build();
        TenantSettingDTO created = TenantSettingDTO.builder()
                .id(10L)
                .tenantId(1L)
                .settingKey("new.key")
                .settingValue("value")
                .category("GENERAL")
                .build();
        when(tenantSettingService.createSetting(any())).thenReturn(created);

        // When / Then
        mockMvc.perform(post("/api/v1/admin/settings/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.settingKey").value("new.key"));
    }

    @Test
    @WithMockUser(authorities = "TENANT_SETTINGS_MANAGE")
    void createTenantSetting_duplicateKey_returns409() throws Exception {
        // Given
        TenantSettingDTO input = TenantSettingDTO.builder()
                .settingKey("pos.receipt.header")
                .settingValue("value")
                .category("POS")
                .build();
        when(tenantSettingService.createSetting(any()))
                .thenThrow(new DuplicateResourceException("TenantSetting", "key", "pos.receipt.header"));

        // When / Then
        mockMvc.perform(post("/api/v1/admin/settings/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isConflict());
    }

    // ---- PUT /api/v1/admin/settings/tenant/{key} ----

    @Test
    @WithMockUser(authorities = "TENANT_SETTINGS_MANAGE")
    void updateTenantSetting_existingKey_returns200() throws Exception {
        // Given
        when(tenantSettingService.updateSetting(eq("pos.receipt.header"), any())).thenReturn(sampleDto());

        // When / Then
        mockMvc.perform(put("/api/v1/admin/settings/tenant/pos.receipt.header")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDto())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingKey").value("pos.receipt.header"));
    }

    @Test
    @WithMockUser(authorities = "TENANT_SETTINGS_MANAGE")
    void updateTenantSetting_nonExistentKey_returns404() throws Exception {
        // Given
        when(tenantSettingService.updateSetting(eq("nonexistent"), any()))
                .thenThrow(new NotFoundException("TenantSetting", "key", "nonexistent"));

        // When / Then
        mockMvc.perform(put("/api/v1/admin/settings/tenant/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDto())))
                .andExpect(status().isNotFound());
    }

    // ---- PUT /api/v1/admin/settings/tenant/{key}/value ----

    @Test
    @WithMockUser(authorities = "TENANT_SETTINGS_MANAGE")
    void updateTenantSettingValue_existingKey_returns200() throws Exception {
        // Given
        when(tenantSettingService.updateSettingValue(eq("pos.receipt.header"), eq("New Header")))
                .thenReturn(sampleDto());

        // When / Then
        mockMvc.perform(put("/api/v1/admin/settings/tenant/pos.receipt.header/value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "New Header"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingKey").value("pos.receipt.header"));
    }

    // ---- PUT /api/v1/admin/settings/tenant/batch ----

    @Test
    @WithMockUser(authorities = "TENANT_SETTINGS_MANAGE")
    void updateTenantSettingsBatch_validMap_returns200() throws Exception {
        // Given
        doNothing().when(tenantSettingService).updateSettings(any());

        // When / Then
        mockMvc.perform(put("/api/v1/admin/settings/tenant/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("pos.receipt.header", "New", "pos.receipt.footer", "Thanks"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // ---- Permission denied ----

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void allTenantSettingEndpoints_noAdminPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/tenant"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/admin/settings/tenant")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/admin/settings/tenant/some.key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
