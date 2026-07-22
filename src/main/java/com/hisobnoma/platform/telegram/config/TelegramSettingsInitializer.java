package com.hisobnoma.platform.telegram.config;

import com.hisobnoma.platform.admin.service.SystemSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Applies UI-configured platform bot credentials (system_settings) over the
 * application.yml/env defaults at startup, so the poller works after a restart
 * without anyone opening the admin page. Values set only via env stay as-is.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TelegramSettingsInitializer {

    private final SystemSettingService systemSettingService;
    private final TelegramProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void applyStoredSettings() {
        try {
            String enabled = systemSettingService.getSettingValue("telegram.enabled", null);
            String token = systemSettingService.getSettingValue("telegram.bot_token", null);
            String username = systemSettingService.getSettingValue("telegram.bot_username", null);

            if (enabled != null) {
                properties.setEnabled("true".equalsIgnoreCase(enabled));
            }
            if (token != null && !token.isBlank()) {
                properties.setBotToken(token);
            }
            if (username != null && !username.isBlank()) {
                properties.setBotUsername(username);
            }
            if (token != null || enabled != null) {
                log.info("Telegram bot settings loaded from system settings (enabled={})",
                        properties.isEnabled());
            }
        } catch (Exception e) {
            log.debug("No stored Telegram settings — using application.yml/env defaults");
        }
    }
}
