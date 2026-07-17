package com.hisobnoma.platform.mobile.push.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.mobile.push.dto.RegisterPushTokenRequest;
import com.hisobnoma.platform.mobile.push.entity.DevicePushToken;
import com.hisobnoma.platform.mobile.push.entity.PushEnvironment;
import com.hisobnoma.platform.mobile.push.repository.DevicePushTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DevicePushTokenServiceTest {

    @Mock private DevicePushTokenRepository repository;
    @Mock private SecurityContextHelper securityContextHelper;
    @InjectMocks private DevicePushTokenService service;

    private static final Long TENANT_ID = 7L;
    private static final Long USER_ID = 42L;

    @Test
    void registerToken_newToken_createsWithOwnershipAndEnvironment() {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(repository.findByTenantIdAndToken(TENANT_ID, "tok-abc")).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DevicePushToken.class))).thenAnswer(i -> i.getArgument(0));

        RegisterPushTokenRequest req = RegisterPushTokenRequest.builder()
                .token("  tok-abc  ").platform("ios").environment("sandbox").appVersion("1.0.0").build();

        service.registerToken(req);

        ArgumentCaptor<DevicePushToken> captor = ArgumentCaptor.forClass(DevicePushToken.class);
        verify(repository).saveAndFlush(captor.capture());
        DevicePushToken saved = captor.getValue();
        assertEquals("tok-abc", saved.getToken(), "token is trimmed");
        assertEquals(TENANT_ID, saved.getTenantId());
        assertEquals(USER_ID, saved.getUserId());
        assertEquals("ios", saved.getPlatform());
        assertEquals(PushEnvironment.SANDBOX, saved.getEnvironment());
        assertEquals("1.0.0", saved.getAppVersion());
        assertNotNull(saved.getLastSeenAt());
    }

    @Test
    void registerToken_existingToken_upsertsInPlaceReassigningOwner() {
        DevicePushToken existing = DevicePushToken.builder()
                .token("tok-abc").tenantId(TENANT_ID).userId(1L)
                .environment(PushEnvironment.PRODUCTION).build();
        existing.setId(99L);

        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);
        when(repository.findByTenantIdAndToken(TENANT_ID, "tok-abc")).thenReturn(Optional.of(existing));
        when(repository.saveAndFlush(any(DevicePushToken.class))).thenAnswer(i -> i.getArgument(0));

        RegisterPushTokenRequest req = RegisterPushTokenRequest.builder()
                .token("tok-abc").environment("production").build();

        service.registerToken(req);

        ArgumentCaptor<DevicePushToken> captor = ArgumentCaptor.forClass(DevicePushToken.class);
        verify(repository).saveAndFlush(captor.capture());
        DevicePushToken saved = captor.getValue();
        assertEquals(99L, saved.getId(), "same row is updated, not a new one");
        assertEquals(USER_ID, saved.getUserId(), "ownership moves to the current user");
        assertEquals("ios", saved.getPlatform(), "platform defaults to ios when blank");
    }

    @Test
    void registerToken_lostInsertRace_retriesAsUpdateInsteadOf500() {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);

        // First pass: no row yet, so a new entity is built and the insert loses the race.
        // Second pass (retry): the winner's row is now visible and gets updated.
        DevicePushToken winner = DevicePushToken.builder()
                .token("tok-race").tenantId(TENANT_ID).userId(1L).build();
        winner.setId(500L);
        when(repository.findByTenantIdAndToken(TENANT_ID, "tok-race"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(repository.saveAndFlush(any(DevicePushToken.class)))
                .thenThrow(new DataIntegrityViolationException("uk_device_push_tokens_token"))
                .thenAnswer(i -> i.getArgument(0));

        RegisterPushTokenRequest req = RegisterPushTokenRequest.builder().token("tok-race").build();

        service.registerToken(req); // must not throw

        verify(repository, times(2)).findByTenantIdAndToken(TENANT_ID, "tok-race");
        ArgumentCaptor<DevicePushToken> captor = ArgumentCaptor.forClass(DevicePushToken.class);
        verify(repository, times(2)).saveAndFlush(captor.capture());
        assertEquals(500L, captor.getAllValues().get(1).getId(), "retry updates the winner's row");
    }

    @Test
    void removeToken_delegatesTenantScopedDelete() {
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);

        service.removeToken("  tok-xyz  ");

        verify(repository).deleteByTenantIdAndToken(TENANT_ID, "tok-xyz");
    }

    @Test
    void removeToken_blank_isNoOp() {
        service.removeToken("   ");
        service.removeToken(null);
        verifyNoInteractions(repository);
        verifyNoInteractions(securityContextHelper);
    }
}
