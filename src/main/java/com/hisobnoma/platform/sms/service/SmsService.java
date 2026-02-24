package com.hisobnoma.platform.sms.service;

import com.hisobnoma.platform.admin.entity.SystemSetting;
import com.hisobnoma.platform.admin.repository.SystemSettingRepository;
import com.hisobnoma.platform.sms.config.SmsProperties;
import com.hisobnoma.platform.sms.dto.SmsBulkSendRequest;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final DevSmsClient smsClient;
    private final SmsProperties smsProperties;
    private final SystemSettingRepository systemSettingRepository;
    private final SmsTemplateService templateService;

    @PostConstruct
    public void loadSettingsFromDb() {
        try {
            systemSettingRepository.findBySettingKey("sms.enabled")
                    .map(SystemSetting::getBooleanValue)
                    .ifPresent(smsProperties::setEnabled);

            systemSettingRepository.findBySettingKey("sms.api_token")
                    .map(SystemSetting::getEffectiveValue)
                    .filter(v -> v != null && !v.isBlank())
                    .ifPresent(smsProperties::setApiToken);

            systemSettingRepository.findBySettingKey("sms.sender_id")
                    .map(SystemSetting::getEffectiveValue)
                    .filter(v -> v != null && !v.isBlank())
                    .ifPresent(smsProperties::setSenderId);

            systemSettingRepository.findBySettingKey("sms.base_url")
                    .map(SystemSetting::getEffectiveValue)
                    .filter(v -> v != null && !v.isBlank())
                    .ifPresent(smsProperties::setBaseUrl);

            log.info("SMS settings loaded from DB: enabled={}, configured={}",
                    smsProperties.isEnabled(), smsClient.isConfigured());
        } catch (Exception e) {
            log.warn("Could not load SMS settings from DB, using defaults: {}", e.getMessage());
        }
    }

    public Map<String, Object> sendSms(String phone, String message) {
        return smsClient.sendSms(phone, message);
    }

    public Map<String, Object> sendSms(String phone, String message, String from) {
        return smsClient.sendSms(phone, message, from);
    }

    @Async
    public void sendSmsAsync(String phone, String message) {
        smsClient.sendSms(phone, message);
    }

    public Map<String, Object> sendBulk(Long templateId, List<SmsBulkSendRequest.Recipient> recipients, String from) {
        int sent = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        for (SmsBulkSendRequest.Recipient recipient : recipients) {
            try {
                String message = templateService.resolveTemplate(templateId, recipient.getVariables());
                Map<String, Object> result = smsClient.sendSms(recipient.getPhone(), message, from);
                if (Boolean.TRUE.equals(result.get("success"))) {
                    sent++;
                } else {
                    failed++;
                    errors.add(recipient.getPhone() + ": " + result.getOrDefault("error", "noma'lum xato"));
                }
            } catch (Exception e) {
                failed++;
                errors.add(recipient.getPhone() + ": " + e.getMessage());
            }
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("total", recipients.size());
        summary.put("sent", sent);
        summary.put("failed", failed);
        if (!errors.isEmpty()) {
            summary.put("errors", errors);
        }
        log.info("Bulk SMS: total={}, sent={}, failed={}", recipients.size(), sent, failed);
        return summary;
    }

    public Map<String, Object> getHistory(int limit, int offset, String status) {
        return smsClient.getHistory(limit, offset, status);
    }

    public Map<String, Object> getBalance() {
        return smsClient.getBalance();
    }

    public Map<String, Object> getStatus(Long smsId, String requestId) {
        return smsClient.getStatus(smsId, requestId);
    }

    @Transactional
    public void updateSettings(boolean enabled, String apiToken, String senderId) {
        smsProperties.setEnabled(enabled);
        persistSetting("sms.enabled", String.valueOf(enabled));

        if (apiToken != null && !apiToken.isBlank() && !apiToken.contains("****")) {
            smsProperties.setApiToken(apiToken);
            persistSetting("sms.api_token", apiToken);
        }
        if (senderId != null && !senderId.isBlank()) {
            smsProperties.setSenderId(senderId);
            persistSetting("sms.sender_id", senderId);
        }
    }

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

    private void persistSetting(String key, String value) {
        systemSettingRepository.findBySettingKey(key).ifPresent(setting -> {
            setting.setSettingValue(value);
            systemSettingRepository.save(setting);
        });
    }
}
