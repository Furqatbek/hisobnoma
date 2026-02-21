package com.hisobnoma.platform.mobile.repository;

import com.hisobnoma.platform.mobile.entity.AlertPreference;
import com.hisobnoma.platform.mobile.entity.MobileAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AlertPreferenceRepository extends JpaRepository<AlertPreference, Long> {

    List<AlertPreference> findByUserIdAndTenantId(Long userId, Long tenantId);

    Optional<AlertPreference> findByUserIdAndTenantIdAndAlertType(Long userId, Long tenantId, MobileAlert.AlertType alertType);

    @Query("SELECT ap FROM AlertPreference ap WHERE ap.userId = :userId AND ap.tenantId = :tenantId AND ap.pushEnabled = true")
    List<AlertPreference> findPushEnabledByUser(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    @Query("SELECT ap.userId FROM AlertPreference ap WHERE ap.tenantId = :tenantId " +
            "AND ap.alertType = :alertType AND ap.pushEnabled = true")
    List<Long> findUserIdsWithPushEnabledForType(@Param("tenantId") Long tenantId, @Param("alertType") MobileAlert.AlertType alertType);

    boolean existsByUserIdAndTenantIdAndAlertTypeAndPushEnabledTrue(Long userId, Long tenantId, MobileAlert.AlertType alertType);

    boolean existsByUserIdAndTenantIdAndAlertTypeAndTelegramEnabledTrue(Long userId, Long tenantId, MobileAlert.AlertType alertType);

    @Query("SELECT ap.userId FROM AlertPreference ap WHERE ap.tenantId = :tenantId " +
            "AND ap.alertType = :alertType AND ap.telegramEnabled = true")
    List<Long> findUserIdsWithTelegramEnabledForType(@Param("tenantId") Long tenantId, @Param("alertType") MobileAlert.AlertType alertType);
}
