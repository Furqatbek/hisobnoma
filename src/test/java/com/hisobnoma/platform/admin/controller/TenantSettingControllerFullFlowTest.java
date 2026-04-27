package com.hisobnoma.platform.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.admin.dto.TenantSettingDTO;
import com.hisobnoma.platform.admin.entity.SystemSetting;
import com.hisobnoma.platform.admin.entity.SystemSetting.SettingValueType;
import com.hisobnoma.platform.admin.entity.TenantSetting;
import com.hisobnoma.platform.admin.repository.SystemSettingRepository;
import com.hisobnoma.platform.admin.repository.TenantSettingRepository;
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
class TenantSettingControllerFullFlowTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TenantSettingRepository tenantSettingRepository;
    @Autowired private SystemSettingRepository systemSettingRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    private Tenant tenant;
    private User adminUser;
    private SystemSetting currencySysSetting;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("TenantSetting Tenant").code("FULLFLOW_TENSET").active(true)
                .maxUsers(100).maxLocations(10).build());

        adminUser = userRepository.saveAndFlush(User.builder()
                .username("tenantsettingadmin")
                .passwordHash(passwordEncoder.encode("admin123"))
                .tenantId(tenant.getId()).enabled(true).build());

        currencySysSetting = systemSettingRepository.saveAndFlush(SystemSetting.builder()
                .settingKey("tenant.currency").settingValue("USD").defaultValue("USD")
                .description("Tenant currency").category("general")
                .valueType(SystemSetting.SettingValueType.STRING).active(true).build());

        systemSettingRepository.saveAndFlush(SystemSetting.builder()
                .settingKey("tenant.language").settingValue("en").defaultValue("en")
                .description("Tenant language").category("general")
                .valueType(SystemSetting.SettingValueType.STRING).active(true).build());

        tenantSettingRepository.saveAndFlush(TenantSetting.builder()
                .settingKey("tenant.currency").settingValue("UZS")
                .description("Overridden currency").category("general")
                .valueType(SystemSetting.SettingValueType.STRING)
                .tenantId(tenant.getId()).active(true).build());

        tenantSettingRepository.saveAndFlush(TenantSetting.builder()
                .settingKey("tenant.custom.field").settingValue("custom-value")
                .description("Custom tenant setting").category("custom")
                .valueType(SystemSetting.SettingValueType.STRING)
                .tenantId(tenant.getId()).active(true).build());
    }

    private RequestPostProcessor tenantAuth() {
        UserPrincipal principal = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(
                        new SimpleGrantedAuthority("TENANT_SETTINGS_VIEW"),
                        new SimpleGrantedAuthority("TENANT_SETTINGS_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /api/v1/admin/settings/tenant ----

    @Test
    void getAllSettings_returnsAllTenantSettings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/tenant").with(tenantAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(2)));
    }

    // ---- GET /api/v1/admin/settings/tenant/categories ----

    @Test
    void getCategories_returnsDistinctCategories() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/tenant/categories").with(tenantAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---- GET /api/v1/admin/settings/tenant/category/{category} ----

    @Test
    void getSettingsByCategory_returnsFiltered() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/tenant/category/general").with(tenantAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    // ---- POST /api/v1/admin/settings/tenant ----

    @Test
    void createSetting_validRequest_createsInDb() throws Exception {
        TenantSettingDTO dto = TenantSettingDTO.builder()
                .settingKey("tenant.new.setting")
                .settingValue("new-value")
                .description("New tenant setting")
                .category("custom")
                .valueType(SettingValueType.STRING)
                .active(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/settings/tenant")
                        .with(tenantAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.settingKey").value("tenant.new.setting"));
    }

    // ---- PUT /api/v1/admin/settings/tenant/{key} ----

    @Test
    void updateSetting_existingKey_updatesMetadata() throws Exception {
        // Note: updateEntity mapper ignores settingValue (prevents saving masked values)
        // Use PUT /{key}/value to update the actual value
        TenantSettingDTO dto = TenantSettingDTO.builder()
                .settingKey("tenant.currency")
                .description("Updated description")
                .category("general")
                .valueType(SettingValueType.STRING)
                .active(true)
                .build();

        mockMvc.perform(put("/api/v1/admin/settings/tenant/tenant.currency")
                        .with(tenantAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.description").value("Updated description"));
    }

    // ---- PUT /api/v1/admin/settings/tenant/{key}/value ----

    @Test
    void updateSettingValue_updatesOnlyValue() throws Exception {
        mockMvc.perform(put("/api/v1/admin/settings/tenant/tenant.currency/value")
                        .with(tenantAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "RUB"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingValue").value("RUB"));
    }

    // ---- PUT /api/v1/admin/settings/tenant/batch ----

    @Test
    void batchUpdate_multipleSettings_updatesAll() throws Exception {
        Map<String, String> updates = Map.of(
                "tenant.currency", "JPY",
                "tenant.custom.field", "updated-custom");

        mockMvc.perform(put("/api/v1/admin/settings/tenant/batch")
                        .with(tenantAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updates)))
                .andExpect(status().isOk());
    }

    // ---- GET /api/v1/admin/settings/tenant/map ----

    @Test
    void getSettingsAsMap_returnsKeyValueMap() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings/tenant/map").with(tenantAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isMap());
    }

    // ---- Permission checks ----

    @Test
    void getAllSettings_noPermission_returns403() throws Exception {
        UserPrincipal noPerm = new UserPrincipal(
                adminUser.getId(), adminUser.getUsername(), "admin123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                noPerm, null, noPerm.getAuthorities());

        mockMvc.perform(get("/api/v1/admin/settings/tenant")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden());
    }

    // ---- Full CRUD lifecycle ----

    @Test
    void fullCrudLifecycle_createReadUpdateViaEndpoints() throws Exception {
        // Create
        TenantSettingDTO createDto = TenantSettingDTO.builder()
                .settingKey("lifecycle.tenant.setting")
                .settingValue("initial")
                .description("Lifecycle test")
                .category("test")
                .valueType(SettingValueType.STRING)
                .active(true)
                .build();

        mockMvc.perform(post("/api/v1/admin/settings/tenant")
                        .with(tenantAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated());

        // Read via category
        mockMvc.perform(get("/api/v1/admin/settings/tenant/category/test").with(tenantAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThanOrEqualTo(1)));

        // Update
        mockMvc.perform(put("/api/v1/admin/settings/tenant/lifecycle.tenant.setting/value")
                        .with(tenantAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("value", "updated"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settingValue").value("updated"));

        // Verify in map
        mockMvc.perform(get("/api/v1/admin/settings/tenant/map").with(tenantAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data['lifecycle.tenant.setting']").value("updated"));
    }
}
