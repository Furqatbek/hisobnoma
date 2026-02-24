package com.hisobnoma.platform.sms.controller;

import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.sms.dto.SmsBulkSendRequest;
import com.hisobnoma.platform.sms.dto.SmsSendRequest;
import com.hisobnoma.platform.sms.dto.SmsSettingsRequest;
import com.hisobnoma.platform.sms.dto.SmsTemplateDto;
import com.hisobnoma.platform.sms.dto.SmsTemplateRequest;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.sms.service.SmsTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sms")
@RequiredArgsConstructor
public class SmsController {

    private final SmsService smsService;
    private final SmsTemplateService templateService;

    // ── Send SMS (template-based) ──────────────────────────────────────

    @PostMapping("/send")
    @PreAuthorize("hasAnyAuthority('SMS_SEND', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendSms(
            @Valid @RequestBody SmsSendRequest request) {
        String message = templateService.resolveTemplate(request.getTemplateId(), request.getVariables());
        Map<String, Object> result = smsService.sendSms(request.getPhone(), message, request.getFrom());
        return ResponseEntity.ok(ApiResponse.success(result, "SMS yuborildi"));
    }

    @PostMapping("/send-bulk")
    @PreAuthorize("hasAnyAuthority('SMS_SEND', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendBulkSms(
            @Valid @RequestBody SmsBulkSendRequest request) {
        Map<String, Object> result = smsService.sendBulk(
                request.getTemplateId(), request.getRecipients(), request.getFrom());
        return ResponseEntity.ok(ApiResponse.success(result, "Ommaviy SMS yuborildi"));
    }

    // ── Templates CRUD ─────────────────────────────────────────────────

    @GetMapping("/templates")
    @PreAuthorize("hasAnyAuthority('SMS_SEND', 'SMS_TEMPLATES_MANAGE', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<List<SmsTemplateDto>>> getTemplates(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<SmsTemplateDto> templates = activeOnly
                ? templateService.getActiveTemplates()
                : templateService.getAllTemplates();
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/templates/{id}")
    @PreAuthorize("hasAnyAuthority('SMS_TEMPLATES_MANAGE', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<SmsTemplateDto>> getTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(templateService.getTemplate(id)));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAnyAuthority('SMS_TEMPLATES_MANAGE', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<SmsTemplateDto>> createTemplate(
            @Valid @RequestBody SmsTemplateRequest request) {
        SmsTemplateDto created = templateService.createTemplate(request);
        return ResponseEntity.ok(ApiResponse.success(created, "Shablon yaratildi"));
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAnyAuthority('SMS_TEMPLATES_MANAGE', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<SmsTemplateDto>> updateTemplate(
            @PathVariable Long id, @Valid @RequestBody SmsTemplateRequest request) {
        SmsTemplateDto updated = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Shablon yangilandi"));
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAnyAuthority('SMS_TEMPLATES_MANAGE', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Shablon o'chirildi"));
    }

    // ── History / Balance / Status ─────────────────────────────────────

    @GetMapping("/history")
    @PreAuthorize("hasAnyAuthority('SMS_VIEW', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHistory(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) String status) {
        Map<String, Object> result = smsService.getHistory(limit, offset, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAnyAuthority('SMS_VIEW', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBalance() {
        Map<String, Object> result = smsService.getBalance();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyAuthority('SMS_VIEW', 'ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus(
            @RequestParam(required = false) Long smsId,
            @RequestParam(required = false) String requestId) {
        Map<String, Object> result = smsService.getStatus(smsId, requestId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // ── Settings ───────────────────────────────────────────────────────

    @GetMapping("/settings")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        Map<String, Object> settings = smsService.getSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @PostMapping("/settings")
    @PreAuthorize("hasAuthority('ADMIN_SETTINGS_MANAGE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveSettings(
            @RequestBody SmsSettingsRequest request) {
        smsService.updateSettings(request.isEnabled(), request.getApiToken(), request.getSenderId());

        Map<String, Object> balance = smsService.getBalance();
        boolean valid = balance.containsKey("success") && Boolean.TRUE.equals(balance.get("success"));

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("saved", true);
        result.put("valid", valid);
        result.put("settings", smsService.getSettings());
        return ResponseEntity.ok(ApiResponse.success(result, "SMS sozlamalari saqlandi"));
    }
}
