package com.hisobnoma.platform.mobile.push.repository;

import com.hisobnoma.platform.mobile.push.entity.DevicePushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DevicePushTokenRepository extends JpaRepository<DevicePushToken, Long> {

    Optional<DevicePushToken> findByTenantIdAndToken(Long tenantId, String token);

    List<DevicePushToken> findByTenantId(Long tenantId);

    @Query("SELECT d FROM DevicePushToken d WHERE d.tenantId = :tenantId AND d.userId = :userId")
    List<DevicePushToken> findByTenantIdAndUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    void deleteByTenantIdAndToken(Long tenantId, String token);
}
