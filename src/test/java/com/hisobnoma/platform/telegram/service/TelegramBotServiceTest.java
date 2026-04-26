package com.hisobnoma.platform.telegram.service;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.telegram.config.TelegramProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramBotServiceTest {

    @Mock
    private TelegramApiClient telegramApi;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TelegramProperties properties;

    @InjectMocks
    private TelegramBotService telegramBotService;

    private User user;
    private static final Long CHAT_ID = 123456789L;
    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(USER_ID)
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .passwordHash("hash")
                .build();
        user.setTenantId(1L);
    }

    // ====== pollUpdates ======

    @Test
    void pollUpdates_notConfigured_doesNothing() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(false);

        // When
        telegramBotService.pollUpdates();

        // Then
        verify(telegramApi, never()).getUpdates(any());
    }

    @Test
    void pollUpdates_nullResponse_doesNothing() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(true);
        when(telegramApi.getUpdates(any())).thenReturn(null);

        // When
        telegramBotService.pollUpdates();

        // Then
        verify(telegramApi).getUpdates(any());
    }

    @Test
    void pollUpdates_notOk_doesNothing() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(true);
        when(telegramApi.getUpdates(any())).thenReturn(Map.of("ok", false));

        // When
        telegramBotService.pollUpdates();

        // Then
        verify(telegramApi).getUpdates(any());
    }

    @Test
    void pollUpdates_withStartCommand_handlesStart() {
        // Given
        Map<String, Object> update = Map.of(
                "update_id", 100,
                "message", Map.of(
                        "chat", Map.of("id", CHAT_ID),
                        "text", "/start"
                )
        );
        when(telegramApi.isConfigured()).thenReturn(true);
        when(telegramApi.getUpdates(any())).thenReturn(Map.of("ok", true, "result", List.of(update)));

        // When
        telegramBotService.pollUpdates();

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("Hisobnoma Bot"));
    }

    @Test
    void pollUpdates_withHelpCommand_handlesHelp() {
        // Given
        Map<String, Object> update = Map.of(
                "update_id", 100,
                "message", Map.of(
                        "chat", Map.of("id", CHAT_ID),
                        "text", "/help"
                )
        );
        when(telegramApi.isConfigured()).thenReturn(true);
        when(telegramApi.getUpdates(any())).thenReturn(Map.of("ok", true, "result", List.of(update)));

        // When
        telegramBotService.pollUpdates();

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("buyruqlar"));
    }

    @Test
    void pollUpdates_withUnknownCommand_sendsUnknownMessage() {
        // Given
        Map<String, Object> update = Map.of(
                "update_id", 100,
                "message", Map.of(
                        "chat", Map.of("id", CHAT_ID),
                        "text", "/unknown"
                )
        );
        when(telegramApi.isConfigured()).thenReturn(true);
        when(telegramApi.getUpdates(any())).thenReturn(Map.of("ok", true, "result", List.of(update)));

        // When
        telegramBotService.pollUpdates();

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("/help"));
    }

    // ====== handleStatus ======

    @Test
    void handleStatus_linkedUsers_showsLinkedAccounts() {
        // Given
        user.setTelegramLinkedAt(Instant.now());
        when(userRepository.findAllByTelegramChatId(CHAT_ID)).thenReturn(List.of(user));

        // When
        telegramBotService.handleStatus(CHAT_ID);

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("Ulangan akkauntlar"));
    }

    @Test
    void handleStatus_noLinkedUsers_showsNotLinked() {
        // Given
        when(userRepository.findAllByTelegramChatId(CHAT_ID)).thenReturn(List.of());

        // When
        telegramBotService.handleStatus(CHAT_ID);

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("ulanmagan"));
    }

    // ====== handleUnlink ======

    @Test
    void handleUnlink_linkedUsers_unlinksAll() {
        // Given
        user.setTelegramChatId(CHAT_ID);
        user.setTelegramLinkedAt(Instant.now());
        when(userRepository.findAllByTelegramChatId(CHAT_ID)).thenReturn(List.of(user));

        // When
        telegramBotService.handleUnlink(CHAT_ID);

        // Then
        assertNull(user.getTelegramChatId());
        assertNull(user.getTelegramLinkedAt());
        verify(userRepository).save(user);
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("uzildi"));
    }

    @Test
    void handleUnlink_noLinkedUsers_showsNotLinked() {
        // Given
        when(userRepository.findAllByTelegramChatId(CHAT_ID)).thenReturn(List.of());

        // When
        telegramBotService.handleUnlink(CHAT_ID);

        // Then
        verify(userRepository, never()).save(any());
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("ulanmagan"));
    }

    // ====== handleLinkCode ======

    @Test
    void handleLinkCode_validCode_linksUser() {
        // Given
        String code = telegramBotService.generateLinkCode(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findAllByTelegramChatId(CHAT_ID)).thenReturn(List.of(user));

        // When
        telegramBotService.handleLinkCode(CHAT_ID, code);

        // Then
        assertEquals(CHAT_ID, user.getTelegramChatId());
        assertNotNull(user.getTelegramLinkedAt());
        verify(userRepository).save(user);
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("Muvaffaqiyatli"));
    }

    @Test
    void handleLinkCode_invalidCode_sendsError() {
        // When
        telegramBotService.handleLinkCode(CHAT_ID, "000000");

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("noto'g'ri"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void handleLinkCode_userNotFound_sendsError() {
        // Given
        String code = telegramBotService.generateLinkCode(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        // When
        telegramBotService.handleLinkCode(CHAT_ID, code);

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("topilmadi"));
    }

    @Test
    void handleLinkCode_alreadyLinkedToSameChat_sendsAlreadyLinked() {
        // Given
        user.setTelegramChatId(CHAT_ID);
        String code = telegramBotService.generateLinkCode(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        // When
        telegramBotService.handleLinkCode(CHAT_ID, code);

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("allaqachon"));
        verify(userRepository, never()).save(any());
    }

    // ====== generateLinkCode ======

    @Test
    void generateLinkCode_returns6DigitCode() {
        // When
        String code = telegramBotService.generateLinkCode(USER_ID);

        // Then
        assertNotNull(code);
        assertEquals(6, code.length());
        assertTrue(code.matches("\\d{6}"));
    }

    @Test
    void generateLinkCode_removesExistingCodesForUser() {
        // Given - generate two codes for same user
        String code1 = telegramBotService.generateLinkCode(USER_ID);
        String code2 = telegramBotService.generateLinkCode(USER_ID);

        // Then - first code should be invalid now
        assertNotEquals(code1, code2);
        // Trying to use code1 should fail (code removed)
        telegramBotService.handleLinkCode(CHAT_ID, code1);
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("noto'g'ri"));
    }

    // ====== cleanupExpiredCodes ======

    @Test
    void cleanupExpiredCodes_doesNotThrow() {
        // Given - generate a code
        telegramBotService.generateLinkCode(USER_ID);

        // When/Then - should not throw
        telegramBotService.cleanupExpiredCodes();
    }
}
