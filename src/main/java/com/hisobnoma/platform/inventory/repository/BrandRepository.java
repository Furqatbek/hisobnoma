package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Brand;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandRepository extends JpaRepository<Brand, Long> {

    @Query("SELECT b FROM Brand b WHERE b.id = :id AND (b.tenantId = :tenantId OR b.tenantId IS NULL)")
    Optional<Brand> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT b FROM Brand b WHERE b.tenantId = :tenantId OR b.tenantId IS NULL ORDER BY b.sortOrder")
    List<Brand> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT b FROM Brand b WHERE b.tenantId = :tenantId OR b.tenantId IS NULL")
    Page<Brand> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT b FROM Brand b WHERE (b.tenantId = :tenantId OR b.tenantId IS NULL) AND b.code = :code")
    Optional<Brand> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    @Query("SELECT b FROM Brand b WHERE (b.tenantId = :tenantId OR b.tenantId IS NULL) AND b.active = true ORDER BY b.sortOrder")
    List<Brand> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT b FROM Brand b WHERE (b.tenantId = :tenantId OR b.tenantId IS NULL) AND " +
           "(LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(b.code) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Brand> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT COUNT(b) > 0 FROM Brand b WHERE b.code = :code AND b.tenantId IS NULL")
    boolean existsByCodeAndTenantIdIsNull(@Param("code") String code);
}
