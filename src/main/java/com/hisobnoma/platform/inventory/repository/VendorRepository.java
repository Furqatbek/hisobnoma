package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Vendor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VendorRepository extends JpaRepository<Vendor, Long> {

    @Query("SELECT v FROM Vendor v WHERE v.id = :id AND (v.tenantId = :tenantId OR v.tenantId IS NULL)")
    Optional<Vendor> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT v FROM Vendor v WHERE v.tenantId = :tenantId OR v.tenantId IS NULL")
    Page<Vendor> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT v FROM Vendor v WHERE v.tenantId = :tenantId OR v.tenantId IS NULL")
    List<Vendor> findAllByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT v FROM Vendor v WHERE (v.tenantId = :tenantId OR v.tenantId IS NULL) AND v.active = true")
    List<Vendor> findAllActiveByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT v FROM Vendor v WHERE v.code = :code AND (v.tenantId = :tenantId OR v.tenantId IS NULL)")
    Optional<Vendor> findByCodeAndTenantId(@Param("code") String code, @Param("tenantId") Long tenantId);

    @Query("SELECT v FROM Vendor v WHERE (v.tenantId = :tenantId OR v.tenantId IS NULL) AND " +
           "(LOWER(v.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(v.phone) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Vendor> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT v FROM Vendor v WHERE (v.tenantId = :tenantId OR v.tenantId IS NULL) AND v.preferred = true")
    List<Vendor> findPreferredVendorsByTenantId(@Param("tenantId") Long tenantId);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT COUNT(v) > 0 FROM Vendor v WHERE v.code = :code AND v.tenantId IS NULL")
    boolean existsByCodeAndTenantIdIsNull(@Param("code") String code);
}
