package com.hisobnoma.platform.sms.service;

import com.hisobnoma.platform.sms.config.SmsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final DevSmsClient smsClient;
    private final SmsProperties smsProperties;

    /**
     * Send SMS synchronously.
     */
    public Map<String, Object> sendSms(String phone, String message) {
        return smsClient.sendSms(phone, message);
    }

    public Map<String, Object> sendSms(String phone, String message, String from) {
        return smsClient.sendSms(phone, message, from);
    }

    /**
     * Send SMS asynchronously (for notifications).
     */
    @Async
    public void sendSmsAsync(String phone, String message) {
        smsClient.sendSms(phone, message);
    }

    /**
     * Get SMS history from DevSMS.
     */
    public Map<String, Object> getHistory(int limit, int offset, String status) {
        return smsClient.getHistory(limit, offset, status);
    }

    /**
     * Get account balance.
     */
    public Map<String, Object> getBalance() {
        return smsClient.getBalance();
    }

    /**
     * Get status of a specific SMS.
     */
    public Map<String, Object> getStatus(Long smsId, String requestId) {
        return smsClient.getStatus(smsId, requestId);
    }

    /**
     * Update SMS settings at runtime.
     */
    public void updateSettings(boolean enabled, String apiToken, String senderId) {
        smsProperties.setEnabled(enabled);
        if (apiToken != null && !apiToken.isBlank()) {
            smsProperties.setApiToken(apiToken);
        }
        if (senderId != null && !senderId.isBlank()) {
            smsProperties.setSenderId(senderId);
        }
    }

    /**
     * Get current settings (without exposing full token).
     */
    public Map<String, Object> getSettings() {
        String maskedToken = "";
        String token = smsProperties.getApiToken();
        if (token != null && token.length() > 8) {
            maskedToken = token.substring(0, 4) + "****" + token.substring(token.length() - 4);
        }
        Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("enabled", smsProperties.isEnabled());
        settings.put("apiToken", maskedToken);
        settings.put("senderId", smsProperties.getSenderId() != null ? smsProperties.getSenderId() : "4546");
        settings.put("configured", smsClient.isConfigured());
        return settings;
    }
}
