package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
import com.hisobnoma.platform.inventory.entity.MovementType;
import com.hisobnoma.platform.inventory.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByTenantIdOrderByMovementDateDesc(Long tenantId, Pageable pageable);

    Page<StockMovement> findByProductIdAndTenantIdOrderByMovementDateDesc(Long productId, Long tenantId, Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.tenantId = :tenantId AND " +
           "(sm.fromLocation.id = :locationId OR sm.toLocation.id = :locationId) " +
           "ORDER BY sm.movementDate DESC")
    Page<StockMovement> findByLocationId(@Param("tenantId") Long tenantId, @Param("locationId") Long locationId, Pageable pageable);

    Page<StockMovement> findByMovementTypeAndTenantIdOrderByMovementDateDesc(MovementType movementType, Long tenantId, Pageable pageable);

    List<StockMovement> findByReferenceTypeAndReferenceIdAndTenantId(MovementReferenceType referenceType, Long referenceId, Long tenantId);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.tenantId = :tenantId AND " +
           "sm.movementDate BETWEEN :startDate AND :endDate ORDER BY sm.movementDate DESC")
    Page<StockMovement> findByDateRange(@Param("tenantId") Long tenantId,
                                        @Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate,
                                        Pageable pageable);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.tenantId = :tenantId AND sm.product.id = :productId AND " +
           "sm.movementDate BETWEEN :startDate AND :endDate ORDER BY sm.movementDate DESC")
    List<StockMovement> findByProductAndDateRange(@Param("tenantId") Long tenantId,
                                                   @Param("productId") Long productId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(CASE WHEN sm.movementType IN ('STOCK_IN', 'RETURN', 'PRODUCTION', 'INITIAL') THEN sm.quantity " +
           "WHEN sm.movementType = 'ADJUSTMENT' AND sm.quantity > 0 THEN sm.quantity ELSE 0 END) " +
           "FROM StockMovement sm WHERE sm.product.id = :productId AND sm.tenantId = :tenantId " +
           "AND sm.movementDate BETWEEN :startDate AND :endDate")
    BigDecimal sumIncomingQuantity(@Param("productId") Long productId,
                                    @Param("tenantId") Long tenantId,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(CASE WHEN sm.movementType IN ('STOCK_OUT', 'CONSUMPTION', 'WRITE_OFF') THEN sm.quantity " +
           "WHEN sm.movementType = 'ADJUSTMENT' AND sm.quantity < 0 THEN ABS(sm.quantity) ELSE 0 END) " +
           "FROM StockMovement sm WHERE sm.product.id = :productId AND sm.tenantId = :tenantId " +
           "AND sm.movementDate BETWEEN :startDate AND :endDate")
    BigDecimal sumOutgoingQuantity(@Param("productId") Long productId,
                                    @Param("tenantId") Long tenantId,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate);

    @Query("SELECT sm FROM StockMovement sm WHERE sm.tenantId = :tenantId AND " +
           "sm.reversed = false AND sm.reversalMovementId IS NULL AND " +
           "sm.movementDate >= :since ORDER BY sm.movementDate DESC")
    List<StockMovement> findRecentUnreversed(@Param("tenantId") Long tenantId, @Param("since") LocalDateTime since);

    // Inventory Planning queries
    @Query("SELECT SUM(sm.quantity * sm.unitCost) FROM StockMovement sm WHERE sm.product.id = :productId " +
           "AND sm.tenantId = :tenantId AND sm.createdAt >= :startDate AND sm.createdAt <= :endDate " +
           "AND sm.movementType IN ('STOCK_OUT', 'CONSUMPTION')")
    BigDecimal sumMovementValueByProductAndDateRange(@Param("productId") Long productId,
                                                      @Param("tenantId") Long tenantId,
                                                      @Param("startDate") Instant startDate,
                                                      @Param("endDate") Instant endDate);

    @Query("SELECT MAX(sm.createdAt) FROM StockMovement sm WHERE sm.product.id = :productId " +
           "AND sm.tenantId = :tenantId AND sm.movementType IN ('STOCK_OUT', 'CONSUMPTION')")
    Instant findLastMovementDateByProduct(@Param("productId") Long productId, @Param("tenantId") Long tenantId);

    @Query("SELECT CASE WHEN COUNT(sm) > 0 THEN true ELSE false END FROM StockMovement sm " +
           "WHERE sm.product.id = :productId AND sm.tenantId = :tenantId AND sm.createdAt > :cutoffDate")
    boolean existsByProductIdAndTenantIdAndCreatedAtAfter(@Param("productId") Long productId,
                                                           @Param("tenantId") Long tenantId,
                                                           @Param("cutoffDate") Instant cutoffDate);
}
