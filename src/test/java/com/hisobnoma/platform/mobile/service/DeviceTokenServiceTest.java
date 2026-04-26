package com.hisobnoma.platform.mobile.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.mobile.dto.DeviceTokenDTO;
import com.hisobnoma.platform.mobile.dto.RegisterDeviceRequest;
import com.hisobnoma.platform.mobile.entity.DeviceToken;
import com.hisobnoma.platform.mobile.mapper.DeviceTokenMapper;
import com.hisobnoma.platform.mobile.repository.DeviceTokenRepository;
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
class DeviceTokenServiceTest {

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

    @Mock
    private DeviceTokenMapper deviceTokenMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    private DeviceToken deviceToken;
    private DeviceTokenDTO deviceTokenDTO;
    private RegisterDeviceRequest registerRequest;
    private UserPrincipal userPrincipal;
    private static final Long USER_ID = 10L;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        userPrincipal = mock(UserPrincipal.class);

        deviceToken = DeviceToken.builder()
                .id(1L)
                .userId(USER_ID)
                .deviceId("device-abc-123")
                .fcmToken("fcm-token-xyz")
                .platform(DeviceToken.Platform.ANDROID)
                .deviceName("Pixel 7")
                .deviceModel("Pixel 7 Pro")
                .osVersion("14.0")
                .appVersion("1.2.0")
                .lastActiveAt(Instant.now())
                .active(true)
                .build();
        deviceToken.setTenantId(TENANT_ID);

        deviceTokenDTO = DeviceTokenDTO.builder()
                .id(1L)
                .deviceId("device-abc-123")
                .fcmToken("fcm-token-xyz")
                .platform(DeviceToken.Platform.ANDROID)
                .deviceName("Pixel 7")
                .deviceModel("Pixel 7 Pro")
                .osVersion("14.0")
                .appVersion("1.2.0")
                .active(true)
                .build();

        registerRequest = RegisterDeviceRequest.builder()
                .deviceId("device-abc-123")
                .fcmToken("fcm-token-xyz")
                .platform(DeviceToken.Platform.ANDROID)
                .deviceName("Pixel 7")
                .deviceModel("Pixel 7 Pro")
                .osVersion("14.0")
                .appVersion("1.2.0")
                .build();
    }

    // ====== registerDevice ======

    @Test
    void registerDevice_newDevice_createsAndReturnsDto() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(deviceTokenRepository.findByUserIdAndDeviceIdAndTenantId(USER_ID, "device-abc-123", TENANT_ID))
                .thenReturn(Optional.empty());
        when(deviceTokenMapper.fromRequest(registerRequest)).thenReturn(deviceToken);
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(deviceToken);
        when(deviceTokenMapper.toDto(deviceToken)).thenReturn(deviceTokenDTO);

        // When
        DeviceTokenDTO result = deviceTokenService.registerDevice(registerRequest);

        // Then
        assertNotNull(result);
        assertEquals("device-abc-123", result.getDeviceId());
        assertEquals(DeviceToken.Platform.ANDROID, result.getPlatform());
        verify(deviceTokenRepository).save(any(DeviceToken.class));
    }

    @Test
    void registerDevice_existingDevice_updatesAndReturnsDto() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(deviceTokenRepository.findByUserIdAndDeviceIdAndTenantId(USER_ID, "device-abc-123", TENANT_ID))
                .thenReturn(Optional.of(deviceToken));
        when(deviceTokenRepository.save(any(DeviceToken.class))).thenReturn(deviceToken);
        when(deviceTokenMapper.toDto(deviceToken)).thenReturn(deviceTokenDTO);

        // When
        DeviceTokenDTO result = deviceTokenService.registerDevice(registerRequest);

        // Then
        assertNotNull(result);
        assertEquals("device-abc-123", result.getDeviceId());
        verify(deviceTokenMapper, never()).fromRequest(any());
        verify(deviceTokenRepository).save(deviceToken);
    }

    // ====== getDevices ======

    @Test
    void getDevices_returnsActiveDevicesForCurrentUser() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(deviceTokenRepository.findByUserIdAndTenantIdAndActiveTrue(USER_ID, TENANT_ID))
                .thenReturn(List.of(deviceToken));
        when(deviceTokenMapper.toDtoList(List.of(deviceToken))).thenReturn(List.of(deviceTokenDTO));

        // When
        List<DeviceTokenDTO> result = deviceTokenService.getDevices();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    // ====== deactivateDevice ======

    @Test
    void deactivateDevice_callsRepository() {
        // Given
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(userPrincipal.getId()).thenReturn(USER_ID);

        // When
        deviceTokenService.deactivateDevice("device-abc-123");

        // Then
        verify(deviceTokenRepository).deactivateDevice(USER_ID, "device-abc-123");
    }

    // ====== updateLastActive ======

    @Test
    void updateLastActive_callsRepository() {
        // When
        deviceTokenService.updateLastActive(1L);

        // Then
        verify(deviceTokenRepository).updateLastActiveAt(eq(1L), any(Instant.class));
    }

    // ====== getActiveDeviceTokens ======

    @Test
    void getActiveDeviceTokens_returnsTokensForUser() {
        // Given
        when(deviceTokenRepository.findByUserIdAndActiveTrue(USER_ID)).thenReturn(List.of(deviceToken));

        // When
        List<DeviceToken> result = deviceTokenService.getActiveDeviceTokens(USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(USER_ID, result.get(0).getUserId());
    }

    // ====== getActiveDeviceTokensForUsers ======

    @Test
    void getActiveDeviceTokensForUsers_returnsTokensForMultipleUsers() {
        // Given
        List<Long> userIds = List.of(10L, 20L);
        when(deviceTokenRepository.findActiveByUserIds(userIds)).thenReturn(List.of(deviceToken));

        // When
        List<DeviceToken> result = deviceTokenService.getActiveDeviceTokensForUsers(userIds);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(deviceTokenRepository).findActiveByUserIds(userIds);
    }

    // ====== cleanupInactiveDevices ======

    @Test
    void cleanupInactiveDevices_deactivatesOldDevices() {
        // Given
        when(deviceTokenRepository.deactivateInactiveDevices(any(Instant.class))).thenReturn(5);

        // When
        deviceTokenService.cleanupInactiveDevices();

        // Then
        verify(deviceTokenRepository).deactivateInactiveDevices(any(Instant.class));
    }
}
