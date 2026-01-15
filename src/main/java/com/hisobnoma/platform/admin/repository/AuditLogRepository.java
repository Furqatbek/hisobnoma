package com.hisobnoma.platform.admin.repository;

import com.hisobnoma.platform.admin.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    Page<AuditLog> findByTenantId(Long tenantId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndUserId(Long tenantId, Long userId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndEntityTypeAndEntityId(Long tenantId, String entityType, Long entityId, Pageable pageable);

    Page<AuditLog> findByTenantIdAndAction(Long tenantId, AuditLog.AuditAction action, Pageable pageable);

    Page<AuditLog> findByTenantIdAndModule(Long tenantId, String module, Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.actionTimestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY a.actionTimestamp DESC")
    Page<AuditLog> findByTenantIdAndDateRange(@Param("tenantId") Long tenantId,
                                               @Param("startDate") Instant startDate,
                                               @Param("endDate") Instant endDate,
                                               Pageable pageable);

    @Query("SELECT a.action, COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId AND a.actionTimestamp >= :since " +
           "GROUP BY a.action ORDER BY COUNT(a) DESC")
    List<Object[]> countActionsByTenantIdSince(@Param("tenantId") Long tenantId, @Param("since") Instant since);

    @Query("SELECT a.module, COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId AND a.actionTimestamp >= :since " +
           "GROUP BY a.module ORDER BY COUNT(a) DESC")
    List<Object[]> countModuleActivityByTenantIdSince(@Param("tenantId") Long tenantId, @Param("since") Instant since);

    @Query("SELECT a.userId, a.username, COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId " +
           "AND a.actionTimestamp >= :since GROUP BY a.userId, a.username ORDER BY COUNT(a) DESC")
    List<Object[]> findMostActiveUsersByTenantIdSince(@Param("tenantId") Long tenantId, @Param("since") Instant since);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.action IN :actions " +
           "ORDER BY a.actionTimestamp DESC")
    Page<AuditLog> findByTenantIdAndActionIn(@Param("tenantId") Long tenantId,
                                              @Param("actions") List<AuditLog.AuditAction> actions,
                                              Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.tenantId = :tenantId AND a.action = 'LOGIN_FAILED' " +
           "AND a.actionTimestamp >= :since")
    long countFailedLoginsSince(@Param("tenantId") Long tenantId, @Param("since") Instant since);

    @Query("SELECT a FROM AuditLog a WHERE a.tenantId = :tenantId AND a.success = false " +
           "ORDER BY a.actionTimestamp DESC")
    Page<AuditLog> findFailedActionsByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);
}
