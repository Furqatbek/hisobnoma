package com.hisobnoma.platform.delivery.repository;

import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRegionRepository extends JpaRepository<DeliveryRegion, Long> {

    @Query("SELECT r FROM DeliveryRegion r WHERE r.tenantId = :tenantId ORDER BY r.sortOrder, r.name")
    Page<DeliveryRegion> findByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT r FROM DeliveryRegion r WHERE r.tenantId = :tenantId AND r.id = :id")
    Optional<DeliveryRegion> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT r FROM DeliveryRegion r WHERE r.tenantId = :tenantId AND r.active = true ORDER BY r.sortOrder, r.name")
    List<DeliveryRegion> findActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT r FROM DeliveryRegion r WHERE r.tenantId = :tenantId AND " +
           "(LOWER(r.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(r.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<DeliveryRegion> searchByTenantId(@Param("search") String search, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM DeliveryRegion r " +
           "WHERE r.tenantId = :tenantId AND r.code = :code AND (:excludeId IS NULL OR r.id != :excludeId)")
    boolean existsByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId, @Param("excludeId") Long excludeId);
}
