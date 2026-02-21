package com.hisobnoma.platform.telegram.controller;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Admin controller for Telegram bot management.
 * Always registered (no @ConditionalOnProperty) so the admin page works
 * even when Telegram is disabled — it just shows "disabled" status.
 *
 * Telegram-dependent beans (TelegramApiClient, TelegramNotificationService)
 * are injected optionally via @Autowired(required = false).
 */
@RestController
@RequestMapping("/api/v1/telegram/admin")
@RequiredArgsConstructor
public class TelegramAdminController {

    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;
    private final TelegramProperties properties;

    @Autowired(required = false)
    private TelegramApiClient telegramApiClient;

    @Autowired(required = false)
    private TelegramNotificationService notificationService;

    /**
     * Get bot info and statistics. Works even when Telegram is disabled.
     */
    @GetMapping("/info")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, Object>> getBotInfo() {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        Map<String, Object> info = new HashMap<>();
        info.put("enabled", properties.isEnabled());
        info.put("botUsername", properties.getBotUsername());

        if (!properties.isEnabled() || telegramApiClient == null) {
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
     * Get all users with Telegram connected for this tenant.
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
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
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, String>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        if (notificationService == null) {
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
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, Object>> broadcastMessage(@Valid @RequestBody BroadcastRequest request) {
        if (notificationService == null) {
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
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, String>> adminUnlinkUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getTelegramChatId() != null) {
            Long chatId = user.getTelegramChatId();
            user.setTelegramChatId(null);
            user.setTelegramLinkedAt(null);
            userRepository.save(user);
            if (telegramApiClient != null) {
                telegramApiClient.sendMessage(chatId, "Akkauntingiz administrator tomonidan uzildi.");
            }
        }
        return ResponseEntity.ok(Map.of("status", "unlinked"));
    }

    // ======================= Request DTOs =======================

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
}
