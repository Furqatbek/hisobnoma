package com.hisobnoma.platform.reports.repository;

import com.hisobnoma.platform.reports.entity.ReportExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportExecutionRepository extends JpaRepository<ReportExecution, Long> {

    Optional<ReportExecution> findByIdAndTenantId(Long id, Long tenantId);

    Page<ReportExecution> findByTenantIdOrderByStartedAtDesc(Long tenantId, Pageable pageable);

    @Query("SELECT e FROM ReportExecution e WHERE e.tenantId = :tenantId AND e.reportDefinition.id = :reportDefinitionId ORDER BY e.startedAt DESC")
    Page<ReportExecution> findByReportDefinitionIdAndTenantId(@Param("reportDefinitionId") Long reportDefinitionId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT e FROM ReportExecution e WHERE e.tenantId = :tenantId AND e.executedByUserId = :userId ORDER BY e.startedAt DESC")
    Page<ReportExecution> findByExecutedByUserIdAndTenantId(@Param("userId") Long userId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT e FROM ReportExecution e WHERE e.tenantId = :tenantId AND e.status = :status ORDER BY e.startedAt DESC")
    List<ReportExecution> findByStatusAndTenantId(@Param("status") ReportExecution.ExecutionStatus status, @Param("tenantId") Long tenantId);

    @Query("SELECT e FROM ReportExecution e WHERE e.status = 'RUNNING' AND e.startedAt < :threshold")
    List<ReportExecution> findStaleRunningExecutions(@Param("threshold") Instant threshold);

    @Modifying
    @Query("UPDATE ReportExecution e SET e.status = :status, e.completedAt = :completedAt, e.durationMs = :durationMs, " +
           "e.rowCount = :rowCount, e.filePath = :filePath, e.fileSize = :fileSize WHERE e.id = :id")
    void updateCompletedExecution(@Param("id") Long id, @Param("status") ReportExecution.ExecutionStatus status,
                                  @Param("completedAt") Instant completedAt, @Param("durationMs") Long durationMs,
                                  @Param("rowCount") Integer rowCount, @Param("filePath") String filePath,
                                  @Param("fileSize") Long fileSize);

    @Modifying
    @Query("UPDATE ReportExecution e SET e.status = 'FAILED', e.completedAt = :completedAt, e.errorMessage = :errorMessage, " +
           "e.errorStackTrace = :stackTrace WHERE e.id = :id")
    void updateFailedExecution(@Param("id") Long id, @Param("completedAt") Instant completedAt,
                               @Param("errorMessage") String errorMessage, @Param("stackTrace") String stackTrace);

    @Modifying
    @Query("DELETE FROM ReportExecution e WHERE e.tenantId = :tenantId AND e.completedAt < :before")
    int deleteOldExecutions(@Param("tenantId") Long tenantId, @Param("before") Instant before);

    @Query("SELECT COUNT(e) FROM ReportExecution e WHERE e.tenantId = :tenantId AND e.reportDefinition.id = :reportDefinitionId AND e.startedAt >= :since")
    long countRecentExecutions(@Param("tenantId") Long tenantId, @Param("reportDefinitionId") Long reportDefinitionId, @Param("since") Instant since);
}
