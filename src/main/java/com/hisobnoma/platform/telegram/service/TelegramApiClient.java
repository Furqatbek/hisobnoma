package com.hisobnoma.platform.telegram.service;

import com.hisobnoma.platform.telegram.config.TelegramProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight HTTP client for Telegram Bot API.
 * Reads bot token from TelegramProperties at runtime (supports dynamic config).
 */
@Slf4j
@Component
public class TelegramApiClient {

    private static final String BASE_URL = "https://api.telegram.org/bot";

    private final RestTemplate restTemplate;
    private final TelegramProperties properties;

    public TelegramApiClient(TelegramProperties properties) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
    }

    private String getApiUrl() {
        return BASE_URL + properties.getBotToken();
    }

    public boolean isConfigured() {
        return properties.isEnabled()
                && properties.getBotToken() != null
                && !properties.getBotToken().isBlank();
    }

    /**
     * Send a text message to a chat.
     */
    public void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, "HTML");
    }

    /**
     * Send a text message with specified parse mode.
     */
    public void sendMessage(Long chatId, String text, String parseMode) {
        if (!isConfigured()) return;
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", parseMode);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            restTemplate.postForEntity(getApiUrl() + "/sendMessage", request, String.class);
            log.debug("Telegram message sent to chatId={}", chatId);
        } catch (Exception e) {
            log.error("Failed to send Telegram message to chatId={}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Get bot info to validate token.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMe() {
        if (!isConfigured()) return null;
        try {
            return restTemplate.getForObject(getApiUrl() + "/getMe", Map.class);
        } catch (Exception e) {
            log.error("Failed to call getMe: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get pending updates (for polling mode).
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getUpdates(Long offset) {
        if (!isConfigured()) return null;
        try {
            String url = getApiUrl() + "/getUpdates?timeout=30";
            if (offset != null) {
                url += "&offset=" + offset;
            }
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("Failed to get updates: {}", e.getMessage());
            return null;
        }
    }
}
