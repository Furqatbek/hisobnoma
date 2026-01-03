package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.Currency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, Long> {

    @Query("SELECT c FROM Currency c WHERE c.tenantId = :tenantId OR c.tenantId IS NULL ORDER BY c.sortOrder, c.code")
    List<Currency> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Currency c WHERE c.tenantId = :tenantId OR c.tenantId IS NULL")
    Page<Currency> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT c FROM Currency c WHERE (c.tenantId = :tenantId OR c.tenantId IS NULL) AND c.code = :code")
    Optional<Currency> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Currency c WHERE c.code = :code AND c.tenantId IS NULL")
    Optional<Currency> findByCode(@Param("code") String code);

    @Query("SELECT c FROM Currency c WHERE c.id = :id AND (c.tenantId = :tenantId OR c.tenantId IS NULL)")
    Optional<Currency> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Currency c WHERE (c.tenantId = :tenantId OR c.tenantId IS NULL) AND c.active = true ORDER BY c.sortOrder, c.code")
    List<Currency> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT c FROM Currency c WHERE (c.tenantId = :tenantId OR c.tenantId IS NULL) AND c.baseCurrency = true")
    Optional<Currency> findBaseCurrencyByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT COUNT(c) > 0 FROM Currency c WHERE c.code = :code AND c.tenantId IS NULL")
    boolean existsByCodeAndTenantIdIsNull(@Param("code") String code);
}
