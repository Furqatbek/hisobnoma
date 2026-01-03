package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.InventoryCountLine;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryCountLineRepository extends JpaRepository<InventoryCountLine, Long> {

    @Query("SELECT icl FROM InventoryCountLine icl WHERE icl.id = :id AND (icl.tenantId = :tenantId OR icl.tenantId IS NULL)")
    Optional<InventoryCountLine> findByIdAndTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    @Query("SELECT icl FROM InventoryCountLine icl WHERE icl.inventoryCount.id = :countId AND (icl.tenantId = :tenantId OR icl.tenantId IS NULL) ORDER BY icl.lineNumber")
    List<InventoryCountLine> findByInventoryCountIdAndTenantId(@Param("countId") Long countId, @Param("tenantId") Long tenantId);

    @Query("SELECT icl FROM InventoryCountLine icl WHERE icl.inventoryCount.id = :countId AND (icl.tenantId = :tenantId OR icl.tenantId IS NULL)")
    Page<InventoryCountLine> findByInventoryCountIdAndTenantIdPageable(@Param("countId") Long countId, @Param("tenantId") Long tenantId, Pageable pageable);

    @Query("SELECT icl FROM InventoryCountLine icl WHERE icl.inventoryCount.id = :countId AND icl.counted = false AND (icl.tenantId = :tenantId OR icl.tenantId IS NULL)")
    List<InventoryCountLine> findUncountedLinesByCountId(@Param("countId") Long countId, @Param("tenantId") Long tenantId);

    @Query("SELECT icl FROM InventoryCountLine icl WHERE icl.inventoryCount.id = :countId AND icl.varianceQuantity != 0 AND (icl.tenantId = :tenantId OR icl.tenantId IS NULL)")
    List<InventoryCountLine> findVarianceLinessByCountId(@Param("countId") Long countId, @Param("tenantId") Long tenantId);

    @Query("SELECT icl FROM InventoryCountLine icl WHERE icl.inventoryCount.id = :countId AND icl.recountRequired = true AND (icl.tenantId = :tenantId OR icl.tenantId IS NULL)")
    List<InventoryCountLine> findRecountRequiredLinesByCountId(@Param("countId") Long countId, @Param("tenantId") Long tenantId);

    @Query("SELECT icl FROM InventoryCountLine icl WHERE icl.inventoryCount.id = :countId AND icl.product.id = :productId AND (icl.tenantId = :tenantId OR icl.tenantId IS NULL)")
    Optional<InventoryCountLine> findByCountIdAndProductId(@Param("countId") Long countId, @Param("productId") Long productId, @Param("tenantId") Long tenantId);

    @Query("SELECT MAX(icl.lineNumber) FROM InventoryCountLine icl WHERE icl.inventoryCount.id = :countId")
    Integer findMaxLineNumberByInventoryCountId(@Param("countId") Long countId);

    @Query("SELECT COUNT(icl) FROM InventoryCountLine icl WHERE icl.inventoryCount.id = :countId AND icl.counted = true")
    long countCountedLinesByCountId(@Param("countId") Long countId);

    @Query("SELECT COUNT(icl) FROM InventoryCountLine icl WHERE icl.inventoryCount.id = :countId")
    long countTotalLinesByCountId(@Param("countId") Long countId);
}
