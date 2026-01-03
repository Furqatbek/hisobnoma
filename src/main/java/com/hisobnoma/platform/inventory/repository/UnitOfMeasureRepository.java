package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitOfMeasureRepository extends JpaRepository<UnitOfMeasure, Long> {

    @Query("SELECT u FROM UnitOfMeasure u WHERE u.tenantId = :tenantId OR u.tenantId IS NULL ORDER BY u.name")
    List<UnitOfMeasure> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT u FROM UnitOfMeasure u WHERE u.tenantId = :tenantId OR u.tenantId IS NULL")
    Page<UnitOfMeasure> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT u FROM UnitOfMeasure u WHERE (u.tenantId = :tenantId OR u.tenantId IS NULL) AND u.code = :code")
    Optional<UnitOfMeasure> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    @Query("SELECT u FROM UnitOfMeasure u WHERE u.code = :code AND u.tenantId IS NULL")
    Optional<UnitOfMeasure> findByCode(@Param("code") String code);

    @Query("SELECT u FROM UnitOfMeasure u WHERE (u.tenantId = :tenantId OR u.tenantId IS NULL) AND u.active = true ORDER BY u.name")
    List<UnitOfMeasure> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT u FROM UnitOfMeasure u WHERE (u.tenantId = :tenantId OR u.tenantId IS NULL) AND u.isBaseUnit = true AND u.active = true ORDER BY u.name")
    List<UnitOfMeasure> findAllBaseUnitsByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT u FROM UnitOfMeasure u WHERE (u.tenantId = :tenantId OR u.tenantId IS NULL) AND " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<UnitOfMeasure> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT COUNT(u) > 0 FROM UnitOfMeasure u WHERE u.code = :code AND u.tenantId IS NULL")
    boolean existsByCodeAndTenantIdIsNull(@Param("code") String code);

    @Query("SELECT u FROM UnitOfMeasure u WHERE u.baseUom.id = :baseUomId")
    List<UnitOfMeasure> findByBaseUomId(@Param("baseUomId") Long baseUomId);

    @Query("SELECT u FROM UnitOfMeasure u WHERE u.id = :id AND (u.tenantId = :tenantId OR u.tenantId IS NULL)")
    Optional<UnitOfMeasure> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);
}
