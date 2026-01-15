package com.hisobnoma.platform.mobile.repository;

import com.hisobnoma.platform.mobile.entity.MobileAlert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MobileAlertRepository extends JpaRepository<MobileAlert, Long> {

    Page<MobileAlert> findByUserIdAndTenantIdOrderByCreatedAtDesc(Long userId, Long tenantId, Pageable pageable);

    Page<MobileAlert> findByUserIdAndTenantIdAndReadFalseOrderByCreatedAtDesc(Long userId, Long tenantId, Pageable pageable);

    Page<MobileAlert> findByUserIdAndTenantIdAndAlertTypeOrderByCreatedAtDesc(
            Long userId, Long tenantId, MobileAlert.AlertType alertType, Pageable pageable);

    @Query("SELECT ma FROM MobileAlert ma WHERE ma.userId = :userId AND ma.tenantId = :tenantId " +
            "AND ma.read = false AND (ma.expiresAt IS NULL OR ma.expiresAt > :now)")
    List<MobileAlert> findUnreadByUser(@Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("now") Instant now);

    @Query("SELECT COUNT(ma) FROM MobileAlert ma WHERE ma.userId = :userId AND ma.tenantId = :tenantId " +
            "AND ma.read = false AND (ma.expiresAt IS NULL OR ma.expiresAt > :now)")
    long countUnreadByUser(@Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE MobileAlert ma SET ma.read = true, ma.readAt = :readAt WHERE ma.id = :id AND ma.userId = :userId")
    int markAsRead(@Param("id") Long id, @Param("userId") Long userId, @Param("readAt") Instant readAt);

    @Modifying
    @Query("UPDATE MobileAlert ma SET ma.read = true, ma.readAt = :readAt WHERE ma.userId = :userId AND ma.tenantId = :tenantId AND ma.read = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("tenantId") Long tenantId, @Param("readAt") Instant readAt);

    @Modifying
    @Query("DELETE FROM MobileAlert ma WHERE ma.expiresAt IS NOT NULL AND ma.expiresAt < :cutoffDate")
    int deleteExpiredAlerts(@Param("cutoffDate") Instant cutoffDate);

    @Query("SELECT ma FROM MobileAlert ma WHERE ma.sentViaPush = false AND ma.userId = :userId " +
            "AND ma.createdAt > :since ORDER BY ma.createdAt DESC")
    List<MobileAlert> findUnsentPushAlerts(@Param("userId") Long userId, @Param("since") Instant since);
}
