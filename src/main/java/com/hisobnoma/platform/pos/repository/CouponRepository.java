package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.enums.CouponStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByIdAndTenantId(Long id, Long tenantId);

    Optional<Coupon> findByCodeAndTenantId(String code, Long tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.tenantId = :tenantId")
    Optional<Coupon> findByCodeAndTenantIdForUpdate(@Param("code") String code, @Param("tenantId") Long tenantId);

    Page<Coupon> findByTenantId(Long tenantId, Pageable pageable);

    @Query("SELECT c FROM Coupon c WHERE c.tenantId = :tenantId " +
           "AND (LOWER(c.code) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(c.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Coupon> searchByTenantId(@Param("query") String query, @Param("tenantId") Long tenantId, Pageable pageable);

    List<Coupon> findByTenantIdAndStatus(Long tenantId, CouponStatus status);

    Page<Coupon> findByTenantIdAndStatus(Long tenantId, CouponStatus status, Pageable pageable);

    List<Coupon> findByPromotionId(Long promotionId);

    Page<Coupon> findByPromotionId(Long promotionId, Pageable pageable);

    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.tenantId = :tenantId " +
           "AND c.status = 'ACTIVE' " +
           "AND (c.startDate IS NULL OR c.startDate <= :date) " +
           "AND (c.endDate IS NULL OR c.endDate >= :date) " +
           "AND (c.maxUses IS NULL OR c.currentUses < c.maxUses)")
    Optional<Coupon> findValidCoupon(
            @Param("code") String code,
            @Param("tenantId") Long tenantId,
            @Param("date") LocalDate date);

    @Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.tenantId = :tenantId " +
           "AND c.status = 'ACTIVE' " +
           "AND (c.customerId IS NULL OR c.customerId = :customerId) " +
           "AND (c.startDate IS NULL OR c.startDate <= :date) " +
           "AND (c.endDate IS NULL OR c.endDate >= :date) " +
           "AND (c.maxUses IS NULL OR c.currentUses < c.maxUses)")
    Optional<Coupon> findValidCouponForCustomer(
            @Param("code") String code,
            @Param("tenantId") Long tenantId,
            @Param("customerId") Long customerId,
            @Param("date") LocalDate date);

    boolean existsByCodeAndTenantId(String code, Long tenantId);

    @Query("SELECT c FROM Coupon c WHERE c.tenantId = :tenantId " +
           "AND c.endDate < :date AND c.status = 'ACTIVE'")
    List<Coupon> findExpiredActiveCoupons(@Param("tenantId") Long tenantId, @Param("date") LocalDate date);

    @Query("SELECT c FROM Coupon c WHERE c.tenantId = :tenantId " +
           "AND c.status = 'ACTIVE' AND c.maxUses IS NOT NULL AND c.currentUses >= c.maxUses")
    List<Coupon> findDepletedActiveCoupons(@Param("tenantId") Long tenantId);

    @Query("SELECT COUNT(c) FROM Coupon c WHERE c.tenantId = :tenantId AND c.status = 'ACTIVE'")
    long countActiveByTenantId(@Param("tenantId") Long tenantId);
}
