package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.StockBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockBatchRepository extends JpaRepository<StockBatch, Long> {

    Optional<StockBatch> findByProductIdAndLocationIdAndBatchNumberAndTenantId(
            Long productId, Long locationId, String batchNumber, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM StockBatch b WHERE b.product.id = :productId AND b.location.id = :locationId " +
           "AND b.batchNumber = :batchNumber AND b.tenantId = :tenantId")
    Optional<StockBatch> findWithLock(@Param("productId") Long productId,
                                       @Param("locationId") Long locationId,
                                       @Param("batchNumber") String batchNumber,
                                       @Param("tenantId") Long tenantId);

    List<StockBatch> findByProductIdAndTenantIdOrderByExpiryDateAsc(Long productId, Long tenantId);

    List<StockBatch> findByProductIdAndLocationIdAndTenantIdOrderByExpiryDateAsc(Long productId, Long locationId, Long tenantId);

    @Query("SELECT b FROM StockBatch b WHERE b.tenantId = :tenantId AND b.status = 'ACTIVE' AND " +
           "b.quantity > b.quantityReserved AND b.product.id = :productId AND b.location.id = :locationId " +
           "ORDER BY b.expiryDate ASC NULLS LAST")
    List<StockBatch> findAvailableBatchesFIFO(@Param("tenantId") Long tenantId,
                                               @Param("productId") Long productId,
                                               @Param("locationId") Long locationId);

    @Query("SELECT b FROM StockBatch b WHERE b.tenantId = :tenantId AND b.status = 'ACTIVE' AND " +
           "b.quantity > b.quantityReserved AND b.product.id = :productId AND b.location.id = :locationId " +
           "ORDER BY b.expiryDate DESC NULLS FIRST")
    List<StockBatch> findAvailableBatchesLIFO(@Param("tenantId") Long tenantId,
                                               @Param("productId") Long productId,
                                               @Param("locationId") Long locationId);

    @Query("SELECT b FROM StockBatch b WHERE b.tenantId = :tenantId AND b.status = 'ACTIVE' AND " +
           "b.expiryDate IS NOT NULL AND b.expiryDate <= :expiryDate")
    Page<StockBatch> findExpiringBefore(@Param("tenantId") Long tenantId,
                                         @Param("expiryDate") LocalDate expiryDate,
                                         Pageable pageable);

    @Query("SELECT b FROM StockBatch b WHERE b.tenantId = :tenantId AND b.status = 'ACTIVE' AND " +
           "b.expiryDate IS NOT NULL AND b.expiryDate < CURRENT_DATE")
    Page<StockBatch> findExpired(@Param("tenantId") Long tenantId, Pageable pageable);

    Page<StockBatch> findByTenantIdAndStatus(Long tenantId, StockBatch.BatchStatus status, Pageable pageable);

    @Query("SELECT b FROM StockBatch b WHERE b.tenantId = :tenantId AND " +
           "(LOWER(b.batchNumber) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.product.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(b.product.sku) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<StockBatch> searchBatches(@Param("tenantId") Long tenantId, @Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(b) FROM StockBatch b WHERE b.tenantId = :tenantId AND b.status = 'ACTIVE' AND " +
           "b.expiryDate IS NOT NULL AND b.expiryDate BETWEEN CURRENT_DATE AND :warningDate")
    Long countExpiringWithinDays(@Param("tenantId") Long tenantId, @Param("warningDate") LocalDate warningDate);
}
