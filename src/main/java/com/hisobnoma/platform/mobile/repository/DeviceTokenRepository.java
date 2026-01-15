package com.hisobnoma.platform.mobile.repository;

import com.hisobnoma.platform.mobile.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByUserIdAndDeviceIdAndTenantId(Long userId, String deviceId, Long tenantId);

    List<DeviceToken> findByUserIdAndActiveTrue(Long userId);

    List<DeviceToken> findByUserIdAndTenantIdAndActiveTrue(Long userId, Long tenantId);

    @Query("SELECT dt FROM DeviceToken dt WHERE dt.userId IN :userIds AND dt.active = true")
    List<DeviceToken> findActiveByUserIds(@Param("userIds") List<Long> userIds);

    @Query("SELECT dt FROM DeviceToken dt WHERE dt.tenantId = :tenantId AND dt.active = true")
    List<DeviceToken> findAllActiveByTenant(@Param("tenantId") Long tenantId);

    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.active = false WHERE dt.userId = :userId AND dt.deviceId = :deviceId")
    void deactivateDevice(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.lastActiveAt = :lastActiveAt WHERE dt.id = :id")
    void updateLastActiveAt(@Param("id") Long id, @Param("lastActiveAt") Instant lastActiveAt);

    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.active = false WHERE dt.lastActiveAt < :cutoffDate")
    int deactivateInactiveDevices(@Param("cutoffDate") Instant cutoffDate);

    boolean existsByFcmTokenAndActiveTrue(String fcmToken);
}
