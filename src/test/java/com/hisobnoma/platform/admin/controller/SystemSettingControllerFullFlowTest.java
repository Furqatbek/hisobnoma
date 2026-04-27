package com.hisobnoma.platform.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.admin.dto.SystemSettingDTO;
import com.hisobnoma.platform.admin.entity.SystemSetting;
import com.hisobnoma.platform.admin.entity.SystemSetting.SettingValueType;
import com.hisobnoma.platform.admin.repository.SystemSettingRepository;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SystemSettingControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private SystemSettingRepository systemSettingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    private Tenant tenant;
    private User adminUser;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("SysSetting Tenant").code("FULLFLOW_SYSSET").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("syssettingadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        systemSettingRepository.saveAndFlush(SystemSetting.builder()
                .settingKey("app.currency").settingValue("USD").defaultValue("USD")
                .description("Default currency").category("general")
                .valueType(SystemSetting.SettingValueType.STRING).active(true).build());

        systemSettingRepository.saveAndFlush(SystemSetting.builder()
                .settingKey("app.tax.rate").settingValue("15").defaultValue("10")
                .description("Tax rate").category("finance")
                .valueType(SystemSetting.SettingValueType.INTEGER).active(true).build());

        systemSettingRepository.saveAndFlush(SystemSetting.builder()
                .settingKey("app.notifications.enabled").settingValue("true").defaultValue("true")
                .description("Enable notifications").category("general")
                .valueType(SystemSetting.SettingValueType.BOOLEAN).active(true).build());
    }

    private RequestPostProcessor viewAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("ADMIN_SETTINGS_VIEW"),
                        new SimpleGrantedAuthority("ADMIN_SETTINGS_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/admin/settings/system ----

    @Test
    void getAllSettings_returnsAllSettings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/system").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(3)));
    }

    // ---- GET /api/v1/admin/settings/system/categories ----

    @Test
    void getCategories_returnsDistinctCategories() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/system/categories").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data", hasItems("general", "finance")));
    }

    // ---- GET /api/v1/admin/settings/system/category/{category} ----

    @Test
    void getSettingsByCategory_returnsFilteredSettings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/system/category/general").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/admin/settings/system/{key} ----

    @Test
    void getSetting_existingKey_returnsSetting() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/system/app.currency").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingKey").value("app.currency"))
                .andExpect(jsonPath("$.data.settingValue").value("USD"));
    }

    // ---- POST /api/v1/admin/settings/system ----

    @Test
    void createSetting_validRequest_createsInDb() throws Exception {
        SystemSettingDTO dto = SystemSettingDTO.builder()
                .settingKey("app.new.setting")
                .settingValue("test-value")
                .defaultValue("default")
                .description("New test setting")
                .category("test")
                .valueType(SettingValueType.STRING)
                .active(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/settings/system")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.settingKey").value("app.new.setting"));
    }

    // ---- PUT /api/v1/admin/settings/system/{key} ----

    @Test
    void updateSetting_existingKey_updatesMetadata() throws Exception {
        // Note: updateEntity mapper intentionally ignores settingValue (prevents saving masked values)
        // Use PUT /{key}/value endpoint to update the actual value
        SystemSettingDTO dto = SystemSettingDTO.builder()
                .settingKey("app.currency")
                .description("Updated currency description")
                .category("general")
                .valueType(SettingValueType.STRING)
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/admin/settings/system/app.currency")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Updated currency description"));
    }

    // ---- PUT /api/v1/admin/settings/system/{key}/value ----

    @Test
    void updateSettingValue_existingKey_updatesOnlyValue() throws Exception {
        mockMvc.perform(put("/api/v1/admin/settings/system/app.tax.rate/value")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "20"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingValue").value("20"));
    }

    // ---- PUT /api/v1/admin/settings/system/batch ----

    @Test
    void batchUpdate_multipleSettings_updatesAll() throws Exception {
        Map<String, String> updates = Map.of(
                "app.currency", "GBP",
                "app.tax.rate", "25");

        mockMvc.perform(put("/api/v1/admin/settings/system/batch")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk());
    }

    // ---- DELETE /api/v1/admin/settings/system/{key} ----

    @Test
    void deleteSetting_existingKey_deactivates() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/settings/system/app.notifications.enabled")
                        .with(viewAuth()))
                .andExpect(status().isOk());
    }

    // ---- Permission checks ----

    @Test
    void getAllSettings_noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/settings/system")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // ---- Full CRUD lifecycle ----

    @Test
    void fullCrudLifecycle_createReadUpdateDelete() throws Exception {
        // Create
        SystemSettingDTO createDto = SystemSettingDTO.builder()
                .settingKey("lifecycle.setting")
                .settingValue("initial")
                .defaultValue("default")
                .description("Lifecycle test")
                .category("test")
                .valueType(SettingValueType.STRING)
                .active(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/settings/system")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated());

        // Read
        mockMvc.perform(get("/api/v1/admin/settings/system/lifecycle.setting").with(viewAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingValue").value("initial"));

        // Update value
        mockMvc.perform(put("/api/v1/admin/settings/system/lifecycle.setting/value")
                        .with(viewAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingValue").value("updated"));

        // Delete
        mockMvc.perform(delete("/api/v1/admin/settings/system/lifecycle.setting").with(viewAuth()))
                .andExpect(status().isOk());
    }
}
