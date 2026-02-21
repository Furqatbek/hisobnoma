package com.hisobnoma.platform.delivery.repository;

import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryVillageRepository extends JpaRepository<DeliveryVillage, Long> {

    @Query("SELECT v FROM DeliveryVillage v JOIN FETCH v.region WHERE v.tenantId = :tenantId ORDER BY v.region.name, v.sortOrder, v.name")
    Page<DeliveryVillage> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT v FROM DeliveryVillage v JOIN FETCH v.region WHERE v.tenantId = :tenantId AND v.id = :id")
    Optional<DeliveryVillage> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT v FROM DeliveryVillage v JOIN FETCH v.region WHERE v.tenantId = :tenantId AND v.region.id = :regionId AND v.active = true ORDER BY v.sortOrder, v.name")
    List<DeliveryVillage> findActiveByRegionIdAndTenantId(@Param("regionId") Long regionId, @Param("tenantId") Long tenantId);

    @Query("SELECT v FROM DeliveryVillage v JOIN FETCH v.region WHERE v.tenantId = :tenantId AND v.active = true ORDER BY v.region.name, v.sortOrder, v.name")
    List<DeliveryVillage> findActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT v FROM DeliveryVillage v JOIN FETCH v.region WHERE v.tenantId = :tenantId AND " +
           "(LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<DeliveryVillage> searchByTenantId(@Param("search") String search, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM DeliveryVillage v " +
           "WHERE v.tenantId = :tenantId AND v.code = :code AND (:excludeId IS NULL OR v.id != :excludeId)")
    boolean existsByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId, @Param("excludeId") Long excludeId);

    @Query("SELECT COUNT(v) FROM DeliveryVillage v WHERE v.region.id = :regionId")
    long countByRegionId(@Param("regionId") Long regionId);
}
