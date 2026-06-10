package com.hisobnoma.platform.pos.repository;

import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.PromotionType;
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
class PromotionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private PromotionRepository promotionRepository;

    private static final Long TENANT_ID = 1L;

    private Promotion persistPromotion(String code, String name, PromotionType type,
                                        boolean active, boolean requiresCoupon,
                                        LocalDate startDate, LocalDate endDate,
                                        int priority, Integer maxUses, Integer currentUses,
                                        Long locationId) {
        Promotion p = Promotion.builder()
                .code(code).name(name).type(type)
                .active(active).requiresCoupon(requiresCoupon)
                .startDate(startDate).endDate(endDate)
                .priority(priority).maxUses(maxUses).currentUses(currentUses)
                .locationId(locationId)
                .build();
        p.setTenantId(TENANT_ID);
        return em.persistAndFlush(p);
    }

    @Test
    void findByIdAndTenantId_existing_returnsPromotion() {
        Promotion p = persistPromotion("PROMO1", "Summer Sale", PromotionType.PERCENTAGE_OFF,
                true, false, null, null, 0, null, 0, null);

        Optional<Promotion> result = promotionRepository.findByIdAndTenantId(p.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("PROMO1", result.get().getCode());
    }

    @Test
    void findByCodeAndTenantId_existing_returnsPromotion() {
        persistPromotion("PROMO-FIND", "Find Me", PromotionType.FIXED_AMOUNT_OFF,
                true, false, null, null, 0, null, 0, null);

        Optional<Promotion> result = promotionRepository.findByCodeAndTenantId("PROMO-FIND", TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Find Me", result.get().getName());
    }

    @Test
    void findByTenantId_returnsPagedResults() {
        persistPromotion("P1", "Promo 1", PromotionType.PERCENTAGE_OFF, true, false, null, null, 0, null, 0, null);
        persistPromotion("P2", "Promo 2", PromotionType.FIXED_AMOUNT_OFF, true, false, null, null, 0, null, 0, null);

        Page<Promotion> result = promotionRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void searchByTenantId_matchesByCodeOrName() {
        persistPromotion("SUMMER-SALE", "Summer Sale", PromotionType.PERCENTAGE_OFF, true, false, null, null, 0, null, 0, null);
        persistPromotion("WINTER-SALE", "Winter Sale", PromotionType.PERCENTAGE_OFF, true, false, null, null, 0, null, 0, null);

        Page<Promotion> result = promotionRepository.searchByTenantId("summer", TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void findByTenantIdAndActive_filtersCorrectly() {
        persistPromotion("P1", "Active", PromotionType.PERCENTAGE_OFF, true, false, null, null, 0, null, 0, null);
        persistPromotion("P2", "Inactive", PromotionType.PERCENTAGE_OFF, false, false, null, null, 0, null, 0, null);

        List<Promotion> result = promotionRepository.findByTenantIdAndActive(TENANT_ID, true);

        assertEquals(1, result.size());
        assertEquals("Active", result.get(0).getName());
    }

    @Test
    void findByTenantIdAndType_filtersCorrectly() {
        persistPromotion("P1", "Percent Off", PromotionType.PERCENTAGE_OFF, true, false, null, null, 0, null, 0, null);
        persistPromotion("P2", "Fixed Off", PromotionType.FIXED_AMOUNT_OFF, true, false, null, null, 0, null, 0, null);

        List<Promotion> result = promotionRepository.findByTenantIdAndType(TENANT_ID, PromotionType.FIXED_AMOUNT_OFF);

        assertEquals(1, result.size());
        assertEquals("Fixed Off", result.get(0).getName());
    }

    @Test
    void findActivePromotions_filtersNoCouponActiveAndDateAndLocation() {
        LocalDate today = LocalDate.now();
        // Active, no coupon, valid date, matching location
        persistPromotion("P1", "Valid", PromotionType.PERCENTAGE_OFF, true, false,
                today.minusDays(5), today.plusDays(5), 1, null, 0, 100L);
        // Requires coupon
        persistPromotion("P2", "Coupon Only", PromotionType.PERCENTAGE_OFF, true, true,
                null, null, 0, null, 0, null);
        // Expired
        persistPromotion("P3", "Expired", PromotionType.PERCENTAGE_OFF, true, false,
                today.minusDays(30), today.minusDays(1), 0, null, 0, null);
        // Different location
        persistPromotion("P4", "Other Loc", PromotionType.PERCENTAGE_OFF, true, false,
                null, null, 0, null, 0, 200L);

        List<Promotion> result = promotionRepository.findActivePromotions(TENANT_ID, today, 100L);

        assertEquals(1, result.size());
        assertEquals("Valid", result.get(0).getName());
    }

    @Test
    void findActivePromotionsForChannels_filtersByChannel() {
        LocalDate today = LocalDate.now();
        Promotion posPromo = persistPromotion("POS-P", "Pos Only", PromotionType.PERCENTAGE_OFF,
                true, false, null, null, 0, null, 0, null); // default channel POS
        Promotion webPromo = Promotion.builder()
                .code("WEB-P").name("Web Only").type(PromotionType.PERCENTAGE_OFF)
                .channel(com.hisobnoma.platform.pos.enums.PromotionChannel.WEB)
                .build();
        webPromo.setTenantId(TENANT_ID);
        em.persistAndFlush(webPromo);
        Promotion allPromo = Promotion.builder()
                .code("ALL-P").name("Everywhere").type(PromotionType.PERCENTAGE_OFF)
                .channel(com.hisobnoma.platform.pos.enums.PromotionChannel.ALL)
                .build();
        allPromo.setTenantId(TENANT_ID);
        em.persistAndFlush(allPromo);

        List<Promotion> webResult = promotionRepository.findActivePromotionsForChannels(
                TENANT_ID, today, null,
                List.of(com.hisobnoma.platform.pos.enums.PromotionChannel.WEB,
                        com.hisobnoma.platform.pos.enums.PromotionChannel.ALL));
        List<Promotion> posResult = promotionRepository.findActivePromotionsForChannels(
                TENANT_ID, today, null,
                List.of(com.hisobnoma.platform.pos.enums.PromotionChannel.POS,
                        com.hisobnoma.platform.pos.enums.PromotionChannel.ALL));

        assertEquals(List.of("WEB-P", "ALL-P"),
                webResult.stream().map(Promotion::getCode).sorted(java.util.Comparator.reverseOrder()).toList());
        assertEquals(2, posResult.size());
        assertTrue(posResult.stream().anyMatch(p -> p.getCode().equals("POS-P")));
        assertTrue(posResult.stream().anyMatch(p -> p.getCode().equals("ALL-P")));
        assertTrue(posResult.stream().noneMatch(p -> p.getCode().equals("WEB-P")));
    }

    @Test
    void findActivePromotionsForChannels_isolatesTenants() {
        Promotion other = Promotion.builder()
                .code("OTHER-T").name("Other Tenant").type(PromotionType.PERCENTAGE_OFF)
                .channel(com.hisobnoma.platform.pos.enums.PromotionChannel.WEB)
                .build();
        other.setTenantId(999L);
        em.persistAndFlush(other);

        List<Promotion> result = promotionRepository.findActivePromotionsForChannels(
                TENANT_ID, LocalDate.now(), null,
                List.of(com.hisobnoma.platform.pos.enums.PromotionChannel.WEB,
                        com.hisobnoma.platform.pos.enums.PromotionChannel.ALL));

        assertTrue(result.isEmpty());
    }

    @Test
    void findAllActivePromotions_filtersActiveAndDate() {
        LocalDate today = LocalDate.now();
        persistPromotion("P1", "Active 1", PromotionType.PERCENTAGE_OFF, true, false,
                null, null, 1, null, 0, null);
        persistPromotion("P2", "Active 2", PromotionType.PERCENTAGE_OFF, true, true,
                null, null, 0, null, 0, null);
        persistPromotion("P3", "Inactive", PromotionType.PERCENTAGE_OFF, false, false,
                null, null, 0, null, 0, null);

        List<Promotion> result = promotionRepository.findAllActivePromotions(TENANT_ID, today);

        assertEquals(2, result.size());
    }

    @Test
    void existsByCodeAndTenantId_existing_returnsTrue() {
        persistPromotion("P-EXISTS", "Exists", PromotionType.PERCENTAGE_OFF, true, false, null, null, 0, null, 0, null);

        assertTrue(promotionRepository.existsByCodeAndTenantId("P-EXISTS", TENANT_ID));
        assertFalse(promotionRepository.existsByCodeAndTenantId("P-NOPE", TENANT_ID));
    }

    @Test
    void countActiveByTenantId_returnsCount() {
        persistPromotion("P1", "Active 1", PromotionType.PERCENTAGE_OFF, true, false, null, null, 0, null, 0, null);
        persistPromotion("P2", "Active 2", PromotionType.FIXED_AMOUNT_OFF, true, false, null, null, 0, null, 0, null);
        persistPromotion("P3", "Inactive", PromotionType.PERCENTAGE_OFF, false, false, null, null, 0, null, 0, null);

        long result = promotionRepository.countActiveByTenantId(TENANT_ID);

        assertEquals(2L, result);
    }

    @Test
    void findExpiredPromotions_returnsExpiredActiveOnes() {
        LocalDate today = LocalDate.now();
        persistPromotion("P1", "Expired Active", PromotionType.PERCENTAGE_OFF, true, false,
                today.minusDays(30), today.minusDays(1), 0, null, 0, null);
        persistPromotion("P2", "Still Active", PromotionType.PERCENTAGE_OFF, true, false,
                null, today.plusDays(10), 0, null, 0, null);

        List<Promotion> result = promotionRepository.findExpiredPromotions(TENANT_ID, today);

        assertEquals(1, result.size());
        assertEquals("Expired Active", result.get(0).getName());
    }
}
