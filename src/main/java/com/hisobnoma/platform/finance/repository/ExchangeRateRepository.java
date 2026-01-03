package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.ExchangeRate;
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
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Query("SELECT er FROM ExchangeRate er WHERE er.tenantId = :tenantId OR er.tenantId IS NULL ORDER BY er.effectiveDate DESC")
    List<ExchangeRate> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT er FROM ExchangeRate er WHERE er.tenantId = :tenantId OR er.tenantId IS NULL")
    Page<ExchangeRate> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT er FROM ExchangeRate er WHERE er.id = :id AND (er.tenantId = :tenantId OR er.tenantId IS NULL)")
    Optional<ExchangeRate> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT er FROM ExchangeRate er WHERE (er.tenantId = :tenantId OR er.tenantId IS NULL) " +
           "AND er.fromCurrency = :fromCurrency AND er.toCurrency = :toCurrency " +
           "AND er.effectiveDate <= :date AND (er.expiryDate IS NULL OR er.expiryDate >= :date) " +
           "AND er.active = true ORDER BY er.effectiveDate DESC")
    Optional<ExchangeRate> findEffectiveRate(@Param("tenantId") Long tenantId,
                                              @Param("fromCurrency") String fromCurrency,
                                              @Param("toCurrency") String toCurrency,
                                              @Param("date") LocalDate date);

    @Query("SELECT er FROM ExchangeRate er WHERE (er.tenantId = :tenantId OR er.tenantId IS NULL) " +
           "AND er.fromCurrency = :fromCurrency AND er.toCurrency = :toCurrency " +
           "AND er.active = true ORDER BY er.effectiveDate DESC")
    List<ExchangeRate> findByFromCurrencyAndToCurrency(@Param("tenantId") Long tenantId,
                                                        @Param("fromCurrency") String fromCurrency,
                                                        @Param("toCurrency") String toCurrency);

    @Query("SELECT er FROM ExchangeRate er WHERE (er.tenantId = :tenantId OR er.tenantId IS NULL) " +
           "AND er.effectiveDate = :date ORDER BY er.fromCurrency, er.toCurrency")
    List<ExchangeRate> findByEffectiveDate(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    @Query("SELECT er FROM ExchangeRate er WHERE (er.tenantId = :tenantId OR er.tenantId IS NULL) " +
           "AND er.fromCurrency = :currency OR er.toCurrency = :currency " +
           "ORDER BY er.effectiveDate DESC")
    List<ExchangeRate> findByCurrency(@Param("tenantId") Long tenantId, @Param("currency") String currency);

    @Query("SELECT er FROM ExchangeRate er WHERE (er.tenantId = :tenantId OR er.tenantId IS NULL) " +
           "AND er.active = true AND er.effectiveDate <= CURRENT_DATE " +
           "AND (er.expiryDate IS NULL OR er.expiryDate >= CURRENT_DATE) " +
           "ORDER BY er.fromCurrency, er.toCurrency, er.effectiveDate DESC")
    List<ExchangeRate> findCurrentActiveRates(@Param("tenantId") Long tenantId);
}
