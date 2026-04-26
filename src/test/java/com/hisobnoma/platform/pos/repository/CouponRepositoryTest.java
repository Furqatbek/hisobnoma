package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.CouponStatus;
import com.hisobnoma.platform.pos.enums.PromotionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class CouponRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private CouponRepository couponRepository;

    private static final Long TENANT_ID = 1L;

    private Promotion promotion;

    @BeforeEach
    void setUp() {
        promotion = Promotion.builder()
                .code("PROMO1").name("Test Promo").type(PromotionType.PERCENTAGE_OFF)
                .active(true).requiresCoupon(true).build();
        promotion.setTenantId(TENANT_ID);
        promotion = em.persistAndFlush(promotion);
    }

    private Coupon persistCoupon(String code, CouponStatus status, LocalDate startDate,
                                  LocalDate endDate, Integer maxUses, Integer currentUses,
                                  Long customerId) {
        Coupon c = Coupon.builder()
                .code(code).promotion(promotion).status(status)
                .description("Coupon " + code)
                .startDate(startDate).endDate(endDate)
                .maxUses(maxUses).currentUses(currentUses)
                .customerId(customerId)
                .build();
        c.setTenantId(TENANT_ID);
        return em.persistAndFlush(c);
    }

    @Test
    void findByIdAndTenantId_existing_returnsCoupon() {
        Coupon c = persistCoupon("CPN1", CouponStatus.ACTIVE, null, null, null, 0, null);

        Optional<Coupon> result = couponRepository.findByIdAndTenantId(c.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("CPN1", result.get().getCode());
    }

    @Test
    void findByCodeAndTenantId_existing_returnsCoupon() {
        persistCoupon("CPN-FIND", CouponStatus.ACTIVE, null, null, null, 0, null);

        Optional<Coupon> result = couponRepository.findByCodeAndTenantId("CPN-FIND", TENANT_ID);

        assertTrue(result.isPresent());
    }

    @Test
    void findByTenantId_returnsPagedResults() {
        persistCoupon("CPN1", CouponStatus.ACTIVE, null, null, null, 0, null);
        persistCoupon("CPN2", CouponStatus.EXPIRED, null, null, null, 0, null);

        Page<Coupon> result = couponRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void searchByTenantId_matchesByCodeOrDescription() {
        persistCoupon("SUMMER-10", CouponStatus.ACTIVE, null, null, null, 0, null);
        persistCoupon("WINTER-20", CouponStatus.ACTIVE, null, null, null, 0, null);

        Page<Coupon> result = couponRepository.searchByTenantId("SUMMER", TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByTenantIdAndStatus_filtersCorrectly() {
        persistCoupon("CPN1", CouponStatus.ACTIVE, null, null, null, 0, null);
        persistCoupon("CPN2", CouponStatus.EXPIRED, null, null, null, 0, null);
        persistCoupon("CPN3", CouponStatus.ACTIVE, null, null, null, 0, null);

        List<Coupon> result = couponRepository.findByTenantIdAndStatus(TENANT_ID, CouponStatus.ACTIVE);

        assertEquals(2, result.size());
    }

    @Test
    void findByPromotionId_returnsCouponsForPromotion() {
        persistCoupon("CPN1", CouponStatus.ACTIVE, null, null, null, 0, null);
        persistCoupon("CPN2", CouponStatus.ACTIVE, null, null, null, 0, null);

        List<Coupon> result = couponRepository.findByPromotionId(promotion.getId());

        assertEquals(2, result.size());
    }

    @Test
    void findValidCoupon_returnsActiveValidCoupon() {
        LocalDate today = LocalDate.now();
        persistCoupon("VALID", CouponStatus.ACTIVE, today.minusDays(5), today.plusDays(5), 10, 0, null);
        persistCoupon("EXPIRED", CouponStatus.ACTIVE, today.minusDays(30), today.minusDays(1), null, 0, null);
        persistCoupon("INACTIVE", CouponStatus.INACTIVE, null, null, null, 0, null);

        Optional<Coupon> result = couponRepository.findValidCoupon("VALID", TENANT_ID, today);

        assertTrue(result.isPresent());
        assertEquals("VALID", result.get().getCode());
    }

    @Test
    void findValidCoupon_depleted_returnsEmpty() {
        LocalDate today = LocalDate.now();
        persistCoupon("DEPLETED", CouponStatus.ACTIVE, null, null, 5, 5, null);

        Optional<Coupon> result = couponRepository.findValidCoupon("DEPLETED", TENANT_ID, today);

        assertTrue(result.isEmpty());
    }

    @Test
    void findValidCouponForCustomer_customerSpecific_returnsForMatchingCustomer() {
        LocalDate today = LocalDate.now();
        persistCoupon("CUST-CPN", CouponStatus.ACTIVE, null, null, null, 0, 100L);

        Optional<Coupon> forCustomer100 = couponRepository.findValidCouponForCustomer("CUST-CPN", TENANT_ID, 100L, today);
        Optional<Coupon> forCustomer200 = couponRepository.findValidCouponForCustomer("CUST-CPN", TENANT_ID, 200L, today);

        assertTrue(forCustomer100.isPresent());
        assertTrue(forCustomer200.isEmpty());
    }

    @Test
    void existsByCodeAndTenantId_existing_returnsTrue() {
        persistCoupon("CPN-EXISTS", CouponStatus.ACTIVE, null, null, null, 0, null);

        assertTrue(couponRepository.existsByCodeAndTenantId("CPN-EXISTS", TENANT_ID));
        assertFalse(couponRepository.existsByCodeAndTenantId("CPN-NOPE", TENANT_ID));
    }

    @Test
    void findExpiredActiveCoupons_returnsExpiredOnes() {
        LocalDate today = LocalDate.now();
        persistCoupon("EXP1", CouponStatus.ACTIVE, null, today.minusDays(1), null, 0, null);
        persistCoupon("VALID", CouponStatus.ACTIVE, null, today.plusDays(10), null, 0, null);

        List<Coupon> result = couponRepository.findExpiredActiveCoupons(TENANT_ID, today);

        assertEquals(1, result.size());
        assertEquals("EXP1", result.get(0).getCode());
    }

    @Test
    void findDepletedActiveCoupons_returnsDepletedOnes() {
        persistCoupon("DEP1", CouponStatus.ACTIVE, null, null, 5, 5, null);
        persistCoupon("AVAIL", CouponStatus.ACTIVE, null, null, 10, 3, null);
        persistCoupon("NOLIMIT", CouponStatus.ACTIVE, null, null, null, 0, null);

        List<Coupon> result = couponRepository.findDepletedActiveCoupons(TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("DEP1", result.get(0).getCode());
    }

    @Test
    void countActiveByTenantId_returnsCount() {
        persistCoupon("CPN1", CouponStatus.ACTIVE, null, null, null, 0, null);
        persistCoupon("CPN2", CouponStatus.ACTIVE, null, null, null, 0, null);
        persistCoupon("CPN3", CouponStatus.EXPIRED, null, null, null, 0, null);

        long result = couponRepository.countActiveByTenantId(TENANT_ID);

        assertEquals(2L, result);
    }
}
