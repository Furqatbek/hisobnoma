package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.POSTransactionLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface POSTransactionLineRepository extends JpaRepository<POSTransactionLine, Long> {

    @Query("SELECT l FROM POSTransactionLine l WHERE l.transaction.id = :transactionId ORDER BY l.lineNumber")
    List<POSTransactionLine> findByTransactionId(@Param("transactionId") Long transactionId);

    @Query("SELECT l FROM POSTransactionLine l WHERE l.transaction.id = :transactionId AND l.id = :lineId")
    java.util.Optional<POSTransactionLine> findByIdAndTransactionId(@Param("lineId") Long lineId, @Param("transactionId") Long transactionId);

    @Query("SELECT l FROM POSTransactionLine l WHERE l.product.id = :productId")
    List<POSTransactionLine> findByProductId(@Param("productId") Long productId);

    @Query("SELECT SUM(l.quantity) FROM POSTransactionLine l WHERE l.product.id = :productId " +
           "AND l.transaction.status = 'COMPLETED' AND l.transaction.completedAt >= :startDate AND l.transaction.completedAt < :endDate")
    BigDecimal sumQuantitySoldByProductId(
            @Param("productId") Long productId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT SUM(l.lineTotal) FROM POSTransactionLine l WHERE l.product.id = :productId " +
           "AND l.transaction.status = 'COMPLETED' AND l.transaction.completedAt >= :startDate AND l.transaction.completedAt < :endDate")
    BigDecimal sumSalesByProductId(
            @Param("productId") Long productId,
            @Param("startDate") Instant startDate,
            @Param("endDate") Instant endDate);

    @Query("SELECT MAX(l.lineNumber) FROM POSTransactionLine l WHERE l.transaction.id = :transactionId")
    Integer findMaxLineNumberByTransactionId(@Param("transactionId") Long transactionId);

    @Query("SELECT COALESCE(SUM(l.quantity), 0) FROM POSTransactionLine l " +
           "WHERE l.originalLineId = :originalLineId AND l.isReturn = true " +
           "AND l.transaction.status IN ('PENDING', 'COMPLETED')")
    BigDecimal sumReturnedQuantityByOriginalLineId(@Param("originalLineId") Long originalLineId);

    @Query("SELECT l FROM POSTransactionLine l WHERE l.serialNumber = :serialNumber")
    List<POSTransactionLine> findBySerialNumber(@Param("serialNumber") String serialNumber);

    @Query("SELECT l FROM POSTransactionLine l WHERE l.batchNumber = :batchNumber")
    List<POSTransactionLine> findByBatchNumber(@Param("batchNumber") String batchNumber);
}
