package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId OR p.tenantId IS NULL")
    Page<Product> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (p.tenantId = :tenantId OR p.tenantId IS NULL) AND p.active = true")
    Page<Product> findAllActiveByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.id = :id AND (p.tenantId = :tenantId OR p.tenantId IS NULL)")
    Optional<Product> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Product p WHERE p.sku = :sku AND (p.tenantId = :tenantId OR p.tenantId IS NULL)")
    Optional<Product> findBySkuAndTenantId(@Param("sku") String sku, @Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Product p WHERE p.barcode = :barcode AND (p.tenantId = :tenantId OR p.tenantId IS NULL)")
    Optional<Product> findByBarcodeAndTenantId(@Param("barcode") String barcode, @Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Product p WHERE p.sku = :sku")
    Optional<Product> findBySku(@Param("sku") String sku);

    @Query("SELECT p FROM Product p WHERE p.barcode = :barcode")
    Optional<Product> findByBarcode(@Param("barcode") String barcode);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.category LEFT JOIN FETCH p.brand LEFT JOIN FETCH p.baseUom WHERE p.id = :id")
    Optional<Product> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.variants WHERE p.id = :id")
    Optional<Product> findByIdWithVariants(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.images WHERE p.id = :id")
    Optional<Product> findByIdWithImages(@Param("id") Long id);

    @Query("SELECT p FROM Product p LEFT JOIN FETCH p.attributes WHERE p.id = :id")
    Optional<Product> findByIdWithAttributes(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE (p.tenantId = :tenantId OR p.tenantId IS NULL) AND p.category.id = :categoryId")
    Page<Product> findByCategoryIdAndTenantId(@Param("categoryId") Long categoryId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (p.tenantId = :tenantId OR p.tenantId IS NULL) AND p.brand.id = :brandId")
    Page<Product> findByBrandIdAndTenantId(@Param("brandId") Long brandId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (p.tenantId = :tenantId OR p.tenantId IS NULL) AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE (p.tenantId = :tenantId OR p.tenantId IS NULL) AND p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.barcode) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Product> searchActiveByTenantId(@Param("tenantId") Long tenantId, @Param("search") String search, Pageable pageable);

    boolean existsBySkuAndTenantId(String sku, Long tenantId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.sku = :sku AND p.tenantId IS NULL")
    boolean existsBySkuAndTenantIdIsNull(@Param("sku") String sku);

    boolean existsByBarcodeAndTenantId(String barcode, Long tenantId);

    @Query("SELECT COUNT(p) > 0 FROM Product p WHERE p.barcode = :barcode AND p.tenantId IS NULL")
    boolean existsByBarcodeAndTenantIdIsNull(@Param("barcode") String barcode);

    @Query("SELECT p FROM Product p WHERE (p.tenantId = :tenantId OR p.tenantId IS NULL) AND p.active = true AND p.sellable = true")
    List<Product> findAllSellableByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Product p WHERE (p.tenantId = :tenantId OR p.tenantId IS NULL) AND p.active = true AND p.purchasable = true")
    List<Product> findAllPurchasableByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.tenantId = :tenantId OR p.tenantId IS NULL")
    long countByTenantId(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(p) FROM Product p WHERE (p.tenantId = :tenantId OR p.tenantId IS NULL) AND p.active = true")
    long countActiveByTenantId(@Param("tenantId") Long tenantId);

    // Mobile module methods
    @Query("SELECT COUNT(p) FROM Product p WHERE p.tenantId = :tenantId AND p.active = true")
    Integer countByTenantIdAndActiveTrue(@Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.active = true")
    List<Product> findByTenantIdAndActiveTrue(@Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.updatedAt > :since")
    List<Product> findByTenantIdAndUpdatedAtAfter(@Param("tenantId") Long tenantId, @Param("since") Instant since);

    @Query("SELECT MAX(p.updatedAt) FROM Product p WHERE p.tenantId = :tenantId")
    Instant findMaxUpdatedAt(@Param("tenantId") Long tenantId);

    @Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId AND p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "p.barcode = :query)")
    Page<Product> searchByNameOrSkuOrBarcode(@Param("tenantId") Long tenantId, @Param("query") String query, Pageable pageable);
}
