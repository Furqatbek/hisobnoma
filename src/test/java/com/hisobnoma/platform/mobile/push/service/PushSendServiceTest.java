package com.hisobnoma.platform.mobile.push.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.mobile.push.apns.ApnsClient;
import com.hisobnoma.platform.mobile.push.apns.ApnsPayload;
import com.hisobnoma.platform.mobile.push.apns.ApnsResult;
import com.hisobnoma.platform.mobile.push.dto.PushSendResult;
import com.hisobnoma.platform.mobile.push.dto.SendNotificationRequest;
import com.hisobnoma.platform.mobile.push.entity.DevicePushToken;
import com.hisobnoma.platform.mobile.push.entity.PushEnvironment;
import com.hisobnoma.platform.mobile.push.repository.DevicePushTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushSendServiceTest {

    @Mock private DevicePushTokenRepository repository;
    @Mock private ApnsClient apnsClient;
    @Mock private SecurityContextHelper securityContextHelper;
    @InjectMocks private PushSendService service;

    private static final Long TENANT_ID = 3L;

    private DevicePushToken token(long id, String value, PushEnvironment env) {
        DevicePushToken t = DevicePushToken.builder()
                .token(value).tenantId(TENANT_ID).userId(1L).environment(env).build();
        t.setId(id);
        return t;
    }

    @Test
    void send_tenantAudience_fansOutAndPrunesDeadTokens() {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        DevicePushToken good = token(10L, "good", PushEnvironment.PRODUCTION);
        DevicePushToken dead = token(11L, "dead", PushEnvironment.SANDBOX);
        DevicePushToken flaky = token(12L, "flaky", PushEnvironment.PRODUCTION);
        when(repository.findByTenantId(TENANT_ID)).thenReturn(List.of(good, dead, flaky));

        when(apnsClient.send(eq("good"), eq(PushEnvironment.PRODUCTION), any())).thenReturn(ApnsResult.ok());
        when(apnsClient.send(eq("dead"), eq(PushEnvironment.SANDBOX), any()))
                .thenReturn(ApnsResult.dead(410, "Unregistered"));
        when(apnsClient.send(eq("flaky"), eq(PushEnvironment.PRODUCTION), any()))
                .thenReturn(ApnsResult.failed(503, "ServiceUnavailable"));
        when(apnsClient.isConfigured()).thenReturn(true);

        SendNotificationRequest req = SendNotificationRequest.builder()
                .title("Hi").body("There").type("system").build();

        PushSendResult result = service.send(req);

        assertEquals(3, result.recipients());
        assertEquals(1, result.sent());
        assertEquals(2, result.failed());
        assertEquals(1, result.pruned());
        assertTrue(result.apnsConfigured());

        // Only the permanently-dead token is deleted; the flaky (transient) one is kept.
        ArgumentCaptor<List<Long>> ids = ArgumentCaptor.forClass(List.class);
        verify(repository).deleteAllById(ids.capture());
        assertEquals(List.of(11L), ids.getValue());

        // Each token routed to the correct Apple host by its own environment.
        verify(apnsClient).send(eq("good"), eq(PushEnvironment.PRODUCTION), any(ApnsPayload.class));
        verify(apnsClient).send(eq("dead"), eq(PushEnvironment.SANDBOX), any(ApnsPayload.class));
    }

    @Test
    void send_noDeadTokens_doesNotTouchDelete() {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(repository.findByTenantId(TENANT_ID)).thenReturn(List.of(token(10L, "good", PushEnvironment.PRODUCTION)));
        when(apnsClient.send(any(), any(), any())).thenReturn(ApnsResult.ok());
        when(apnsClient.isConfigured()).thenReturn(true);

        PushSendResult result = service.send(SendNotificationRequest.builder().title("t").body("b").build());

        assertEquals(0, result.pruned());
        verify(repository, never()).deleteAllById(any());
    }

    @Test
    void send_userAudience_requiresUserId() {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);

        SendNotificationRequest req = SendNotificationRequest.builder()
                .audience("user").title("t").body("b").build();

        BusinessException ex = assertThrows(BusinessException.class, () -> service.send(req));
        assertEquals("USER_ID_REQUIRED", ex.getCode());
        verifyNoInteractions(apnsClient);
    }

    @Test
    void send_userAudience_targetsOnlyThatUsersTokens() {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(repository.findByTenantIdAndUserId(TENANT_ID, 99L))
                .thenReturn(List.of(token(10L, "u", PushEnvironment.PRODUCTION)));
        when(apnsClient.send(any(), any(), any())).thenReturn(ApnsResult.ok());
        when(apnsClient.isConfigured()).thenReturn(true);

        service.send(SendNotificationRequest.builder().audience("user").userId(99L).title("t").body("b").build());

        verify(repository).findByTenantIdAndUserId(TENANT_ID, 99L);
        verify(repository, never()).findByTenantId(any());
    }

    @Test
    void notifyTenant_broadcastsToAllTenantTokensAndPrunesDead() {
        DevicePushToken good = token(10L, "good", PushEnvironment.PRODUCTION);
        DevicePushToken dead = token(11L, "dead", PushEnvironment.SANDBOX);
        when(repository.findByTenantId(TENANT_ID)).thenReturn(List.of(good, dead));
        when(apnsClient.send(eq("good"), any(), any())).thenReturn(ApnsResult.ok());
        when(apnsClient.send(eq("dead"), any(), any())).thenReturn(ApnsResult.dead(410, "Unregistered"));

        // System-triggered path: no security context is touched (checkout is anonymous).
        service.notifyTenant(TENANT_ID, new ApnsPayload("New order", "WO-1", null, "new_order", 5L, null));

        verify(repository).findByTenantId(TENANT_ID);
        verify(repository).deleteAllById(List.of(11L));
        verifyNoInteractions(securityContextHelper);
    }

    @Test
    void notifyTenant_swallowsErrorsSoItNeverBreaksTheCaller() {
        when(repository.findByTenantId(TENANT_ID)).thenThrow(new RuntimeException("db down"));

        // Must not propagate — the triggering operation (checkout) cannot be broken by a push failure.
        service.notifyTenant(TENANT_ID, new ApnsPayload("t", "b", null, null, null, null));
    }

    @Test
    void send_apnsNotConfigured_reportsSkippedWithoutFakingDelivery() {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(repository.findByTenantId(TENANT_ID)).thenReturn(List.of(token(10L, "t", PushEnvironment.PRODUCTION)));
        when(apnsClient.send(any(), any(), any())).thenReturn(ApnsResult.skipped());
        when(apnsClient.isConfigured()).thenReturn(false);

        PushSendResult result = service.send(SendNotificationRequest.builder().title("t").body("b").build());

        assertFalse(result.apnsConfigured());
        assertEquals(1, result.recipients());
        assertEquals(0, result.sent());
        assertEquals(1, result.failed());
    }
}
