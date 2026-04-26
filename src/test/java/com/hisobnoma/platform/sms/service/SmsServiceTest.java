package com.hisobnoma.platform.sms.service;

import com.hisobnoma.platform.admin.entity.SystemSetting;
import com.hisobnoma.platform.admin.repository.SystemSettingRepository;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.sms.config.SmsProperties;
import com.hisobnoma.platform.sms.dto.SmsBulkSendRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsServiceTest {

    @Mock
    private DevSmsClient smsClient;

    @Mock
    private SmsProperties smsProperties;

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private SmsTemplateService templateService;

    @InjectMocks
    private SmsService smsService;

    private static final String PHONE = "+998901234567";
    private static final String MESSAGE = "Test SMS message";

    @BeforeEach
    void setUp() {
    }

    // ====== sendSms (2 args) ======

    @Test
    void sendSms_sufficientBalance_sendsSuccessfully() {
        // Given
        when(smsClient.getBalanceAmount()).thenReturn(1000.0);
        when(smsClient.sendSms(PHONE, MESSAGE)).thenReturn(Map.of("success", true));

        // When
        Map<String, Object> result = smsService.sendSms(PHONE, MESSAGE);

        // Then
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        verify(smsClient).sendSms(PHONE, MESSAGE);
    }

    @Test
    void sendSms_insufficientBalance_throwsBusinessException() {
        // Given
        when(smsClient.getBalanceAmount()).thenReturn(100.0);

        // When/Then
        assertThrows(BusinessException.class, () -> smsService.sendSms(PHONE, MESSAGE));
        verify(smsClient, never()).sendSms(any(), any());
    }

    @Test
    void sendSms_negativeBalance_allowsSend() {
        // Given - negative balance means check failed, allow send
        when(smsClient.getBalanceAmount()).thenReturn(-1.0);
        when(smsClient.sendSms(PHONE, MESSAGE)).thenReturn(Map.of("success", true));

        // When
        Map<String, Object> result = smsService.sendSms(PHONE, MESSAGE);

        // Then
        assertNotNull(result);
        verify(smsClient).sendSms(PHONE, MESSAGE);
    }

    // ====== sendSms (3 args) ======

    @Test
    void sendSms_withFrom_sendsWithCustomSender() {
        // Given
        when(smsClient.getBalanceAmount()).thenReturn(1000.0);
        when(smsClient.sendSms(PHONE, MESSAGE, "CUSTOM")).thenReturn(Map.of("success", true));

        // When
        Map<String, Object> result = smsService.sendSms(PHONE, MESSAGE, "CUSTOM");

        // Then
        assertNotNull(result);
        verify(smsClient).sendSms(PHONE, MESSAGE, "CUSTOM");
    }

    // ====== sendSmsAsync ======

    @Test
    void sendSmsAsync_sufficientBalance_sends() {
        // Given
        when(smsClient.getBalanceAmount()).thenReturn(1000.0);

        // When
        smsService.sendSmsAsync(PHONE, MESSAGE);

        // Then
        verify(smsClient).sendSms(PHONE, MESSAGE);
    }

    @Test
    void sendSmsAsync_insufficientBalance_throwsBusinessException() {
        // Given
        when(smsClient.getBalanceAmount()).thenReturn(100.0);

        // When/Then
        assertThrows(BusinessException.class, () -> smsService.sendSmsAsync(PHONE, MESSAGE));
    }

    // ====== sendBulk ======

    @Test
    void sendBulk_allSucceed_returnsSummary() {
        // Given
        SmsBulkSendRequest.Recipient r1 = new SmsBulkSendRequest.Recipient();
        r1.setPhone("+998901111111");
        r1.setVariables(Map.of("name", "User1"));

        SmsBulkSendRequest.Recipient r2 = new SmsBulkSendRequest.Recipient();
        r2.setPhone("+998902222222");
        r2.setVariables(Map.of("name", "User2"));

        when(smsClient.getBalanceAmount()).thenReturn(10000.0);
        when(templateService.resolveTemplate(eq(1L), any())).thenReturn("Hello User");
        when(smsClient.sendSms(any(), eq("Hello User"), eq("4546")))
                .thenReturn(Map.of("success", true));

        // When
        Map<String, Object> result = smsService.sendBulk(1L, List.of(r1, r2), "4546");

        // Then
        assertNotNull(result);
        assertEquals(2, result.get("total"));
        assertEquals(2, result.get("sent"));
        assertEquals(0, result.get("failed"));
    }

    @Test
    void sendBulk_someFail_returnsSummaryWithErrors() {
        // Given
        SmsBulkSendRequest.Recipient r1 = new SmsBulkSendRequest.Recipient();
        r1.setPhone("+998901111111");
        r1.setVariables(Map.of("name", "User1"));

        SmsBulkSendRequest.Recipient r2 = new SmsBulkSendRequest.Recipient();
        r2.setPhone("+998902222222");
        r2.setVariables(Map.of("name", "User2"));

        when(smsClient.getBalanceAmount()).thenReturn(10000.0);
        when(templateService.resolveTemplate(eq(1L), any())).thenReturn("Hello User");
        when(smsClient.sendSms("+998901111111", "Hello User", "4546"))
                .thenReturn(Map.of("success", true));
        when(smsClient.sendSms("+998902222222", "Hello User", "4546"))
                .thenReturn(Map.of("success", false, "error", "Invalid phone"));

        // When
        Map<String, Object> result = smsService.sendBulk(1L, List.of(r1, r2), "4546");

        // Then
        assertNotNull(result);
        assertEquals(2, result.get("total"));
        assertEquals(1, result.get("sent"));
        assertEquals(1, result.get("failed"));
        assertNotNull(result.get("errors"));
    }

    @Test
    void sendBulk_insufficientBalance_throwsBusinessException() {
        // Given
        SmsBulkSendRequest.Recipient r1 = new SmsBulkSendRequest.Recipient();
        r1.setPhone("+998901111111");
        r1.setVariables(Map.of("name", "User1"));

        when(smsClient.getBalanceAmount()).thenReturn(100.0);

        // When/Then
        assertThrows(BusinessException.class, () -> smsService.sendBulk(1L, List.of(r1), "4546"));
    }

    @Test
    void sendBulk_templateResolveThrows_countsAsFailed() {
        // Given
        SmsBulkSendRequest.Recipient r1 = new SmsBulkSendRequest.Recipient();
        r1.setPhone("+998901111111");
        r1.setVariables(Map.of("name", "User1"));

        when(smsClient.getBalanceAmount()).thenReturn(10000.0);
        when(templateService.resolveTemplate(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Template not found"));

        // When
        Map<String, Object> result = smsService.sendBulk(1L, List.of(r1), "4546");

        // Then
        assertEquals(1, result.get("total"));
        assertEquals(0, result.get("sent"));
        assertEquals(1, result.get("failed"));
    }

    // ====== getHistory ======

    @Test
    void getHistory_delegatesToClient() {
        // Given
        Map<String, Object> history = Map.of("success", true, "data", List.of());
        when(smsClient.getHistory(10, 0, "delivered")).thenReturn(history);

        // When
        Map<String, Object> result = smsService.getHistory(10, 0, "delivered");

        // Then
        assertNotNull(result);
        assertEquals(true, result.get("success"));
        verify(smsClient).getHistory(10, 0, "delivered");
    }

    // ====== getBalance ======

    @Test
    void getBalance_delegatesToClient() {
        // Given
        Map<String, Object> balance = Map.of("success", true, "balance", 5000);
        when(smsClient.getBalance()).thenReturn(balance);

        // When
        Map<String, Object> result = smsService.getBalance();

        // Then
        assertNotNull(result);
        verify(smsClient).getBalance();
    }

    // ====== getStatus ======

    @Test
    void getStatus_delegatesToClient() {
        // Given
        Map<String, Object> status = Map.of("success", true, "status", "delivered");
        when(smsClient.getStatus(123L, null)).thenReturn(status);

        // When
        Map<String, Object> result = smsService.getStatus(123L, null);

        // Then
        assertNotNull(result);
        verify(smsClient).getStatus(123L, null);
    }

    // ====== updateSettings ======

    @Test
    void updateSettings_updatesPropertiesAndPersists() {
        // Given
        SystemSetting enabledSetting = mock(SystemSetting.class);
        when(systemSettingRepository.findBySettingKey("sms.enabled")).thenReturn(Optional.of(enabledSetting));
        when(systemSettingRepository.findBySettingKey("sms.api_token")).thenReturn(Optional.empty());
        when(systemSettingRepository.findBySettingKey("sms.sender_id")).thenReturn(Optional.empty());

        // When
        smsService.updateSettings(true, "new-api-token-12345", "SENDER");

        // Then
        verify(smsProperties).setEnabled(true);
        verify(smsProperties).setApiToken("new-api-token-12345");
        verify(smsProperties).setSenderId("SENDER");
        verify(enabledSetting).setSettingValue("true");
        verify(systemSettingRepository).save(enabledSetting);
    }

    @Test
    void updateSettings_maskedToken_doesNotUpdateApiToken() {
        // Given
        when(systemSettingRepository.findBySettingKey("sms.enabled")).thenReturn(Optional.empty());

        // When
        smsService.updateSettings(false, "abcd****efgh", "SENDER");

        // Then
        verify(smsProperties).setEnabled(false);
        verify(smsProperties, never()).setApiToken(any());
        verify(smsProperties).setSenderId("SENDER");
    }

    @Test
    void updateSettings_blankToken_doesNotUpdateApiToken() {
        // Given
        when(systemSettingRepository.findBySettingKey("sms.enabled")).thenReturn(Optional.empty());

        // When
        smsService.updateSettings(false, "  ", null);

        // Then
        verify(smsProperties, never()).setApiToken(any());
        verify(smsProperties, never()).setSenderId(any());
    }

    // ====== getSettings ======

    @Test
    void getSettings_returnsMaskedSettings() {
        // Given
        when(smsProperties.isEnabled()).thenReturn(true);
        when(smsProperties.getApiToken()).thenReturn("abcdefghijklmnop");
        when(smsProperties.getSenderId()).thenReturn("4546");
        when(smsClient.isConfigured()).thenReturn(true);

        // When
        Map<String, Object> result = smsService.getSettings();

        // Then
        assertNotNull(result);
        assertEquals(true, result.get("enabled"));
        assertEquals("abcd****mnop", result.get("apiToken"));
        assertEquals("4546", result.get("senderId"));
        assertEquals(true, result.get("configured"));
    }

    @Test
    void getSettings_shortToken_returnsEmptyMask() {
        // Given
        when(smsProperties.isEnabled()).thenReturn(false);
        when(smsProperties.getApiToken()).thenReturn("short");
        when(smsProperties.getSenderId()).thenReturn(null);
        when(smsClient.isConfigured()).thenReturn(false);

        // When
        Map<String, Object> result = smsService.getSettings();

        // Then
        assertNotNull(result);
        assertEquals(false, result.get("enabled"));
        assertEquals("", result.get("apiToken"));
        assertEquals("4546", result.get("senderId"));
    }
}
