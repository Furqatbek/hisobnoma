package com.hisobnoma.platform.telegram.controller;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.telegram.config.TelegramProperties;
import com.hisobnoma.platform.telegram.service.TelegramBotService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for Telegram integration endpoints.
 * Allows users to generate link codes and manage their Telegram connection.
 */
@RestController
@RequestMapping("/api/v1/telegram")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.telegram.enabled", havingValue = "true")
public class TelegramController {

    private final TelegramBotService botService;
    private final UserRepository userRepository;
    private final SecurityContextHelper securityContextHelper;
    private final TelegramProperties properties;

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
}
