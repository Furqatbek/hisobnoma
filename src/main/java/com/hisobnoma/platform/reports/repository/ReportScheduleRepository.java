package com.hisobnoma.platform.reports.repository;

import com.hisobnoma.platform.reports.entity.ReportSchedule;
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
public interface ReportScheduleRepository extends JpaRepository<ReportSchedule, Long> {

    Optional<ReportSchedule> findByIdAndTenantId(Long id, Long tenantId);

    Page<ReportSchedule> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT s FROM ReportSchedule s WHERE s.tenantId = :tenantId AND s.reportDefinition.id = :reportDefinitionId")
    List<ReportSchedule> findByReportDefinitionIdAndTenantId(@Param("reportDefinitionId") Long reportDefinitionId, @Param("tenantId") Long tenantId);

    @Query("SELECT s FROM ReportSchedule s WHERE s.active = true AND s.nextRunAt <= :now")
    List<ReportSchedule> findSchedulesDueForExecution(@Param("now") Instant now);

    @Query("SELECT s FROM ReportSchedule s WHERE s.tenantId = :tenantId AND s.active = true ORDER BY s.nextRunAt")
    List<ReportSchedule> findActiveSchedulesByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query("UPDATE ReportSchedule s SET s.lastRunAt = :lastRunAt, s.nextRunAt = :nextRunAt, s.lastRunStatus = :status WHERE s.id = :id")
    void updateExecutionStatus(@Param("id") Long id, @Param("lastRunAt") Instant lastRunAt,
                               @Param("nextRunAt") Instant nextRunAt, @Param("status") String status);

    @Query("SELECT s FROM ReportSchedule s WHERE s.tenantId = :tenantId AND s.createdByUserId = :userId")
    List<ReportSchedule> findByCreatedByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);
}
