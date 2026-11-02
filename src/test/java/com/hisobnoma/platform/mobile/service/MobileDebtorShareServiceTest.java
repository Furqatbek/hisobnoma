package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.finance.dto.CustomerBalanceReportDto;
import com.hisobnoma.platform.finance.service.ARReportService;
import com.hisobnoma.platform.telegram.service.TelegramNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileDebtorShareServiceTest {

    private static final Long TENANT = 1L;
    private static final Long USER_ID = 10L;

    @Mock private SecurityContextHelper securityContextHelper;
    @Mock private ARReportService arReportService;
    @Mock private UserRepository userRepository;
    @Mock private TelegramNotificationService telegramNotificationService;

    @InjectMocks private MobileDebtorShareService service;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT);
        CustomerBalanceReportDto report = CustomerBalanceReportDto.builder()
                .customerBalances(List.of(
                        balance("Ali", "500000"),
                        balance("Vali", "1200000"),
                        balance("PaidUp", "0")))   // filtered out (no outstanding balance)
                .build();
        lenient().when(arReportService.generateCustomerBalanceReport()).thenReturn(report);
    }

    private CustomerBalanceReportDto.CustomerBalanceDto balance(String name, String net) {
        return CustomerBalanceReportDto.CustomerBalanceDto.builder()
                .customerName(name).netBalance(new BigDecimal(net)).build();
    }

    private User userWithChat(Long chatId) {
        User u = User.builder().username("staff").tenantId(TENANT).telegramChatId(chatId).build();
        u.setId(USER_ID);
        return u;
    }

    @Test
    void shareSelf_sendsToCallersChat_withOnlyDebtorsSortedDesc() {
        when(securityContextHelper.getRequiredCurrentUser())
                .thenReturn(new UserPrincipal(USER_ID, "staff", "p", TENANT, true, true, List.of()));
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT))
                .thenReturn(Optional.of(userWithChat(555L)));

        var result = service.shareDebtors("self");

        assertEquals("self", result.getTarget());
        assertEquals(1, result.getRecipients());
        assertEquals(1, result.getSent());

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(telegramNotificationService).sendDirectMessage(eq(555L), anyString(), body.capture());
        String text = body.getValue();
        assertTrue(text.contains("Vali"));
        assertTrue(text.contains("Ali"));
        assertFalse(text.contains("PaidUp"));          // zero balance excluded
        assertTrue(text.indexOf("Vali") < text.indexOf("Ali")); // largest debt first
    }

    @Test
    void shareSelf_notLinked_throws() {
        when(securityContextHelper.getRequiredCurrentUser())
                .thenReturn(new UserPrincipal(USER_ID, "staff", "p", TENANT, true, true, List.of()));
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT))
                .thenReturn(Optional.of(userWithChat(null)));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.shareDebtors("self"));
        assertEquals("TELEGRAM_NOT_LINKED", ex.getCode());
        verifyNoInteractions(telegramNotificationService);
    }

    @Test
    void shareGroup_sendsToEachUniqueLinkedChat() {
        User a = userWithChat(100L);
        User b = User.builder().username("b").tenantId(TENANT).telegramChatId(200L).build();
        User dup = User.builder().username("c").tenantId(TENANT).telegramChatId(100L).build(); // same chat
        when(userRepository.findByTenantIdAndTelegramChatIdIsNotNull(TENANT))
                .thenReturn(List.of(a, b, dup));

        var result = service.shareDebtors("group");

        assertEquals("group", result.getTarget());
        assertEquals(2, result.getRecipients());        // deduped
        assertEquals(2, result.getSent());
        verify(telegramNotificationService).sendDirectMessage(eq(100L), anyString(), anyString());
        verify(telegramNotificationService).sendDirectMessage(eq(200L), anyString(), anyString());
    }

    @Test
    void shareGroup_noLinkedStaff_throws() {
        when(userRepository.findByTenantIdAndTelegramChatIdIsNotNull(TENANT)).thenReturn(List.of());
        BusinessException ex = assertThrows(BusinessException.class, () -> service.shareDebtors("group"));
        assertEquals("TELEGRAM_NO_RECIPIENTS", ex.getCode());
    }
}
