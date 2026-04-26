package com.hisobnoma.platform.mobile.repository;

import com.hisobnoma.platform.mobile.entity.DeviceToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class DeviceTokenRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private DeviceTokenRepository deviceTokenRepository;

    private static final Long TENANT_ID = 1L;

    private DeviceToken persistToken(Long userId, String deviceId, String fcmToken,
                                      DeviceToken.Platform platform, boolean active,
                                      Instant lastActiveAt) {
        DeviceToken dt = DeviceToken.builder()
                .userId(userId).deviceId(deviceId).fcmToken(fcmToken)
                .platform(platform).active(active).lastActiveAt(lastActiveAt)
                .build();
        dt.setTenantId(TENANT_ID);
        return em.persistAndFlush(dt);
    }

    @Test
    void findByUserIdAndDeviceIdAndTenantId_existing_returnsToken() {
        persistToken(100L, "device-1", "fcm-token-1", DeviceToken.Platform.ANDROID, true, Instant.now());

        Optional<DeviceToken> result = deviceTokenRepository.findByUserIdAndDeviceIdAndTenantId(
                100L, "device-1", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("fcm-token-1", result.get().getFcmToken());
    }

    @Test
    void findByUserIdAndDeviceIdAndTenantId_wrongUser_returnsEmpty() {
        persistToken(100L, "device-1", "fcm-token-1", DeviceToken.Platform.ANDROID, true, Instant.now());

        Optional<DeviceToken> result = deviceTokenRepository.findByUserIdAndDeviceIdAndTenantId(
                999L, "device-1", TENANT_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void findByUserIdAndActiveTrue_returnsActiveTokensAcrossTenants() {
        persistToken(100L, "device-1", "fcm-1", DeviceToken.Platform.ANDROID, true, Instant.now());
        persistToken(100L, "device-2", "fcm-2", DeviceToken.Platform.IOS, false, Instant.now());

        DeviceToken otherTenantToken = DeviceToken.builder()
                .userId(100L).deviceId("device-3").fcmToken("fcm-3")
                .platform(DeviceToken.Platform.WEB).active(true).build();
        otherTenantToken.setTenantId(2L);
        em.persistAndFlush(otherTenantToken);

        List<DeviceToken> result = deviceTokenRepository.findByUserIdAndActiveTrue(100L);

        assertEquals(2, result.size());
    }

    @Test
    void findByUserIdAndTenantIdAndActiveTrue_returnsTenantScopedActiveTokens() {
        persistToken(100L, "device-1", "fcm-1", DeviceToken.Platform.ANDROID, true, Instant.now());
        persistToken(100L, "device-2", "fcm-2", DeviceToken.Platform.IOS, false, Instant.now());

        List<DeviceToken> result = deviceTokenRepository.findByUserIdAndTenantIdAndActiveTrue(100L, TENANT_ID);

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void findActiveByUserIds_returnsActiveTokensForMultipleUsers() {
        persistToken(100L, "device-1", "fcm-1", DeviceToken.Platform.ANDROID, true, Instant.now());
        persistToken(101L, "device-2", "fcm-2", DeviceToken.Platform.IOS, true, Instant.now());
        persistToken(102L, "device-3", "fcm-3", DeviceToken.Platform.ANDROID, false, Instant.now());

        List<DeviceToken> result = deviceTokenRepository.findActiveByUserIds(List.of(100L, 101L, 102L));

        assertEquals(2, result.size());
    }

    @Test
    void findAllActiveByTenant_returnsAllActiveForTenant() {
        persistToken(100L, "device-1", "fcm-1", DeviceToken.Platform.ANDROID, true, Instant.now());
        persistToken(101L, "device-2", "fcm-2", DeviceToken.Platform.IOS, true, Instant.now());
        persistToken(102L, "device-3", "fcm-3", DeviceToken.Platform.ANDROID, false, Instant.now());

        List<DeviceToken> result = deviceTokenRepository.findAllActiveByTenant(TENANT_ID);

        assertEquals(2, result.size());
    }

    @Test
    void existsByFcmTokenAndActiveTrue_existing_returnsTrue() {
        persistToken(100L, "device-1", "fcm-unique", DeviceToken.Platform.ANDROID, true, Instant.now());

        assertTrue(deviceTokenRepository.existsByFcmTokenAndActiveTrue("fcm-unique"));
        assertFalse(deviceTokenRepository.existsByFcmTokenAndActiveTrue("fcm-nope"));
    }

    @Test
    void existsByFcmTokenAndActiveTrue_inactiveToken_returnsFalse() {
        persistToken(100L, "device-1", "fcm-inactive", DeviceToken.Platform.ANDROID, false, Instant.now());

        assertFalse(deviceTokenRepository.existsByFcmTokenAndActiveTrue("fcm-inactive"));
    }
}
