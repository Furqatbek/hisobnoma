package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.TaxRate;
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
public interface TaxRateRepository extends JpaRepository<TaxRate, Long> {

    @Query("SELECT tr FROM TaxRate tr WHERE tr.tenantId = :tenantId OR tr.tenantId IS NULL ORDER BY tr.effectiveFrom DESC")
    List<TaxRate> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT tr FROM TaxRate tr WHERE tr.tenantId = :tenantId OR tr.tenantId IS NULL")
    Page<TaxRate> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT tr FROM TaxRate tr WHERE tr.id = :id AND (tr.tenantId = :tenantId OR tr.tenantId IS NULL)")
    Optional<TaxRate> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT tr FROM TaxRate tr WHERE tr.taxCode.id = :taxCodeId AND (tr.tenantId = :tenantId OR tr.tenantId IS NULL) ORDER BY tr.effectiveFrom DESC")
    List<TaxRate> findByTaxCodeIdAndTenantId(@Param("taxCodeId") Long taxCodeId, @Param("tenantId") Long tenantId);

    @Query("SELECT tr FROM TaxRate tr WHERE tr.taxCode.id = :taxCodeId " +
           "AND tr.active = true " +
           "AND tr.effectiveFrom <= :date " +
           "AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :date) " +
           "AND (tr.tenantId = :tenantId OR tr.tenantId IS NULL) " +
           "ORDER BY tr.effectiveFrom DESC")
    Optional<TaxRate> findEffectiveRateByTaxCodeIdAndDate(
            @Param("taxCodeId") Long taxCodeId,
            @Param("date") LocalDate date,
            @Param("tenantId") Long tenantId);

    @Query("SELECT tr FROM TaxRate tr WHERE tr.taxCode.code = :taxCode " +
           "AND tr.active = true " +
           "AND tr.effectiveFrom <= :date " +
           "AND (tr.effectiveTo IS NULL OR tr.effectiveTo >= :date) " +
           "AND (tr.tenantId = :tenantId OR tr.tenantId IS NULL) " +
           "ORDER BY tr.effectiveFrom DESC")
    Optional<TaxRate> findEffectiveRateByTaxCodeAndDate(
            @Param("taxCode") String taxCode,
            @Param("date") LocalDate date,
            @Param("tenantId") Long tenantId);

    @Query("SELECT tr FROM TaxRate tr WHERE tr.taxCode.id = :taxCodeId AND tr.active = true AND (tr.tenantId = :tenantId OR tr.tenantId IS NULL)")
    List<TaxRate> findActiveByTaxCodeIdAndTenantId(@Param("taxCodeId") Long taxCodeId, @Param("tenantId") Long tenantId);

    @Query("SELECT tr FROM TaxRate tr WHERE tr.effectiveTo < :date AND tr.active = true AND (tr.tenantId = :tenantId OR tr.tenantId IS NULL)")
    List<TaxRate> findExpiredRates(@Param("date") LocalDate date, @Param("tenantId") Long tenantId);
}
