package com.hisobnoma.platform.telegram.controller;

import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.telegram.config.TelegramProperties;
import com.hisobnoma.platform.telegram.service.TelegramApiClient;
import com.hisobnoma.platform.telegram.service.TelegramNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for Telegram bot management.
 * Allows configuring bot credentials from the frontend.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/telegram/admin")
@RequiredArgsConstructor
public class TelegramAdminController {

    private static final String SETTING_ENABLED = "telegram.enabled";
    private static final String SETTING_BOT_TOKEN = "telegram.bot_token";
    private static final String SETTING_BOT_USERNAME = "telegram.bot_username";

    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;
    private final TelegramProperties properties;
    private final TelegramApiClient telegramApiClient;
    private final TelegramNotificationService notificationService;
    private final TenantSettingService tenantSettingService;

    /**
     * Get bot info and statistics.
     */
    @GetMapping("/info")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, Object>> getBotInfo() {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        loadSettingsFromDb();

        Map<String, Object> info = new HashMap<>();
        info.put("enabled", properties.isEnabled());
        info.put("botUsername", properties.getBotUsername());
        info.put("tokenConfigured", properties.getBotToken() != null && !properties.getBotToken().isBlank());

        if (!telegramApiClient.isConfigured()) {
            info.put("botName", "");
            info.put("connectedUsers", 0);
            info.put("totalUsers", userRepository.countByTenantId(tenantId));
            return ResponseEntity.ok(info);
        }

        List<User> linkedUsers = userRepository.findUsersWithTelegramByTenantId(tenantId);

        Map<String, Object> botInfo = telegramApiClient.getMe();
        String botName = "";
        if (botInfo != null && botInfo.get("result") instanceof Map<?, ?> result) {
            botName = (String) result.get("first_name");
        }

        info.put("botName", botName);
        info.put("connectedUsers", linkedUsers.size());
        info.put("totalUsers", userRepository.countByTenantId(tenantId));

        return ResponseEntity.ok(info);
    }

    /**
     * Get current Telegram settings (token is masked).
     */
    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, Object>> getSettings() {
        loadSettingsFromDb();

        String token = properties.getBotToken();
        String maskedToken = "";
        if (token != null && token.length() > 8) {
            maskedToken = token.substring(0, 4) + "****" + token.substring(token.length() - 4);
        }

        Map<String, Object> settings = new HashMap<>();
        settings.put("enabled", properties.isEnabled());
        settings.put("botToken", maskedToken);
        settings.put("botUsername", properties.getBotUsername());
        return ResponseEntity.ok(settings);
    }

    /**
     * Save Telegram settings (enable/disable, token, username).
     */
    @PostMapping("/settings")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, Object>> saveSettings(@Valid @RequestBody SaveSettingsRequest request) {
        tenantSettingService.updateSettings(Map.of(
                SETTING_ENABLED, String.valueOf(request.isEnabled()),
                SETTING_BOT_USERNAME, request.getBotUsername() != null ? request.getBotUsername() : "hisobnoma_bot"
        ));

        // Only update token if a new one was provided (not masked)
        if (request.getBotToken() != null && !request.getBotToken().isBlank()
                && !request.getBotToken().contains("****")) {
            tenantSettingService.updateSettings(Map.of(SETTING_BOT_TOKEN, request.getBotToken()));
        }

        // Reload properties from DB
        loadSettingsFromDb();

        // Validate the token by calling getMe
        Map<String, Object> result = new HashMap<>();
        result.put("saved", true);

        if (properties.isEnabled() && telegramApiClient.isConfigured()) {
            Map<String, Object> botInfo = telegramApiClient.getMe();
            if (botInfo != null && Boolean.TRUE.equals(botInfo.get("ok"))) {
                result.put("valid", true);
                if (botInfo.get("result") instanceof Map<?, ?> r) {
                    result.put("botName", r.get("first_name"));
                }
            } else {
                result.put("valid", false);
                result.put("error", "Token noto'g'ri yoki bot javob bermayapti");
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get all users with Telegram connected for this tenant.
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<List<Map<String, Object>>> getConnectedUsers() {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        List<User> users = userRepository.findUsersWithTelegramByTenantId(tenantId);

        List<Map<String, Object>> result = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("fullName", u.getFullName());
            map.put("phone", u.getPhone());
            map.put("telegramChatId", u.getTelegramChatId());
            map.put("linkedAt", u.getTelegramLinkedAt() != null ? u.getTelegramLinkedAt().toString() : null);
            return map;
        }).toList();

        return ResponseEntity.ok(result);
    }

    /**
     * Send a message to a specific connected user via Telegram.
     */
    @PostMapping("/send")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, String>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        if (!telegramApiClient.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Telegram bot yoqilmagan"));
        }

        User user = userRepository.findById(request.getUserId()).orElseThrow();
        if (user.getTelegramChatId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Foydalanuvchi Telegramga ulanmagan"));
        }

        notificationService.sendDirectMessage(user.getTelegramChatId(), request.getTitle(), request.getMessage());
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    /**
     * Send a broadcast message to all connected users in this tenant.
     */
    @PostMapping("/broadcast")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, Object>> broadcastMessage(@Valid @RequestBody BroadcastRequest request) {
        if (!telegramApiClient.isConfigured()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Telegram bot yoqilmagan"));
        }

        Long tenantId = securityContextHelper.getRequiredTenantId();
        List<User> users = userRepository.findUsersWithTelegramByTenantId(tenantId);

        int sentCount = 0;
        for (User user : users) {
            if (user.getTelegramChatId() != null) {
                notificationService.sendDirectMessage(user.getTelegramChatId(), request.getTitle(), request.getMessage());
                sentCount++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "status", "sent",
                "recipientCount", sentCount
        ));
    }

    /**
     * Unlink a specific user's Telegram (admin action).
     */
    @DeleteMapping("/users/{userId}/unlink")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, String>> adminUnlinkUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getTelegramChatId() != null) {
            Long chatId = user.getTelegramChatId();
            user.setTelegramChatId(null);
            user.setTelegramLinkedAt(null);
            userRepository.save(user);
            if (telegramApiClient.isConfigured()) {
                telegramApiClient.sendMessage(chatId, "Akkauntingiz administrator tomonidan uzildi.");
            }
        }
        return ResponseEntity.ok(Map.of("status", "unlinked"));
    }

    /**
     * Load Telegram settings from tenant_settings into TelegramProperties.
     */
    private void loadSettingsFromDb() {
        try {
            String enabled = tenantSettingService.getSettingValue(SETTING_ENABLED, "false");
            String token = tenantSettingService.getSettingValue(SETTING_BOT_TOKEN, "");
            String username = tenantSettingService.getSettingValue(SETTING_BOT_USERNAME, "hisobnoma_bot");

            properties.setEnabled("true".equalsIgnoreCase(enabled));
            if (token != null && !token.isBlank()) {
                properties.setBotToken(token);
            }
            if (username != null && !username.isBlank()) {
                properties.setBotUsername(username);
            }
        } catch (Exception e) {
            // Settings may not exist yet — use defaults from application.yml
            log.debug("Telegram settings not found in DB, using defaults");
        }
    }

    // ======================= Daily Report Settings =======================

    private static final String SETTING_REPORT_ENABLED = "telegram.daily_report.enabled";
    private static final String SETTING_REPORT_TIME = "telegram.daily_report.time";
    private static final String SETTING_REPORT_SALES = "telegram.daily_report.sales";
    private static final String SETTING_REPORT_INVENTORY = "telegram.daily_report.inventory";
    private static final String SETTING_REPORT_FINANCE = "telegram.daily_report.finance";

    /**
     * Get daily report settings.
     */
    @GetMapping("/daily-report")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, Object>> getDailyReportSettings() {
        Map<String, Object> settings = new HashMap<>();
        settings.put("enabled", getBoolSetting(SETTING_REPORT_ENABLED, true));
        settings.put("time", getStrSetting(SETTING_REPORT_TIME, "20:00"));
        settings.put("salesEnabled", getBoolSetting(SETTING_REPORT_SALES, true));
        settings.put("inventoryEnabled", getBoolSetting(SETTING_REPORT_INVENTORY, true));
        settings.put("financeEnabled", getBoolSetting(SETTING_REPORT_FINANCE, true));
        return ResponseEntity.ok(settings);
    }

    /**
     * Save daily report settings.
     */
    @PostMapping("/daily-report")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, String>> saveDailyReportSettings(@RequestBody DailyReportSettingsRequest request) {
        tenantSettingService.updateSettings(Map.of(
                SETTING_REPORT_ENABLED, String.valueOf(request.isEnabled()),
                SETTING_REPORT_TIME, request.getTime() != null ? request.getTime() : "20:00",
                SETTING_REPORT_SALES, String.valueOf(request.isSalesEnabled()),
                SETTING_REPORT_INVENTORY, String.valueOf(request.isInventoryEnabled()),
                SETTING_REPORT_FINANCE, String.valueOf(request.isFinanceEnabled())
        ));
        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    private boolean getBoolSetting(String key, boolean defaultValue) {
        try {
            String val = tenantSettingService.getSettingValue(key, String.valueOf(defaultValue));
            return "true".equalsIgnoreCase(val);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String getStrSetting(String key, String defaultValue) {
        try {
            return tenantSettingService.getSettingValue(key, defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ======================= Request DTOs =======================

    @Data
    public static class SaveSettingsRequest {
        private boolean enabled;
        private String botToken;
        private String botUsername;
    }

    @Data
    public static class SendMessageRequest {
        private Long userId;
        @NotBlank
        private String title;
        @NotBlank
        private String message;
    }

    @Data
    public static class BroadcastRequest {
        @NotBlank
        private String title;
        @NotBlank
        private String message;
    }

    @Data
    public static class DailyReportSettingsRequest {
        private boolean enabled;
        private String time;
        private boolean salesEnabled;
        private boolean inventoryEnabled;
        private boolean financeEnabled;
    }
}
