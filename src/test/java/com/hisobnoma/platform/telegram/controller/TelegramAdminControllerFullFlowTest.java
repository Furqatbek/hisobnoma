package com.hisobnoma.platform.telegram.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import jakarta.persistence.EntityManager;
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
class TelegramAdminControllerFullFlowTest {

    private static final String BASE_URL = "/api/v1/telegram/admin";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager entityManager;

    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.saveAndFlush(Tenant.builder()
                .name("Telegram Admin Test").code("FULLFLOW_TGADM").active(true)
                .maxUsers(100).maxLocations(10).build());

        user = userRepository.saveAndFlush(User.builder()
                .username("tgadmtestuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .tenantId(tenant.getId()).enabled(true).build());

        entityManager.clear();
    }

    private RequestPostProcessor adminAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("ADMIN_SETTINGS_MANAGE")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    private RequestPostProcessor noPermAuth() {
        UserPrincipal principal = new UserPrincipal(
                user.getId(), user.getUsername(), "password123", tenant.getId(),
                true, true, List.of(new SimpleGrantedAuthority("SOME_OTHER_PERM")));
        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());
        return authentication(auth);
    }

    // ---- GET /settings ----

    @Test
    void getSettings_returnsDefaults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/settings").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").exists())
                .andExpect(jsonPath("$.botUsername").isNotEmpty());
    }

    // ---- POST /settings + GET /settings ----

    @Test
    void saveSettings_andRetrieve() throws Exception {
        Map<String, Object> settingsBody = Map.of(
                "enabled", true,
                "botUsername", "test_bot_updated");

        mockMvc.perform(post(BASE_URL + "/settings")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(settingsBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));

        mockMvc.perform(get(BASE_URL + "/settings").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.botUsername").value("test_bot_updated"));
    }

    // ---- GET /users ----

    @Test
    void getUsers_returnsEmptyList() throws Exception {
        mockMvc.perform(get(BASE_URL + "/users").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ---- DELETE /users/{userId}/unlink ----

    @Test
    void unlinkUser_returnsSuccess() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/users/" + user.getId() + "/unlink").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("unlinked"));
    }

    // ---- GET /daily-report ----

    @Test
    void getDailyReport_returnsDefaults() throws Exception {
        mockMvc.perform(get(BASE_URL + "/daily-report").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.trigger").value("FIXED_TIME"))
                .andExpect(jsonPath("$.time").value("20:00"))
                .andExpect(jsonPath("$.salesEnabled").value(true))
                .andExpect(jsonPath("$.inventoryEnabled").value(true))
                .andExpect(jsonPath("$.financeEnabled").value(true));
    }

    // ---- POST /daily-report + GET /daily-report ----

    @Test
    void saveDailyReport_andRetrieve() throws Exception {
        Map<String, Object> reportBody = Map.of(
                "enabled", false,
                "trigger", "SHIFT_CLOSE",
                "time", "22:00",
                "salesEnabled", true,
                "inventoryEnabled", false,
                "financeEnabled", true);

        mockMvc.perform(post(BASE_URL + "/daily-report")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reportBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("saved"));

        mockMvc.perform(get(BASE_URL + "/daily-report").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.trigger").value("SHIFT_CLOSE"))
                .andExpect(jsonPath("$.time").value("22:00"))
                .andExpect(jsonPath("$.inventoryEnabled").value(false));
    }

    // ---- POST /send (bot not configured) ----

    @Test
    void sendMessage_botNotConfigured_returns400() throws Exception {
        Map<String, Object> sendBody = Map.of(
                "userId", user.getId(),
                "title", "Test Title",
                "message", "Test message body");

        mockMvc.perform(post(BASE_URL + "/send")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // ---- POST /broadcast (bot not configured) ----

    @Test
    void broadcastMessage_botNotConfigured_returns400() throws Exception {
        Map<String, Object> broadcastBody = Map.of(
                "title", "Broadcast Title",
                "message", "Broadcast message body");

        mockMvc.perform(post(BASE_URL + "/broadcast")
                        .with(adminAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(broadcastBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    // ---- GET /info ----

    @Test
    void getInfo_botNotConfigured_returnsPartialInfo() throws Exception {
        mockMvc.perform(get(BASE_URL + "/info").with(adminAuth()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectedUsers").value(0))
                .andExpect(jsonPath("$.botName").value(""))
                .andExpect(jsonPath("$.totalUsers").isNumber());
    }

    // ---- Permission checks ----

    @Test
    void permissionCheck_noAuth_returns403() throws Exception {
        mockMvc.perform(get(BASE_URL + "/settings").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE_URL + "/settings")
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/users").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/daily-report").with(noPermAuth()))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(BASE_URL + "/daily-report")
                        .with(noPermAuth())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_URL + "/info").with(noPermAuth()))
                .andExpect(status().isForbidden());
    }
}
