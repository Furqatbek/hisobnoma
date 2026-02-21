package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.ProductVendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVendorRepository extends JpaRepository<ProductVendor, Long> {

    List<ProductVendor> findByTenantIdAndProductId(Long tenantId, Long productId);

    List<ProductVendor> findByTenantIdAndVendorId(Long tenantId, Long vendorId);

    Optional<ProductVendor> findByIdAndTenantId(Long id, Long tenantId);

    Optional<ProductVendor> findByTenantIdAndProductIdAndVendorId(Long tenantId, Long productId, Long vendorId);

    boolean existsByTenantIdAndProductIdAndVendorId(Long tenantId, Long productId, Long vendorId);

    @Modifying
    @Query("UPDATE ProductVendor pv SET pv.preferred = false WHERE pv.tenantId = :tenantId AND pv.product.id = :productId")
    void clearPreferredForProduct(@Param("tenantId") Long tenantId, @Param("productId") Long productId);
}
