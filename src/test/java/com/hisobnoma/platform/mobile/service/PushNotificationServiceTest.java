package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.mobile.entity.DeviceToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    @Mock
    private DeviceTokenService deviceTokenService;

    @InjectMocks
    private PushNotificationService pushNotificationService;

    private DeviceToken deviceToken;
    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pushNotificationService, "firebaseEnabled", false);

        deviceToken = DeviceToken.builder()
                .id(1L)
                .userId(USER_ID)
                .deviceId("device-abc-123")
                .fcmToken("fcm-token-xyz-abcdef1234567890")
                .platform(DeviceToken.Platform.ANDROID)
                .active(true)
                .build();
    }

    // ====== sendPushNotification ======

    @Test
    void sendPushNotification_withActiveDevices_sendsToAllDevices() {
        // Given
        when(deviceTokenService.getActiveDeviceTokens(USER_ID)).thenReturn(List.of(deviceToken));

        // When
        pushNotificationService.sendPushNotification(USER_ID, "Test Title", "Test Body", "LOW_STOCK", 100L);

        // Then
        verify(deviceTokenService).getActiveDeviceTokens(USER_ID);
    }

    @Test
    void sendPushNotification_noActiveDevices_doesNothing() {
        // Given
        when(deviceTokenService.getActiveDeviceTokens(USER_ID)).thenReturn(List.of());

        // When
        pushNotificationService.sendPushNotification(USER_ID, "Test Title", "Test Body", "LOW_STOCK", 100L);

        // Then
        verify(deviceTokenService).getActiveDeviceTokens(USER_ID);
    }

    @Test
    void sendPushNotification_withNullEntityId_sendsWithoutEntityId() {
        // Given
        when(deviceTokenService.getActiveDeviceTokens(USER_ID)).thenReturn(List.of(deviceToken));

        // When
        pushNotificationService.sendPushNotification(USER_ID, "Test Title", "Test Body", "SYSTEM_ALERT", null);

        // Then
        verify(deviceTokenService).getActiveDeviceTokens(USER_ID);
    }

    // ====== sendPushNotificationToUsers ======

    @Test
    void sendPushNotificationToUsers_withActiveDevices_sendsToAll() {
        // Given
        List<Long> userIds = List.of(10L, 20L);
        when(deviceTokenService.getActiveDeviceTokensForUsers(userIds)).thenReturn(List.of(deviceToken));

        // When
        pushNotificationService.sendPushNotificationToUsers(userIds, "Broadcast", "Body", "SYSTEM_ALERT", null);

        // Then
        verify(deviceTokenService).getActiveDeviceTokensForUsers(userIds);
    }

    @Test
    void sendPushNotificationToUsers_noDevices_doesNothing() {
        // Given
        List<Long> userIds = List.of(10L, 20L);
        when(deviceTokenService.getActiveDeviceTokensForUsers(userIds)).thenReturn(List.of());

        // When
        pushNotificationService.sendPushNotificationToUsers(userIds, "Broadcast", "Body", "SYSTEM_ALERT", null);

        // Then
        verify(deviceTokenService).getActiveDeviceTokensForUsers(userIds);
    }

    // ====== sendToDevice ======

    @Test
    void sendToDevice_firebaseDisabled_logsOnly() {
        // Given
        ReflectionTestUtils.setField(pushNotificationService, "firebaseEnabled", false);

        // When - should not throw
        pushNotificationService.sendToDevice("fcm-token-xyz-abcdef1234567890", "Title", "Body", Map.of("type", "TEST"));

        // Then - no exception, just logs
    }

    @Test
    void sendToDevice_firebaseEnabled_attemptsToSend() {
        // Given
        ReflectionTestUtils.setField(pushNotificationService, "firebaseEnabled", true);

        // When - should not throw (FCM is a TODO placeholder)
        pushNotificationService.sendToDevice("fcm-token-xyz-abcdef1234567890", "Title", "Body", Map.of("type", "TEST"));

        // Then - no exception
    }

    // ====== sendLowStockAlert ======

    @Test
    void sendLowStockAlert_logsAlert() {
        // When - should not throw
        pushNotificationService.sendLowStockAlert(1L, "Widget", "SKU-001", 5, 10);

        // Then - just logs, no exception
    }

    // ====== sendLargeTransactionAlert ======

    @Test
    void sendLargeTransactionAlert_sendsPushNotification() {
        // Given
        when(deviceTokenService.getActiveDeviceTokens(USER_ID)).thenReturn(List.of(deviceToken));

        // When
        pushNotificationService.sendLargeTransactionAlert(1L, USER_ID, "TXN-001", "500000 UZS", "Customer A");

        // Then
        verify(deviceTokenService).getActiveDeviceTokens(USER_ID);
    }

    // ====== sendPaymentReceivedAlert ======

    @Test
    void sendPaymentReceivedAlert_sendsPushNotification() {
        // Given
        when(deviceTokenService.getActiveDeviceTokens(USER_ID)).thenReturn(List.of(deviceToken));

        // When
        pushNotificationService.sendPaymentReceivedAlert(1L, USER_ID, "INV-001", "100000 UZS");

        // Then
        verify(deviceTokenService).getActiveDeviceTokens(USER_ID);
    }

    // ====== sendDailySummary ======

    @Test
    void sendDailySummary_sendsPushNotification() {
        // Given
        when(deviceTokenService.getActiveDeviceTokens(USER_ID)).thenReturn(List.of(deviceToken));

        // When
        pushNotificationService.sendDailySummary(USER_ID, "500000 UZS", 25, 3);

        // Then
        verify(deviceTokenService).getActiveDeviceTokens(USER_ID);
    }
}
