package com.hisobnoma.platform.telegram.service;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.mobile.entity.MobileAlert;
import com.hisobnoma.platform.mobile.repository.AlertPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramNotificationServiceTest {

    @Mock
    private TelegramApiClient telegramApi;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AlertPreferenceRepository preferenceRepository;

    @InjectMocks
    private TelegramNotificationService telegramNotificationService;

    private User user;
    private static final Long USER_ID = 10L;
    private static final Long TENANT_ID = 1L;
    private static final Long CHAT_ID = 123456789L;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .passwordHash("hash")
                .telegramChatId(CHAT_ID)
                .telegramLinkedAt(Instant.now())
                .build();
        user.setTenantId(TENANT_ID);
    }

    // ====== sendAlert ======

    @Test
    void sendAlert_telegramEnabled_sendsMessage() {
        // Given
        when(preferenceRepository.existsByUserIdAndTenantIdAndAlertTypeAndTelegramEnabledTrue(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When
        telegramNotificationService.sendAlert(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK,
                "Low Stock", "Widget is low on stock");

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("Low Stock"));
    }

    @Test
    void sendAlert_telegramDisabled_doesNotSend() {
        // Given
        when(preferenceRepository.existsByUserIdAndTenantIdAndAlertTypeAndTelegramEnabledTrue(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK)).thenReturn(false);

        // When
        telegramNotificationService.sendAlert(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK,
                "Low Stock", "Widget is low on stock");

        // Then
        verify(userRepository, never()).findById(any());
        verify(telegramApi, never()).sendMessage(any(), any());
    }

    @Test
    void sendAlert_userNotFound_doesNotSend() {
        // Given
        when(preferenceRepository.existsByUserIdAndTenantIdAndAlertTypeAndTelegramEnabledTrue(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When
        telegramNotificationService.sendAlert(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK,
                "Low Stock", "Widget is low on stock");

        // Then
        verify(telegramApi, never()).sendMessage(any(), any());
    }

    @Test
    void sendAlert_userWithNoChatId_doesNotSend() {
        // Given
        User userWithoutChat = User.builder()
                .id(USER_ID)
                .username("testuser")
                .passwordHash("hash")
                .telegramChatId(null)
                .build();

        when(preferenceRepository.existsByUserIdAndTenantIdAndAlertTypeAndTelegramEnabledTrue(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(userWithoutChat));

        // When
        telegramNotificationService.sendAlert(
                USER_ID, TENANT_ID, MobileAlert.AlertType.LOW_STOCK,
                "Low Stock", "Widget is low on stock");

        // Then
        verify(telegramApi, never()).sendMessage(any(), any());
    }

    // ====== sendBroadcastAlert ======

    @Test
    void sendBroadcastAlert_sendsToAllEnabledUsers() {
        // Given
        User user2 = User.builder()
                .id(20L)
                .username("testuser2")
                .passwordHash("hash")
                .telegramChatId(999L)
                .build();
        user2.setTenantId(TENANT_ID);

        when(preferenceRepository.findUserIdsWithTelegramEnabledForType(TENANT_ID, MobileAlert.AlertType.SYSTEM_ALERT))
                .thenReturn(List.of(USER_ID, 20L));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findById(20L)).thenReturn(Optional.of(user2));

        // When
        telegramNotificationService.sendBroadcastAlert(
                TENANT_ID, MobileAlert.AlertType.SYSTEM_ALERT, "System Alert", "Maintenance window");

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("System Alert"));
        verify(telegramApi).sendMessage(eq(999L), contains("System Alert"));
    }

    @Test
    void sendBroadcastAlert_skipsUserFromAnotherTenant() {
        User foreignUser = User.builder()
                .id(21L)
                .username("foreign")
                .passwordHash("hash")
                .telegramChatId(777L)
                .build();
        foreignUser.setTenantId(99L); // different tenant — must never receive this alert

        when(preferenceRepository.findUserIdsWithTelegramEnabledForType(TENANT_ID, MobileAlert.AlertType.SYSTEM_ALERT))
                .thenReturn(List.of(21L));
        when(userRepository.findById(21L)).thenReturn(Optional.of(foreignUser));

        telegramNotificationService.sendBroadcastAlert(
                TENANT_ID, MobileAlert.AlertType.SYSTEM_ALERT, "System Alert", "Maintenance window");

        verify(telegramApi, never()).sendMessage(anyLong(), anyString());
    }

    @Test
    void sendBroadcastAlert_noEnabledUsers_doesNotSend() {
        // Given
        when(preferenceRepository.findUserIdsWithTelegramEnabledForType(TENANT_ID, MobileAlert.AlertType.SYSTEM_ALERT))
                .thenReturn(List.of());

        // When
        telegramNotificationService.sendBroadcastAlert(
                TENANT_ID, MobileAlert.AlertType.SYSTEM_ALERT, "System Alert", "Maintenance");

        // Then
        verify(telegramApi, never()).sendMessage(any(), any());
    }

    // ====== sendDirectMessage ======

    @Test
    void sendDirectMessage_sendsFormattedMessage() {
        // When
        telegramNotificationService.sendDirectMessage(CHAT_ID, "Test Title", "Test message body");

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("Test Title"));
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("Test message body"));
    }

    // ====== sendLowStockAlert ======

    @Test
    void sendLowStockAlert_broadcastsToTenant() {
        // Given
        when(preferenceRepository.findUserIdsWithTelegramEnabledForType(TENANT_ID, MobileAlert.AlertType.LOW_STOCK))
                .thenReturn(List.of(USER_ID));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When
        telegramNotificationService.sendLowStockAlert(TENANT_ID, "Widget Pro", "SKU-001", 5, 10);

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), any());
    }

    // ====== sendPaymentAlert ======

    @Test
    void sendPaymentAlert_sendsToUser() {
        // Given
        when(preferenceRepository.existsByUserIdAndTenantIdAndAlertTypeAndTelegramEnabledTrue(
                USER_ID, TENANT_ID, MobileAlert.AlertType.PAYMENT_RECEIVED)).thenReturn(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When
        telegramNotificationService.sendPaymentAlert(USER_ID, TENANT_ID, "INV-001", "500000 UZS");

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("INV-001"));
    }

    // ====== sendCreditLimitAlert ======

    @Test
    void sendCreditLimitAlert_sendsToAllLinkedUsers() {
        // Given
        when(userRepository.findUsersWithTelegramByTenantId(TENANT_ID)).thenReturn(List.of(user));

        // When
        telegramNotificationService.sendCreditLimitAlert(
                TENANT_ID, "Customer A", "1500000 UZS", "1000000 UZS");

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("Customer A"));
    }

    @Test
    void sendCreditLimitAlert_userWithNoChatId_skipsUser() {
        // Given
        User userWithoutChat = User.builder()
                .id(USER_ID)
                .username("testuser")
                .passwordHash("hash")
                .telegramChatId(null)
                .build();

        when(userRepository.findUsersWithTelegramByTenantId(TENANT_ID)).thenReturn(List.of(userWithoutChat));

        // When
        telegramNotificationService.sendCreditLimitAlert(
                TENANT_ID, "Customer A", "1500000 UZS", "1000000 UZS");

        // Then
        verify(telegramApi, never()).sendMessage(any(), any());
    }
}
