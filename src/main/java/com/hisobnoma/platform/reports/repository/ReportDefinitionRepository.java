package com.hisobnoma.platform.reports.repository;

import com.hisobnoma.platform.reports.entity.ReportDefinition;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportDefinitionRepository extends JpaRepository<ReportDefinition, Long> {

    Optional<ReportDefinition> findByCodeAndTenantId(String code, Long tenantId);

    Optional<ReportDefinition> findByIdAndTenantId(Long id, Long tenantId);

    @Query("SELECT r FROM ReportDefinition r WHERE (r.tenantId = :tenantId OR r.systemReport = true) AND r.active = true ORDER BY r.category, r.sortOrder")
    List<ReportDefinition> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT r FROM ReportDefinition r WHERE (r.tenantId = :tenantId OR r.systemReport = true) ORDER BY r.category, r.sortOrder")
    Page<ReportDefinition> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT r FROM ReportDefinition r WHERE (r.tenantId = :tenantId OR r.systemReport = true) AND r.active = true AND r.category = :category ORDER BY r.sortOrder")
    List<ReportDefinition> findByCategoryAndTenantId(@Param("category") ReportDefinition.ReportCategory category, @Param("tenantId") Long tenantId);

    @Query("SELECT r FROM ReportDefinition r WHERE (r.tenantId = :tenantId OR r.systemReport = true) AND r.active = true AND r.category = :category ORDER BY r.sortOrder")
    Page<ReportDefinition> findByCategoryAndTenantIdPaged(@Param("category") ReportDefinition.ReportCategory category, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT r FROM ReportDefinition r WHERE r.tenantId = :tenantId AND " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<ReportDefinition> searchByTenantId(@Param("search") String search, @Param("tenantId") Long tenantId, Pageable pageable);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT r FROM ReportDefinition r WHERE r.systemReport = true AND r.active = true ORDER BY r.category, r.sortOrder")
    List<ReportDefinition> findAllSystemReports();
}
