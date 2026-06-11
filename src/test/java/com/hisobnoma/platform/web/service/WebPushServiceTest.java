package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.mobile.service.PushNotificationService;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebDeviceToken;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import com.hisobnoma.platform.web.repository.WebDeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebPushServiceTest {

    @Mock private WebDeviceTokenRepository tokenRepository;
    @Mock private WebCustomerRepository customerRepository;
    @Mock private PushNotificationService pushNotificationService;
    @Mock private WebNotificationService notificationService;

    @InjectMocks
    private WebPushService service;

    @Test
    void register_newTokenCreatesEntry() {
        when(tokenRepository.findByTenantIdAndToken(1L, "fcm-token-1")).thenReturn(Optional.empty());
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.register(1L, 10L, "fcm-token-1", "ANDROID");

        verify(tokenRepository).save(argThat(t ->
                t.getTenantId() == 1L && t.getWebCustomerId() == 10L
                        && "fcm-token-1".equals(t.getToken()) && "ANDROID".equals(t.getPlatform())));
    }

    @Test
    void register_existingTokenUpdatesCustomer() {
        WebDeviceToken existing = WebDeviceToken.builder()
                .tenantId(1L).webCustomerId(5L).token("fcm-token-1").platform("IOS")
                .lastSeenAt(Instant.now().minusSeconds(3600)).build();
        when(tokenRepository.findByTenantIdAndToken(1L, "fcm-token-1")).thenReturn(Optional.of(existing));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.register(1L, 10L, "fcm-token-1", "ANDROID");

        assertEquals(10L, existing.getWebCustomerId());
        assertEquals("ANDROID", existing.getPlatform());
        verify(tokenRepository).save(existing);
    }

    @Test
    void sendToCustomer_sendsToAllDeviceTokens() {
        List<WebDeviceToken> tokens = List.of(
                WebDeviceToken.builder().token("tok1").tenantId(1L).webCustomerId(10L).build(),
                WebDeviceToken.builder().token("tok2").tenantId(1L).webCustomerId(10L).build());
        when(tokenRepository.findByTenantIdAndWebCustomerId(1L, 10L)).thenReturn(tokens);

        service.sendToCustomer(1L, 10L, "Title", "Body", Map.of("k", "v"));

        verify(pushNotificationService).sendToDevice("tok1", "Title", "Body", Map.of("k", "v"));
        verify(pushNotificationService).sendToDevice("tok2", "Title", "Body", Map.of("k", "v"));
    }

    @Test
    void sendToCustomer_failureOnOneTokenDoesNotBlockOthers() {
        List<WebDeviceToken> tokens = List.of(
                WebDeviceToken.builder().token("tok1").tenantId(1L).webCustomerId(10L).build(),
                WebDeviceToken.builder().token("tok2").tenantId(1L).webCustomerId(10L).build());
        when(tokenRepository.findByTenantIdAndWebCustomerId(1L, 10L)).thenReturn(tokens);
        doThrow(new RuntimeException("FCM error")).when(pushNotificationService)
                .sendToDevice(eq("tok1"), any(), any(), any());

        assertDoesNotThrow(() -> service.sendToCustomer(1L, 10L, "T", "B", Map.of()));
        verify(pushNotificationService).sendToDevice(eq("tok2"), any(), any(), any());
    }

    @Test
    void notifyOrderStatusChange_sendsForConfirmedDeliveringCompleted() {
        WebCustomer customer = WebCustomer.builder().phone("998901234567").tenantId(1L).build();
        customer.setId(10L);
        when(customerRepository.findByTenantIdAndPhone(1L, "998901234567"))
                .thenReturn(Optional.of(customer));
        when(tokenRepository.findByTenantIdAndWebCustomerId(1L, 10L))
                .thenReturn(List.of(WebDeviceToken.builder().token("tok1").tenantId(1L).webCustomerId(10L).build()));

        WebOrder order = WebOrder.builder()
                .orderNumber("WO-001").phoneNormalized("998901234567")
                .tenantId(1L).totalAmount(BigDecimal.valueOf(10000)).build();

        service.notifyOrderStatusChange(order, WebOrderStatus.CONFIRMED);
        verify(pushNotificationService).sendToDevice(eq("tok1"), contains("тасдиқланди"), any(), any());

        service.notifyOrderStatusChange(order, WebOrderStatus.DELIVERING);
        verify(pushNotificationService).sendToDevice(eq("tok1"), contains("йўлда"), any(), any());

        service.notifyOrderStatusChange(order, WebOrderStatus.COMPLETED);
        verify(pushNotificationService).sendToDevice(eq("tok1"), contains("тугалланди"), any(), any());
    }

    @Test
    void notifyOrderStatusChange_doesNotThrowOnFailure() {
        WebCustomer customer = WebCustomer.builder().phone("998901234567").tenantId(1L).build();
        customer.setId(10L);
        when(customerRepository.findByTenantIdAndPhone(1L, "998901234567"))
                .thenReturn(Optional.of(customer));
        when(tokenRepository.findByTenantIdAndWebCustomerId(1L, 10L))
                .thenThrow(new RuntimeException("DB error"));

        WebOrder order = WebOrder.builder()
                .orderNumber("WO-001").phoneNormalized("998901234567")
                .tenantId(1L).totalAmount(BigDecimal.valueOf(10000)).build();

        assertDoesNotThrow(() -> service.notifyOrderStatusChange(order, WebOrderStatus.CONFIRMED));
    }

    @Test
    void hasPushToken_returnsTrueWhenTokenExists() {
        when(tokenRepository.existsByTenantIdAndWebCustomerId(1L, 10L)).thenReturn(true);
        assertTrue(service.hasPushToken(1L, 10L));

        when(tokenRepository.existsByTenantIdAndWebCustomerId(1L, 20L)).thenReturn(false);
        assertFalse(service.hasPushToken(1L, 20L));
    }
}
