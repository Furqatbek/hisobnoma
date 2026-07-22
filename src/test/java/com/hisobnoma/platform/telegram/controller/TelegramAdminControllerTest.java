package com.hisobnoma.platform.telegram.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.admin.service.SystemSettingService;
import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.telegram.config.TelegramProperties;
import com.hisobnoma.platform.telegram.service.TelegramApiClient;
import com.hisobnoma.platform.telegram.service.TelegramNotificationService;
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
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TelegramAdminControllerTest {

    private static final Long TENANT_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private SecurityContextHelper securityContextHelper;

    @MockBean
    private TelegramProperties properties;

    @MockBean
    private TelegramApiClient telegramApiClient;

    @MockBean
    private TelegramNotificationService notificationService;

    @MockBean
    private TenantSettingService tenantSettingService;

    @MockBean
    private SystemSettingService systemSettingService;

    // ==================== GET /api/v1/telegram/admin/info ====================

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void getBotInfo_authenticated_returns200() throws Exception {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(1L);
        when(tenantSettingService.getSettingValue(anyString(), anyString())).thenReturn("false");
        when(systemSettingService.getSettingValue(anyString(), anyString())).thenReturn("false");
        when(properties.isEnabled()).thenReturn(false);
        when(properties.getBotUsername()).thenReturn("test_bot");
        when(properties.getBotToken()).thenReturn("");
        when(telegramApiClient.isConfigured()).thenReturn(false);
        when(userRepository.countByTenantId(1L)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/telegram/admin/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.botUsername").value("test_bot"));
    }

    @Test
    void getBotInfo_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/admin/info"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getBotInfo_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/admin/info"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/telegram/admin/settings ====================

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void getSettings_authenticated_returns200() throws Exception {
        when(tenantSettingService.getSettingValue(anyString(), anyString())).thenReturn("false");
        when(systemSettingService.getSettingValue(anyString(), anyString())).thenReturn("false");
        when(properties.isEnabled()).thenReturn(false);
        when(properties.getBotToken()).thenReturn("");
        when(properties.getBotUsername()).thenReturn("test_bot");

        mockMvc.perform(get("/api/v1/telegram/admin/settings"))
                .andExpect(status().isOk());
    }

    @Test
    void getSettings_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/admin/settings"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getSettings_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/admin/settings"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/telegram/admin/settings ====================

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void saveSettings_authenticated_returns200() throws Exception {
        doNothing().when(tenantSettingService).updateSettings(any());
        doNothing().when(systemSettingService).updateSettings(any());
        when(tenantSettingService.getSettingValue(anyString(), anyString())).thenReturn("false");
        when(systemSettingService.getSettingValue(anyString(), anyString())).thenReturn("false");
        when(properties.isEnabled()).thenReturn(false);

        String requestBody = """
                {
                    "enabled": true,
                    "botToken": "123:ABC",
                    "botUsername": "test_bot"
                }
                """;

        mockMvc.perform(post("/api/v1/telegram/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void saveSettings_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void saveSettings_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/telegram/admin/users ====================

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void getConnectedUsers_authenticated_returns200() throws Exception {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(1L);
        when(userRepository.findUsersWithTelegramByTenantId(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/telegram/admin/users"))
                .andExpect(status().isOk());
    }

    @Test
    void getConnectedUsers_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getConnectedUsers_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/admin/users"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/telegram/admin/send ====================

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void sendMessage_authenticated_returns200() throws Exception {
        when(telegramApiClient.isConfigured()).thenReturn(true);
        User user = new User();
        user.setId(1L);
        user.setTelegramChatId(12345L);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(userRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(user));
        doNothing().when(notificationService).sendDirectMessage(any(), any(), any());

        String requestBody = """
                {
                    "userId": 1,
                    "title": "Test Title",
                    "message": "Test Message"
                }
                """;

        mockMvc.perform(post("/api/v1/telegram/admin/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("sent"));
    }

    @Test
    void sendMessage_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/admin/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"title\":\"t\",\"message\":\"m\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void sendMessage_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/admin/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":1,\"title\":\"t\",\"message\":\"m\"}"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/telegram/admin/broadcast ====================

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void broadcastMessage_authenticated_returns200() throws Exception {
        when(telegramApiClient.isConfigured()).thenReturn(true);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(1L);
        when(userRepository.findUsersWithTelegramByTenantId(1L)).thenReturn(List.of());

        String requestBody = """
                {
                    "title": "Broadcast Title",
                    "message": "Broadcast Message"
                }
                """;

        mockMvc.perform(post("/api/v1/telegram/admin/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void broadcastMessage_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/admin/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"message\":\"m\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void broadcastMessage_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/admin/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"t\",\"message\":\"m\"}"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/v1/telegram/admin/users/{userId}/unlink ====================

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void adminUnlinkUser_authenticated_returns200() throws Exception {
        User user = new User();
        user.setId(1L);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(userRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(user));

        mockMvc.perform(delete("/api/v1/telegram/admin/users/1/unlink"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("unlinked"));
    }

    @Test
    void adminUnlinkUser_unauthenticated_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/telegram/admin/users/1/unlink"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void adminUnlinkUser_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/v1/telegram/admin/users/1/unlink"))
                .andExpect(status().isForbidden());
    }

    // ==================== GET /api/v1/telegram/admin/daily-report ====================

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void getDailyReportSettings_authenticated_returns200() throws Exception {
        when(tenantSettingService.getSettingValue(anyString(), anyString())).thenReturn("true");

        mockMvc.perform(get("/api/v1/telegram/admin/daily-report"))
                .andExpect(status().isOk());
    }

    @Test
    void getDailyReportSettings_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/admin/daily-report"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void getDailyReportSettings_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/v1/telegram/admin/daily-report"))
                .andExpect(status().isForbidden());
    }

    // ==================== POST /api/v1/telegram/admin/daily-report ====================

    @Test
    @WithMockUser(authorities = "ADMIN_SETTINGS_MANAGE")
    void saveDailyReportSettings_authenticated_returns200() throws Exception {
        doNothing().when(tenantSettingService).updateSettings(any());
        doNothing().when(systemSettingService).updateSettings(any());

        String requestBody = """
                {
                    "enabled": true,
                    "trigger": "FIXED_TIME",
                    "time": "20:00",
                    "salesEnabled": true,
                    "inventoryEnabled": true,
                    "financeEnabled": true
                }
                """;

        mockMvc.perform(post("/api/v1/telegram/admin/daily-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    @Test
    void saveDailyReportSettings_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/admin/daily-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "SOME_OTHER_PERMISSION")
    void saveDailyReportSettings_noPermission_returns403() throws Exception {
        mockMvc.perform(post("/api/v1/telegram/admin/daily-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }
}
