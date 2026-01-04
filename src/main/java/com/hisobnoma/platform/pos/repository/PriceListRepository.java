package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.PriceList;
import com.hisobnoma.platform.pos.enums.PriceListType;
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
public interface PriceListRepository extends JpaRepository<PriceList, Long> {

    Optional<PriceList> findByIdAndTenantId(Long id, Long tenantId);

    Optional<PriceList> findByCodeAndTenantId(String code, Long tenantId);

    Page<PriceList> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT pl FROM PriceList pl WHERE pl.tenantId = :tenantId " +
           "AND (LOWER(pl.code) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(pl.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<PriceList> searchByTenantId(@Param("query") String query, @Param("tenantId") Long tenantId, Pageable pageable);

    List<PriceList> findByTenantIdAndActive(Long tenantId, boolean active);

    List<PriceList> findByTenantIdAndType(Long tenantId, PriceListType type);

    Optional<PriceList> findByTenantIdAndDefaultPriceListTrue(Long tenantId);

    @Query("SELECT pl FROM PriceList pl WHERE pl.tenantId = :tenantId " +
           "AND pl.active = true " +
           "AND (pl.startDate IS NULL OR pl.startDate <= :date) " +
           "AND (pl.endDate IS NULL OR pl.endDate >= :date) " +
           "ORDER BY pl.priority DESC")
    List<PriceList> findEffectivePriceLists(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    @Query("SELECT pl FROM PriceList pl WHERE pl.tenantId = :tenantId " +
           "AND pl.active = true " +
           "AND (pl.locationId IS NULL OR pl.locationId = :locationId) " +
           "AND (pl.startDate IS NULL OR pl.startDate <= :date) " +
           "AND (pl.endDate IS NULL OR pl.endDate >= :date) " +
           "ORDER BY pl.priority DESC")
    List<PriceList> findEffectivePriceListsForLocation(
            @Param("tenantId") Long tenantId,
            @Param("locationId") Long locationId,
            @Param("date") LocalDate date);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT COUNT(pl) FROM PriceList pl WHERE pl.tenantId = :tenantId AND pl.active = true")
    long countActiveByTenantId(@Param("tenantId") Long tenantId);
}
