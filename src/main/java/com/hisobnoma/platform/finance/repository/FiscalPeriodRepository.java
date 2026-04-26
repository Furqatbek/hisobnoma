package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.FiscalPeriod;
import com.hisobnoma.platform.finance.entity.PeriodStatus;
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
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, Long> {

    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.tenantId = :tenantId ORDER BY fp.fiscalYear.year DESC, fp.periodNumber")
    List<FiscalPeriod> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.tenantId = :tenantId")
    Page<FiscalPeriod> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.id = :id AND fp.tenantId = :tenantId")
    Optional<FiscalPeriod> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.fiscalYear.id = :fiscalYearId ORDER BY fp.periodNumber")
    List<FiscalPeriod> findByFiscalYearId(@Param("fiscalYearId") Long fiscalYearId);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.tenantId = :tenantId AND :date BETWEEN fp.startDate AND fp.endDate")
    Optional<FiscalPeriod> findByDateAndTenantId(@Param("date") LocalDate date, @Param("tenantId") Long tenantId);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.tenantId = :tenantId AND fp.status = :status ORDER BY fp.fiscalYear.year, fp.periodNumber")
    List<FiscalPeriod> findByStatusAndTenantId(@Param("tenantId") Long tenantId, @Param("status") PeriodStatus status);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.tenantId = :tenantId AND fp.status = 'OPEN' ORDER BY fp.fiscalYear.year, fp.periodNumber")
    List<FiscalPeriod> findOpenPeriodsByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.tenantId = :tenantId AND fp.status = 'OPEN' " +
           "ORDER BY fp.fiscalYear.year, fp.periodNumber LIMIT 1")
    Optional<FiscalPeriod> findFirstOpenPeriodByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.fiscalYear.id = :fiscalYearId AND fp.periodNumber = :periodNumber")
    Optional<FiscalPeriod> findByFiscalYearAndPeriodNumber(@Param("fiscalYearId") Long fiscalYearId, @Param("periodNumber") Integer periodNumber);

    @Query("SELECT COUNT(fp) FROM FiscalPeriod fp WHERE fp.fiscalYear.id = :fiscalYearId AND fp.status = 'OPEN'")
    long countOpenPeriodsByFiscalYear(@Param("fiscalYearId") Long fiscalYearId);
}
