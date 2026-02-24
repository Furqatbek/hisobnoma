package com.hisobnoma.platform.sms.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.sms.dto.SmsSendRequest;
import com.hisobnoma.platform.sms.dto.SmsSettingsRequest;
import com.hisobnoma.platform.sms.service.SmsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;

    /**
     * Send an SMS message.
     */
    @PostMapping("/send")
    @PreAuthorize("hasAnyAuthority('SMS_SEND', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendSms(
            @Valid @RequestBody SmsSendRequest request) {
        Map<String, Object> result = smsService.sendSms(
                request.getPhone(), request.getMessage(), request.getFrom());
        return ResponseEntity.ok(ApiResponse.success(result, "SMS yuborildi"));
    }

    /**
     * Get SMS delivery history.
     */
    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('SMS_VIEW', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String status) {
        Map<String, Object> result = smsService.getHistory(limit, offset, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get SMS account balance.
     */
    @GetMapping("/balance")
    @PreAuthorize("hasAnyAuthority('SMS_VIEW', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance() {
        Map<String, Object> result = smsService.getBalance();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get status of a specific SMS.
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('SMS_VIEW', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(
            @RequestParam(required = false) Long smsId,
            @RequestParam(required = false) String requestId) {
        Map<String, Object> result = smsService.getStatus(smsId, requestId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * Get current SMS settings.
     */
    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        Map<String, Object> settings = smsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    /**
     * Update SMS settings.
     */
    @PostMapping("/settings")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveSettings(
            @RequestBody SmsSettingsRequest request) {
        smsService.updateSettings(request.isEnabled(), request.getApiToken(), request.getSenderId());

        // Verify configuration by fetching balance
        Map<String, Object> balance = smsService.getBalance();
        boolean valid = balance.containsKey("success") && Boolean.TRUE.equals(balance.get("success"));

        Map<String, Object> result = Map.of(
                "saved", true,
                "valid", valid,
                "settings", smsService.getSettings()
        );
        return ResponseEntity.ok(ApiResponse.success(result, "SMS sozlamalari saqlandi"));
    }
}
