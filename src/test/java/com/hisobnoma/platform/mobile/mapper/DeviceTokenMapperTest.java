package com.hisobnoma.platform.mobile.mapper;

import com.hisobnoma.platform.mobile.dto.DeviceTokenDTO;
import com.hisobnoma.platform.mobile.dto.RegisterDeviceRequest;
import com.hisobnoma.platform.mobile.entity.DeviceToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DeviceTokenMapperTest {

    @Autowired
    private DeviceTokenMapper deviceTokenMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Instant lastActiveAt = Instant.now();
        DeviceToken entity = DeviceToken.builder()
                .id(1L)
                .tenantId(1L)
                .userId(10L)
                .deviceId("device-123")
                .fcmToken("fcm-token-abc")
                .platform(DeviceToken.Platform.ANDROID)
                .deviceName("Samsung Galaxy S23")
                .deviceModel("SM-S911B")
                .osVersion("14.0")
                .appVersion("2.1.0")
                .lastActiveAt(lastActiveAt)
                .active(true)
                .build();

        // When
        DeviceTokenDTO dto = deviceTokenMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("device-123", dto.getDeviceId());
        assertEquals("fcm-token-abc", dto.getFcmToken());
        assertEquals(DeviceToken.Platform.ANDROID, dto.getPlatform());
        assertEquals("Samsung Galaxy S23", dto.getDeviceName());
        assertEquals("SM-S911B", dto.getDeviceModel());
        assertEquals("14.0", dto.getOsVersion());
        assertEquals("2.1.0", dto.getAppVersion());
        assertEquals(lastActiveAt, dto.getLastActiveAt());
        assertTrue(dto.isActive());
    }

    @Test
    void fromRequest_mapsBasicFields() {
        // Given
        RegisterDeviceRequest request = RegisterDeviceRequest.builder()
                .deviceId("device-456")
                .fcmToken("fcm-token-xyz")
                .platform(DeviceToken.Platform.IOS)
                .deviceName("iPhone 15")
                .deviceModel("iPhone15,2")
                .osVersion("17.0")
                .appVersion("2.0.0")
                .build();

        // When
        DeviceToken entity = deviceTokenMapper.fromRequest(request);

        // Then
        assertNotNull(entity);
        assertEquals("device-456", entity.getDeviceId());
        assertEquals("fcm-token-xyz", entity.getFcmToken());
        assertEquals(DeviceToken.Platform.IOS, entity.getPlatform());
        assertEquals("iPhone 15", entity.getDeviceName());
        assertEquals("iPhone15,2", entity.getDeviceModel());
        assertEquals("17.0", entity.getOsVersion());
        assertEquals("2.0.0", entity.getAppVersion());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getUserId());
    }

    @Test
    void updateFromRequest_updatesFieldsOnExisting() {
        // Given
        DeviceToken existing = DeviceToken.builder()
                .id(1L)
                .tenantId(1L)
                .userId(10L)
                .deviceId("device-123")
                .fcmToken("old-token")
                .platform(DeviceToken.Platform.ANDROID)
                .deviceName("Old Phone")
                .active(true)
                .build();

        RegisterDeviceRequest request = RegisterDeviceRequest.builder()
                .deviceId("device-123")
                .fcmToken("new-token")
                .platform(DeviceToken.Platform.ANDROID)
                .deviceName("New Phone")
                .deviceModel("SM-S911B")
                .osVersion("14.0")
                .appVersion("3.0.0")
                .build();

        // When
        deviceTokenMapper.updateFromRequest(request, existing);

        // Then
        assertEquals("new-token", existing.getFcmToken());
        assertEquals("New Phone", existing.getDeviceName());
        assertEquals("SM-S911B", existing.getDeviceModel());
        assertEquals("14.0", existing.getOsVersion());
        assertEquals("3.0.0", existing.getAppVersion());
        // ID, tenantId, userId, deviceId should remain unchanged
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
        assertEquals(10L, existing.getUserId());
        assertEquals("device-123", existing.getDeviceId());
    }

    @Test
    void toDtoList_mapsAllTokens() {
        // Given
        DeviceToken t1 = DeviceToken.builder()
                .id(1L).deviceId("d1").fcmToken("t1")
                .platform(DeviceToken.Platform.ANDROID).active(true).build();
        DeviceToken t2 = DeviceToken.builder()
                .id(2L).deviceId("d2").fcmToken("t2")
                .platform(DeviceToken.Platform.IOS).active(true).build();

        // When
        List<DeviceTokenDTO> dtos = deviceTokenMapper.toDtoList(List.of(t1, t2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("d1", dtos.get(0).getDeviceId());
        assertEquals("d2", dtos.get(1).getDeviceId());
    }
}
