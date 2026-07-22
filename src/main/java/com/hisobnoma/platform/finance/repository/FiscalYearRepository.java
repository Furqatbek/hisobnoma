package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.FiscalYear;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {

    @Query("SELECT fy FROM FiscalYear fy WHERE fy.tenantId = :tenantId ORDER BY fy.year DESC")
    List<FiscalYear> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT fy FROM FiscalYear fy WHERE fy.tenantId = :tenantId ORDER BY fy.year DESC")
    Page<FiscalYear> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT fy FROM FiscalYear fy WHERE fy.id = :id AND fy.tenantId = :tenantId")
    Optional<FiscalYear> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT fy FROM FiscalYear fy WHERE fy.tenantId = :tenantId AND fy.year = :year")
    Optional<FiscalYear> findByYearAndTenantId(@Param("year") Integer year, @Param("tenantId") Long tenantId);

    @Query("SELECT fy FROM FiscalYear fy WHERE fy.tenantId = :tenantId AND fy.current = true")
    Optional<FiscalYear> findCurrentByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT fy FROM FiscalYear fy WHERE fy.tenantId = :tenantId AND :date BETWEEN fy.startDate AND fy.endDate")
    Optional<FiscalYear> findByDateAndTenantId(@Param("date") LocalDate date, @Param("tenantId") Long tenantId);

    @Query("SELECT fy FROM FiscalYear fy LEFT JOIN FETCH fy.periods "
            + "WHERE fy.id = :id AND fy.tenantId = :tenantId")
    Optional<FiscalYear> findByIdWithPeriods(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT fy FROM FiscalYear fy WHERE fy.tenantId = :tenantId AND fy.closed = false ORDER BY fy.year DESC")
    List<FiscalYear> findOpenYearsByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByYearAndTenantId(Integer year, Long tenantId);
}
