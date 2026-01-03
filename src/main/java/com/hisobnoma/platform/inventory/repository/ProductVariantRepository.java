package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductIdOrderBySortOrder(Long productId);

    @Query("SELECT v FROM ProductVariant v WHERE v.sku = :sku AND (v.tenantId = :tenantId OR v.tenantId IS NULL)")
    Optional<ProductVariant> findBySkuAndTenantId(@Param("sku") String sku, @Param("tenantId") Long tenantId);

    @Query("SELECT v FROM ProductVariant v WHERE v.barcode = :barcode AND (v.tenantId = :tenantId OR v.tenantId IS NULL)")
    Optional<ProductVariant> findByBarcodeAndTenantId(@Param("barcode") String barcode, @Param("tenantId") Long tenantId);

    @Query("SELECT v FROM ProductVariant v WHERE v.sku = :sku")
    Optional<ProductVariant> findBySku(@Param("sku") String sku);

    @Query("SELECT v FROM ProductVariant v WHERE v.barcode = :barcode")
    Optional<ProductVariant> findByBarcode(@Param("barcode") String barcode);

    @Query("SELECT v FROM ProductVariant v LEFT JOIN FETCH v.product WHERE v.id = :id")
    Optional<ProductVariant> findByIdWithProduct(@Param("id") Long id);

    List<ProductVariant> findByProductIdAndActiveTrue(Long productId);

    boolean existsBySkuAndTenantId(String sku, Long tenantId);

    boolean existsByBarcodeAndTenantId(String barcode, Long tenantId);

    @Query("SELECT COUNT(v) FROM ProductVariant v WHERE v.product.id = :productId")
    long countByProductId(@Param("productId") Long productId);

    void deleteByProductId(Long productId);
}
