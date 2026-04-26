package com.hisobnoma.platform.delivery.repository;

import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class DeliveryVillageRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private DeliveryVillageRepository deliveryVillageRepository;

    private static final Long TENANT_ID = 1L;

    private DeliveryRegion region1;
    private DeliveryRegion region2;

    @BeforeEach
    void setUp() {
        region1 = DeliveryRegion.builder().name("Tashkent").code("TASH").active(true).sortOrder(1).build();
        region1.setTenantId(TENANT_ID);
        region1 = em.persistAndFlush(region1);

        region2 = DeliveryRegion.builder().name("Samarkand").code("SAM").active(true).sortOrder(2).build();
        region2.setTenantId(TENANT_ID);
        region2 = em.persistAndFlush(region2);
    }

    private DeliveryVillage persistVillage(String name, String code, DeliveryRegion region,
                                            boolean active, int sortOrder) {
        DeliveryVillage v = DeliveryVillage.builder()
                .name(name).code(code).region(region)
                .active(active).sortOrder(sortOrder).build();
        v.setTenantId(TENANT_ID);
        return em.persistAndFlush(v);
    }

    @Test
    void findByTenantId_returnsPagedResults() {
        persistVillage("Chilanzar", "CHI", region1, true, 1);
        persistVillage("Yunusabad", "YUN", region1, true, 2);

        Page<DeliveryVillage> result = deliveryVillageRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByIdAndTenantId_existing_returnsVillage() {
        DeliveryVillage v = persistVillage("Chilanzar", "CHI", region1, true, 1);

        Optional<DeliveryVillage> result = deliveryVillageRepository.findByIdAndTenantId(v.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Chilanzar", result.get().getName());
    }

    @Test
    void findActiveByRegionIdAndTenantId_returnsActiveVillagesInRegion() {
        persistVillage("Chilanzar", "CHI", region1, true, 1);
        persistVillage("Yunusabad", "YUN", region1, false, 2);
        persistVillage("Samarkand Central", "SC", region2, true, 1);

        List<DeliveryVillage> result = deliveryVillageRepository.findActiveByRegionIdAndTenantId(
                region1.getId(), TENANT_ID);

        assertEquals(1, result.size());
        assertEquals("Chilanzar", result.get(0).getName());
    }

    @Test
    void findActiveByTenantId_returnsAllActiveVillages() {
        persistVillage("Chilanzar", "CHI", region1, true, 1);
        persistVillage("Yunusabad", "YUN", region1, true, 2);
        persistVillage("Inactive", "INV", region1, false, 3);

        List<DeliveryVillage> result = deliveryVillageRepository.findActiveByTenantId(TENANT_ID);

        assertEquals(2, result.size());
    }

    @Test
    void searchByTenantId_matchesByName() {
        persistVillage("Chilanzar", "CHI", region1, true, 1);
        persistVillage("Yunusabad", "YUN", region1, true, 2);

        Page<DeliveryVillage> result = deliveryVillageRepository.searchByTenantId("chil", TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Chilanzar", result.getContent().get(0).getName());
    }

    @Test
    void searchByTenantId_matchesByCode() {
        persistVillage("Chilanzar", "CHI", region1, true, 1);
        persistVillage("Yunusabad", "YUN", region1, true, 2);

        Page<DeliveryVillage> result = deliveryVillageRepository.searchByTenantId("YUN", TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void existsByCodeAndTenantId_existing_returnsTrue() {
        persistVillage("Chilanzar", "CHI", region1, true, 1);

        assertTrue(deliveryVillageRepository.existsByCodeAndTenantId("CHI", TENANT_ID, null));
        assertFalse(deliveryVillageRepository.existsByCodeAndTenantId("NOPE", TENANT_ID, null));
    }

    @Test
    void existsByCodeAndTenantId_excludesOwnId() {
        DeliveryVillage v = persistVillage("Chilanzar", "CHI", region1, true, 1);

        assertFalse(deliveryVillageRepository.existsByCodeAndTenantId("CHI", TENANT_ID, v.getId()));
    }

    @Test
    void countByRegionId_returnsCount() {
        persistVillage("Chilanzar", "CHI", region1, true, 1);
        persistVillage("Yunusabad", "YUN", region1, true, 2);
        persistVillage("Samarkand Central", "SC", region2, true, 1);

        long result = deliveryVillageRepository.countByRegionId(region1.getId());

        assertEquals(2L, result);
    }
}
