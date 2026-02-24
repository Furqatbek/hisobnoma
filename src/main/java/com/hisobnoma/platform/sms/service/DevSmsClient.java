package com.hisobnoma.platform.sms.service;

import com.hisobnoma.platform.sms.config.SmsProperties;
import com.hisobnoma.platform.sms.util.PhoneUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class DevSmsClient {

    private final RestTemplate restTemplate;
    private final SmsProperties properties;

    public DevSmsClient(SmsProperties properties) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
    }

    public boolean isConfigured() {
        return properties.isEnabled()
                && properties.getApiToken() != null
                && !properties.getApiToken().isBlank();
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(properties.getApiToken());
        return headers;
    }

    /**
     * Send an SMS message.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> sendSms(String phone, String message) {
        return sendSms(phone, message, properties.getSenderId());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> sendSms(String phone, String message, String from) {
        if (!isConfigured()) {
            log.warn("SMS is not configured, skipping send to {}", phone);
            return Map.of("success", false, "error", "SMS not configured");
        }
        try {
            String normalizedPhone = PhoneUtils.normalize(phone);
            log.debug("Phone normalized: {} -> {}", phone, normalizedPhone);

            Map<String, Object> body = new HashMap<>();
            body.put("phone", normalizedPhone);
            body.put("message", message);
            body.put("from", from != null ? from : properties.getSenderId());

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, createHeaders());
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    properties.getBaseUrl() + "/send_sms.php", request, Map.class);

            log.info("SMS sent to {}: status={}", phone, response.getStatusCode());
            return response.getBody() != null ? response.getBody() : Map.of("success", true);
        } catch (Exception e) {
            log.error("Failed to send SMS to {}: {}", phone, e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Get SMS delivery history.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getHistory(int limit, int offset, String status) {
        if (!isConfigured()) return Map.of("success", false, "error", "SMS not configured");
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(properties.getBaseUrl() + "/get_history.php")
                    .queryParam("limit", limit)
                    .queryParam("offset", offset);
            if (status != null && !status.isBlank()) {
                builder.queryParam("status", status);
            }

            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, request, Map.class);

            return response.getBody() != null ? response.getBody() : Map.of("success", true);
        } catch (Exception e) {
            log.error("Failed to get SMS history: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Get current account balance.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getBalance() {
        if (!isConfigured()) return Map.of("success", false, "error", "SMS not configured");
        try {
            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.getBaseUrl() + "/get_balance.php", HttpMethod.GET, request, Map.class);

            return response.getBody() != null ? response.getBody() : Map.of("success", true);
        } catch (Exception e) {
            log.error("Failed to get SMS balance: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }

    /**
     * Get status of a specific SMS by sms_id or request_id.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getStatus(Long smsId, String requestId) {
        if (!isConfigured()) return Map.of("success", false, "error", "SMS not configured");
        try {
            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(properties.getBaseUrl() + "/get_status.php");
            if (smsId != null) {
                builder.queryParam("sms_id", smsId);
            } else if (requestId != null) {
                builder.queryParam("request_id", requestId);
            }

            HttpEntity<Void> request = new HttpEntity<>(createHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    builder.toUriString(), HttpMethod.GET, request, Map.class);

            return response.getBody() != null ? response.getBody() : Map.of("success", true);
        } catch (Exception e) {
            log.error("Failed to get SMS status: {}", e.getMessage());
            return Map.of("success", false, "error", e.getMessage());
        }
    }
}
