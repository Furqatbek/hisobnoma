package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    Optional<Location> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Location> findByCodeAndTenantId(String code, Long tenantId);

    List<Location> findByTenantIdOrderBySortOrderAsc(Long tenantId);

    long countByTenantIdAndParentLocationIdIsNull(Long tenantId);

    List<Location> findByTenantIdAndActiveOrderBySortOrderAsc(Long tenantId, boolean active);

    List<Location> findByTenantIdAndLocationTypeOrderBySortOrderAsc(Long tenantId, LocationType locationType);

    List<Location> findByParentLocationIdAndTenantIdOrderBySortOrderAsc(Long parentLocationId, Long tenantId);

    @Query("SELECT l FROM Location l WHERE l.tenantId = :tenantId AND l.parentLocation IS NULL ORDER BY l.sortOrder")
    List<Location> findRootLocationsByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT l FROM Location l WHERE l.tenantId = :tenantId AND " +
           "(LOWER(l.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(l.code) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Location> searchByNameOrCode(@Param("tenantId") Long tenantId, @Param("query") String query, Pageable pageable);

    Page<Location> findByTenantId(Long tenantId, Pageable pageable);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    boolean existsByCodeAndTenantIdAndIdNot(String code, Long tenantId, Long id);

    Optional<Location> findByTenantIdAndIsDefaultTrue(Long tenantId);

    @Query("SELECT COUNT(l) > 0 FROM Location l WHERE l.parentLocation.id = :parentId AND l.tenantId = :tenantId")
    boolean hasChildren(@Param("parentId") Long parentId, @Param("tenantId") Long tenantId);

    @Query("SELECT l FROM Location l WHERE l.tenantId = :tenantId AND l.locationType IN :types AND l.active = true ORDER BY l.sortOrder")
    List<Location> findActiveByTypes(@Param("tenantId") Long tenantId, @Param("types") List<LocationType> types);
}
