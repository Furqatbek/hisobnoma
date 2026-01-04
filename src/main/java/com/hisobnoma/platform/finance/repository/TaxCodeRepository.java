package com.hisobnoma.platform.finance.repository;

import com.hisobnoma.platform.finance.entity.TaxCode;
import com.hisobnoma.platform.finance.entity.TaxType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaxCodeRepository extends JpaRepository<TaxCode, Long> {

    @Query("SELECT tc FROM TaxCode tc WHERE tc.tenantId = :tenantId OR tc.tenantId IS NULL ORDER BY tc.sortOrder, tc.code")
    List<TaxCode> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT tc FROM TaxCode tc WHERE tc.tenantId = :tenantId OR tc.tenantId IS NULL")
    Page<TaxCode> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT tc FROM TaxCode tc WHERE tc.id = :id AND (tc.tenantId = :tenantId OR tc.tenantId IS NULL)")
    Optional<TaxCode> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT tc FROM TaxCode tc WHERE tc.code = :code AND (tc.tenantId = :tenantId OR tc.tenantId IS NULL)")
    Optional<TaxCode> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    @Query("SELECT tc FROM TaxCode tc WHERE (tc.tenantId = :tenantId OR tc.tenantId IS NULL) AND tc.active = true ORDER BY tc.sortOrder, tc.code")
    List<TaxCode> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT tc FROM TaxCode tc WHERE tc.taxType = :type AND (tc.tenantId = :tenantId OR tc.tenantId IS NULL) ORDER BY tc.sortOrder, tc.code")
    List<TaxCode> findByTaxTypeAndTenantId(@Param("type") TaxType type, @Param("tenantId") Long tenantId);

    @Query("SELECT tc FROM TaxCode tc WHERE tc.appliesToSales = true AND tc.active = true AND (tc.tenantId = :tenantId OR tc.tenantId IS NULL) ORDER BY tc.sortOrder")
    List<TaxCode> findSalesTaxCodesByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT tc FROM TaxCode tc WHERE tc.appliesToPurchases = true AND tc.active = true AND (tc.tenantId = :tenantId OR tc.tenantId IS NULL) ORDER BY tc.sortOrder")
    List<TaxCode> findPurchaseTaxCodesByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT COUNT(tc) > 0 FROM TaxCode tc WHERE tc.code = :code AND tc.tenantId IS NULL")
    boolean existsByCodeAndTenantIdIsNull(@Param("code") String code);

    @Query("SELECT tc FROM TaxCode tc WHERE (tc.tenantId = :tenantId OR tc.tenantId IS NULL) " +
           "AND (LOWER(tc.code) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(tc.name) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<TaxCode> searchByTenantId(@Param("search") String search, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT tc FROM TaxCode tc LEFT JOIN FETCH tc.rates WHERE tc.id = :id AND (tc.tenantId = :tenantId OR tc.tenantId IS NULL)")
    Optional<TaxCode> findByIdWithRatesAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT tc FROM TaxCode tc LEFT JOIN FETCH tc.rates WHERE tc.code = :code AND (tc.tenantId = :tenantId OR tc.tenantId IS NULL)")
    Optional<TaxCode> findByCodeWithRatesAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);
}
