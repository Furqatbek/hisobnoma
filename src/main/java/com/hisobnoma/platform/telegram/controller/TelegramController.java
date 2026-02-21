package com.hisobnoma.platform.telegram.controller;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.telegram.config.TelegramProperties;
import com.hisobnoma.platform.telegram.service.TelegramApiClient;
import com.hisobnoma.platform.telegram.service.TelegramBotService;
import com.hisobnoma.platform.telegram.service.TelegramNotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for Telegram integration endpoints.
 * Includes both user-facing (link/unlink) and admin (manage, broadcast) endpoints.
 */
@RestController
@RequestMapping("/api/v1/telegram")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.telegram.enabled", havingValue = "true")
public class TelegramController {

    private final TelegramBotService botService;
    private final TelegramApiClient telegramApiClient;
    private final TelegramNotificationService notificationService;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;
    private final TelegramProperties properties;

    // ======================= User Endpoints =======================

    /**
     * Get current Telegram link status for the authenticated user.
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Long userId = securityContextHelper.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();

        boolean linked = user.getTelegramChatId() != null;
        return ResponseEntity.ok(Map.of(
                "linked", linked,
                "botUsername", properties.getBotUsername(),
                "linkedAt", linked && user.getTelegramLinkedAt() != null
                        ? user.getTelegramLinkedAt().toString() : ""
        ));
    }

    /**
     * Generate a 6-digit link code. User sends this code to the bot in Telegram.
     */
    @PostMapping("/link-code")
    public ResponseEntity<Map<String, String>> generateLinkCode() {
        Long userId = securityContextHelper.getCurrentUserId();
        String code = botService.generateLinkCode(userId);
        return ResponseEntity.ok(Map.of(
                "code", code,
                "botUsername", properties.getBotUsername(),
                "expiresInSeconds", "600"
        ));
    }

    /**
     * Unlink Telegram from the authenticated user's account.
     */
    @DeleteMapping("/unlink")
    public ResponseEntity<Map<String, String>> unlink() {
        Long userId = securityContextHelper.getCurrentUserId();
        User user = userRepository.findById(userId).orElseThrow();

        if (user.getTelegramChatId() != null) {
            user.setTelegramChatId(null);
            user.setTelegramLinkedAt(null);
            userRepository.save(user);
        }

        return ResponseEntity.ok(Map.of("status", "unlinked"));
    }

    // ======================= Admin Endpoints =======================

    /**
     * Get bot info and statistics.
     */
    @GetMapping("/admin/info")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, Object>> getBotInfo() {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        List<User> linkedUsers = userRepository.findUsersWithTelegramByTenantId(tenantId);

        Map<String, Object> botInfo = telegramApiClient.getMe();
        String botName = "";
        if (botInfo != null && botInfo.get("result") instanceof Map<?, ?> result) {
            botName = (String) result.get("first_name");
        }

        Map<String, Object> info = new HashMap<>();
        info.put("enabled", properties.isEnabled());
        info.put("botUsername", properties.getBotUsername());
        info.put("botName", botName);
        info.put("connectedUsers", linkedUsers.size());
        info.put("totalUsers", userRepository.countByTenantId(tenantId));

        return ResponseEntity.ok(info);
    }

    /**
     * Get all users with Telegram connected for this tenant.
     */
    @GetMapping("/admin/users")
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
    @PostMapping("/admin/send")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, String>> sendMessage(@Valid @RequestBody SendMessageRequest request) {
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
    @PostMapping("/admin/broadcast")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, Object>> broadcastMessage(@Valid @RequestBody BroadcastRequest request) {
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
    @DeleteMapping("/admin/users/{userId}/unlink")
    @PreAuthorize("hasAuthority('SETTINGS_MANAGE')")
    public ResponseEntity<Map<String, String>> adminUnlinkUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        if (user.getTelegramChatId() != null) {
            Long chatId = user.getTelegramChatId();
            user.setTelegramChatId(null);
            user.setTelegramLinkedAt(null);
            userRepository.save(user);
            telegramApiClient.sendMessage(chatId, "Akkauntingiz administrator tomonidan uzildi.");
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
