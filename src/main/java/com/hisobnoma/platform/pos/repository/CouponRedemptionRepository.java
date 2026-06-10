package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.CouponRedemption;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Repository
public interface CouponRedemptionRepository extends JpaRepository<CouponRedemption, Long> {

    List<CouponRedemption> findByCouponId(Long couponId);

    Page<CouponRedemption> findByCouponId(Long couponId, Pageable pageable);

    List<CouponRedemption> findByCustomerId(Long customerId);

    List<CouponRedemption> findByTransactionId(Long transactionId);

    List<CouponRedemption> findByWebOrderIdAndReversedFalse(Long webOrderId);

    @Query("SELECT COUNT(cr) FROM CouponRedemption cr " +
           "WHERE cr.coupon.id = :couponId AND cr.customerId = :customerId AND cr.reversed = false")
    int countByCustomerAndCoupon(@Param("couponId") Long couponId, @Param("customerId") Long customerId);

    @Query("SELECT COUNT(cr) FROM CouponRedemption cr " +
           "WHERE cr.coupon.id = :couponId AND cr.customerEmail = :email AND cr.reversed = false")
    int countByEmailAndCoupon(@Param("couponId") Long couponId, @Param("email") String email);

    @Query("SELECT COALESCE(SUM(cr.discountAmount), 0) FROM CouponRedemption cr " +
           "WHERE cr.coupon.id = :couponId AND cr.reversed = false")
    BigDecimal sumDiscountByCouponId(@Param("couponId") Long couponId);

    @Query("SELECT cr FROM CouponRedemption cr WHERE cr.coupon.id = :couponId " +
           "AND cr.redeemedAt BETWEEN :start AND :end")
    List<CouponRedemption> findByCouponIdAndDateRange(
            @Param("couponId") Long couponId,
            @Param("start") Instant start,
            @Param("end") Instant end);
}
