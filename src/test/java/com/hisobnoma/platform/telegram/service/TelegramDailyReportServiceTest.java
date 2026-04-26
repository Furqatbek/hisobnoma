package com.hisobnoma.platform.telegram.service;

import com.hisobnoma.platform.admin.service.TenantSettingService;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import com.hisobnoma.platform.finance.repository.APInvoiceRepository;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.BankAccountRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramDailyReportServiceTest {

    @Mock
    private TelegramApiClient telegramApi;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TenantSettingService tenantSettingService;

    @Mock
    private POSTransactionRepository transactionRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ARInvoiceRepository arInvoiceRepository;

    @Mock
    private APInvoiceRepository apInvoiceRepository;

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private TelegramDailyReportService telegramDailyReportService;

    private User user;
    private Tenant tenant;
    private static final Long TENANT_ID = 1L;
    private static final Long CHAT_ID = 123456789L;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(10L)
                .username("testuser")
                .firstName("Test")
                .lastName("User")
                .passwordHash("hash")
                .telegramChatId(CHAT_ID)
                .telegramLinkedAt(Instant.now())
                .build();
        user.setTenantId(TENANT_ID);

        tenant = new Tenant();
        tenant.setId(TENANT_ID);
    }

    // ====== sendScheduledReports ======

    @Test
    void sendScheduledReports_notConfigured_doesNothing() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(false);

        // When
        telegramDailyReportService.sendScheduledReports();

        // Then
        verify(tenantRepository, never()).findAll();
    }

    @Test
    void sendScheduledReports_reportDisabled_doesNotSend() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(true);
        when(tenantRepository.findAll()).thenReturn(List.of(tenant));
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_ENABLED))
                .thenReturn("false");

        // When
        telegramDailyReportService.sendScheduledReports();

        // Then
        verify(userRepository, never()).findUsersWithTelegramByTenantId(any());
    }

    @Test
    void sendScheduledReports_shiftCloseMode_skipsFixedTime() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(true);
        when(tenantRepository.findAll()).thenReturn(List.of(tenant));
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_ENABLED))
                .thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_TRIGGER))
                .thenReturn("SHIFT_CLOSE");

        // When
        telegramDailyReportService.sendScheduledReports();

        // Then
        verify(userRepository, never()).findUsersWithTelegramByTenantId(any());
    }

    // ====== onShiftClosed ======

    @Test
    void onShiftClosed_notConfigured_doesNothing() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(false);

        // When
        telegramDailyReportService.onShiftClosed(TENANT_ID, "SHIFT-001", "Cashier", "POS-01");

        // Then
        verify(userRepository, never()).findUsersWithTelegramByTenantId(any());
    }

    @Test
    void onShiftClosed_reportDisabled_doesNotSend() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(true);
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_ENABLED))
                .thenReturn("false");

        // When
        telegramDailyReportService.onShiftClosed(TENANT_ID, "SHIFT-001", "Cashier", "POS-01");

        // Then
        verify(userRepository, never()).findUsersWithTelegramByTenantId(any());
    }

    @Test
    void onShiftClosed_fixedTimeMode_skipsShiftClose() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(true);
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_ENABLED))
                .thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_TRIGGER))
                .thenReturn("FIXED_TIME");

        // When
        telegramDailyReportService.onShiftClosed(TENANT_ID, "SHIFT-001", "Cashier", "POS-01");

        // Then
        verify(userRepository, never()).findUsersWithTelegramByTenantId(any());
    }

    @Test
    void onShiftClosed_shiftCloseMode_sendsReport() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(true);
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_ENABLED))
                .thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_TRIGGER))
                .thenReturn("SHIFT_CLOSE");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_SALES))
                .thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_INVENTORY))
                .thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_FINANCE))
                .thenReturn("true");
        when(userRepository.findUsersWithTelegramByTenantId(TENANT_ID)).thenReturn(List.of(user));
        when(transactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("500000"));
        when(transactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(25);
        when(stockRepository.countLowStockProducts(TENANT_ID)).thenReturn(3);
        when(stockRepository.countOutOfStockProducts(TENANT_ID)).thenReturn(1);
        when(stockRepository.calculateTotalInventoryValue(TENANT_ID)).thenReturn(new BigDecimal("5000000"));
        when(arInvoiceRepository.sumOutstandingBalance(eq(TENANT_ID), any())).thenReturn(new BigDecimal("200000"));
        when(apInvoiceRepository.sumOutstandingBalance(eq(TENANT_ID), any())).thenReturn(new BigDecimal("100000"));
        when(bankAccountRepository.sumTotalBalance(TENANT_ID)).thenReturn(new BigDecimal("1000000"));

        // When
        telegramDailyReportService.onShiftClosed(TENANT_ID, "SHIFT-001", "Cashier", "POS-01");

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), contains("Kunlik hisobot"));
    }

    @Test
    void onShiftClosed_bothMode_sendsReport() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(true);
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_ENABLED))
                .thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_TRIGGER))
                .thenReturn("BOTH");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_SALES))
                .thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_INVENTORY))
                .thenReturn("false");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_FINANCE))
                .thenReturn("false");
        when(userRepository.findUsersWithTelegramByTenantId(TENANT_ID)).thenReturn(List.of(user));
        when(transactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("500000"));
        when(transactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(25);

        // When
        telegramDailyReportService.onShiftClosed(TENANT_ID, "SHIFT-001", "Cashier", "POS-01");

        // Then
        verify(telegramApi).sendMessage(eq(CHAT_ID), any());
    }

    @Test
    void onShiftClosed_noUsersWithTelegram_doesNotSendMessage() {
        // Given
        when(telegramApi.isConfigured()).thenReturn(true);
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_ENABLED))
                .thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_TRIGGER))
                .thenReturn("SHIFT_CLOSE");
        when(userRepository.findUsersWithTelegramByTenantId(TENANT_ID)).thenReturn(List.of());

        // When
        telegramDailyReportService.onShiftClosed(TENANT_ID, "SHIFT-001", "Cashier", "POS-01");

        // Then
        verify(telegramApi, never()).sendMessage(any(), any());
    }

    @Test
    void onShiftClosed_duplicateChatIds_sendsOnlyOnce() {
        // Given
        User user2 = User.builder()
                .id(20L)
                .username("testuser2")
                .firstName("Test2")
                .lastName("User2")
                .passwordHash("hash")
                .telegramChatId(CHAT_ID)
                .telegramLinkedAt(Instant.now())
                .build();
        user2.setTenantId(TENANT_ID);

        when(telegramApi.isConfigured()).thenReturn(true);
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_ENABLED))
                .thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_TRIGGER))
                .thenReturn("SHIFT_CLOSE");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_SALES))
                .thenReturn("true");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_INVENTORY))
                .thenReturn("false");
        when(tenantSettingService.getSettingValue(TENANT_ID, TelegramDailyReportService.SETTING_REPORT_FINANCE))
                .thenReturn("false");
        when(userRepository.findUsersWithTelegramByTenantId(TENANT_ID)).thenReturn(List.of(user, user2));
        when(transactionRepository.sumCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(new BigDecimal("500000"));
        when(transactionRepository.countCompletedSalesByDateRange(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(25);

        // When
        telegramDailyReportService.onShiftClosed(TENANT_ID, "SHIFT-001", "Cashier", "POS-01");

        // Then - same chatId, should only send once
        verify(telegramApi, times(1)).sendMessage(eq(CHAT_ID), any());
    }

    // ====== getReportBoolSetting ======

    @Test
    void getReportBoolSetting_trueValue_returnsTrue() {
        // Given
        when(tenantSettingService.getSettingValue(TENANT_ID, "test.key")).thenReturn("true");

        // When
        boolean result = telegramDailyReportService.getReportBoolSetting(TENANT_ID, "test.key", false);

        // Then
        assertTrue(result);
    }

    @Test
    void getReportBoolSetting_falseValue_returnsFalse() {
        // Given
        when(tenantSettingService.getSettingValue(TENANT_ID, "test.key")).thenReturn("false");

        // When
        boolean result = telegramDailyReportService.getReportBoolSetting(TENANT_ID, "test.key", true);

        // Then
        assertFalse(result);
    }

    @Test
    void getReportBoolSetting_nullValue_returnsDefault() {
        // Given
        when(tenantSettingService.getSettingValue(TENANT_ID, "test.key")).thenReturn(null);

        // When
        boolean result = telegramDailyReportService.getReportBoolSetting(TENANT_ID, "test.key", true);

        // Then
        assertTrue(result);
    }

    @Test
    void getReportBoolSetting_exception_returnsDefault() {
        // Given
        when(tenantSettingService.getSettingValue(TENANT_ID, "test.key"))
                .thenThrow(new RuntimeException("DB error"));

        // When
        boolean result = telegramDailyReportService.getReportBoolSetting(TENANT_ID, "test.key", true);

        // Then
        assertTrue(result);
    }

    // ====== getReportStrSetting ======

    @Test
    void getReportStrSetting_hasValue_returnsValue() {
        // Given
        when(tenantSettingService.getSettingValue(TENANT_ID, "test.key")).thenReturn("20:30");

        // When
        String result = telegramDailyReportService.getReportStrSetting(TENANT_ID, "test.key", "20:00");

        // Then
        assertEquals("20:30", result);
    }

    @Test
    void getReportStrSetting_nullValue_returnsDefault() {
        // Given
        when(tenantSettingService.getSettingValue(TENANT_ID, "test.key")).thenReturn(null);

        // When
        String result = telegramDailyReportService.getReportStrSetting(TENANT_ID, "test.key", "20:00");

        // Then
        assertEquals("20:00", result);
    }

    @Test
    void getReportStrSetting_blankValue_returnsDefault() {
        // Given
        when(tenantSettingService.getSettingValue(TENANT_ID, "test.key")).thenReturn("  ");

        // When
        String result = telegramDailyReportService.getReportStrSetting(TENANT_ID, "test.key", "20:00");

        // Then
        assertEquals("20:00", result);
    }

    @Test
    void getReportStrSetting_exception_returnsDefault() {
        // Given
        when(tenantSettingService.getSettingValue(TENANT_ID, "test.key"))
                .thenThrow(new RuntimeException("DB error"));

        // When
        String result = telegramDailyReportService.getReportStrSetting(TENANT_ID, "test.key", "DEFAULT");

        // Then
        assertEquals("DEFAULT", result);
    }
}
