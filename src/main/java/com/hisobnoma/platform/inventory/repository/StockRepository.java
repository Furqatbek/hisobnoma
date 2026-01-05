package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.Stock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByProductIdAndLocationIdAndTenantId(Long productId, Long locationId, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Stock s WHERE s.product.id = :productId AND s.location.id = :locationId AND s.tenantId = :tenantId")
    Optional<Stock> findByProductIdAndLocationIdWithLock(@Param("productId") Long productId,
                                                          @Param("locationId") Long locationId,
                                                          @Param("tenantId") Long tenantId);

    List<Stock> findByProductIdAndTenantId(Long productId, Long tenantId);

    List<Stock> findByLocationIdAndTenantId(Long locationId, Long tenantId);

    Page<Stock> findByTenantId(Long tenantId, Pageable pageable);

    Page<Stock> findByLocationIdAndTenantId(Long locationId, Long tenantId, Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE s.tenantId = :tenantId AND " +
           "(s.quantityOnHand - s.quantityReserved) <= COALESCE(s.reorderPoint, s.product.reorderPoint)")
    Page<Stock> findBelowReorderPoint(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE s.tenantId = :tenantId AND " +
           "s.quantityOnHand <= COALESCE(s.minStockLevel, s.product.minStockLevel)")
    Page<Stock> findBelowMinimum(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE s.tenantId = :tenantId AND s.quantityOnHand = 0")
    Page<Stock> findOutOfStock(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT SUM(s.quantityOnHand) FROM Stock s WHERE s.product.id = :productId AND s.tenantId = :tenantId")
    BigDecimal getTotalQuantityByProduct(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

    @Query("SELECT SUM(s.quantityOnHand - s.quantityReserved) FROM Stock s WHERE s.product.id = :productId AND s.tenantId = :tenantId")
    BigDecimal getAvailableQuantityByProduct(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

    @Query("SELECT SUM(s.quantityOnHand * s.averageCost) FROM Stock s WHERE s.tenantId = :tenantId")
    BigDecimal getTotalStockValue(@Param("tenantId") Long tenantId);

    @Query("SELECT SUM(s.quantityOnHand * s.averageCost) FROM Stock s WHERE s.location.id = :locationId AND s.tenantId = :tenantId")
    BigDecimal getStockValueByLocation(@Param("locationId") Long locationId, @Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(DISTINCT s.product.id) FROM Stock s WHERE s.tenantId = :tenantId AND s.quantityOnHand > 0")
    Long countProductsWithStock(@Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Stock s JOIN FETCH s.product p JOIN FETCH s.location l WHERE s.tenantId = :tenantId")
    List<Stock> findAllWithProductAndLocation(@Param("tenantId") Long tenantId);

    @Query("SELECT s FROM Stock s WHERE s.tenantId = :tenantId AND " +
           "s.product.category.id = :categoryId")
    Page<Stock> findByCategory(@Param("tenantId") Long tenantId, @Param("categoryId") Long categoryId, Pageable pageable);

    @Query("SELECT s FROM Stock s WHERE s.tenantId = :tenantId AND " +
           "(LOWER(s.product.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.product.sku) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "s.product.barcode = :query)")
    Page<Stock> searchStock(@Param("tenantId") Long tenantId, @Param("query") String query, Pageable pageable);

    // Inventory Planning queries
    List<Stock> findByProductId(Long productId);

    List<Stock> findByProductIdAndLocationId(Long productId, Long locationId);

    @Query("SELECT s FROM Stock s WHERE s.tenantId = :tenantId")
    List<Stock> findByTenantId(@Param("tenantId") Long tenantId);
}
