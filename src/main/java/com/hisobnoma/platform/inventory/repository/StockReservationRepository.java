package com.hisobnoma.platform.inventory.repository;

import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
import com.hisobnoma.platform.inventory.entity.StockReservation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

    List<StockReservation> findByProductIdAndLocationIdAndTenantIdAndStatus(
            Long productId, Long locationId, Long tenantId, StockReservation.ReservationStatus status);

    List<StockReservation> findByReferenceTypeAndReferenceIdAndTenantId(
            MovementReferenceType referenceType, Long referenceId, Long tenantId);

    Optional<StockReservation> findByProductIdAndLocationIdAndReferenceTypeAndReferenceIdAndTenantId(
            Long productId, Long locationId, MovementReferenceType referenceType, Long referenceId, Long tenantId);

    @Query("SELECT COALESCE(SUM(r.quantity), 0) FROM StockReservation r WHERE r.product.id = :productId " +
           "AND r.location.id = :locationId AND r.tenantId = :tenantId AND r.status = 'ACTIVE'")
    BigDecimal sumActiveReservations(@Param("productId") Long productId,
                                      @Param("locationId") Long locationId,
                                      @Param("tenantId") Long tenantId);

    @Query("SELECT r FROM StockReservation r WHERE r.tenantId = :tenantId AND r.status = 'ACTIVE' " +
           "AND r.expiresAt IS NOT NULL AND r.expiresAt < :now")
    List<StockReservation> findExpiredReservations(@Param("tenantId") Long tenantId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE StockReservation r SET r.status = 'EXPIRED' WHERE r.status = 'ACTIVE' " +
           "AND r.expiresAt IS NOT NULL AND r.expiresAt < :now")
    int expireReservations(@Param("now") LocalDateTime now);

    Page<StockReservation> findByTenantIdAndStatus(Long tenantId, StockReservation.ReservationStatus status, Pageable pageable);

    @Query("SELECT r FROM StockReservation r WHERE r.tenantId = :tenantId AND r.status = 'ACTIVE' " +
           "AND r.product.id = :productId ORDER BY r.reservedAt ASC")
    List<StockReservation> findActiveByProduct(@Param("tenantId") Long tenantId, @Param("productId") Long productId);
}
