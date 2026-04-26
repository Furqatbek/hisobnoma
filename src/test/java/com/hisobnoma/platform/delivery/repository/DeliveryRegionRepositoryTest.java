package com.hisobnoma.platform.delivery.repository;

import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
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
class DeliveryRegionRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private DeliveryRegionRepository deliveryRegionRepository;

    private static final Long TENANT_ID = 1L;

    private DeliveryRegion persistRegion(String name, String code, boolean active, int sortOrder) {
        DeliveryRegion r = DeliveryRegion.builder()
                .name(name).code(code).active(active).sortOrder(sortOrder).build();
        r.setTenantId(TENANT_ID);
        return em.persistAndFlush(r);
    }

    @Test
    void findByTenantId_returnsPagedResults() {
        persistRegion("Tashkent", "TASH", true, 1);
        persistRegion("Samarkand", "SAM", true, 2);

        Page<DeliveryRegion> result = deliveryRegionRepository.findByTenantId(TENANT_ID, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
    }

    @Test
    void findByIdAndTenantId_existing_returnsRegion() {
        DeliveryRegion r = persistRegion("Tashkent", "TASH", true, 1);

        Optional<DeliveryRegion> result = deliveryRegionRepository.findByIdAndTenantId(r.getId(), TENANT_ID);

        assertTrue(result.isPresent());
        assertEquals("Tashkent", result.get().getName());
    }

    @Test
    void findByIdAndTenantId_wrongTenant_returnsEmpty() {
        DeliveryRegion r = persistRegion("Tashkent", "TASH", true, 1);

        Optional<DeliveryRegion> result = deliveryRegionRepository.findByIdAndTenantId(r.getId(), 999L);

        assertTrue(result.isEmpty());
    }

    @Test
    void findActiveByTenantId_returnsOnlyActive() {
        persistRegion("Tashkent", "TASH", true, 1);
        persistRegion("Bukhara", "BUK", false, 2);
        persistRegion("Samarkand", "SAM", true, 3);

        List<DeliveryRegion> result = deliveryRegionRepository.findActiveByTenantId(TENANT_ID);

        assertEquals(2, result.size());
        result.forEach(r -> assertTrue(r.isActive()));
    }

    @Test
    void searchByTenantId_matchesByName() {
        persistRegion("Tashkent", "TASH", true, 1);
        persistRegion("Samarkand", "SAM", true, 2);

        Page<DeliveryRegion> result = deliveryRegionRepository.searchByTenantId("tash", TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Tashkent", result.getContent().get(0).getName());
    }

    @Test
    void searchByTenantId_matchesByCode() {
        persistRegion("Tashkent", "TASH", true, 1);
        persistRegion("Samarkand", "SAM", true, 2);

        Page<DeliveryRegion> result = deliveryRegionRepository.searchByTenantId("SAM", TENANT_ID, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals("Samarkand", result.getContent().get(0).getName());
    }

    @Test
    void existsByCodeAndTenantId_existing_returnsTrue() {
        persistRegion("Tashkent", "TASH", true, 1);

        assertTrue(deliveryRegionRepository.existsByCodeAndTenantId("TASH", TENANT_ID, null));
        assertFalse(deliveryRegionRepository.existsByCodeAndTenantId("NOPE", TENANT_ID, null));
    }

    @Test
    void existsByCodeAndTenantId_excludesOwnId() {
        DeliveryRegion r = persistRegion("Tashkent", "TASH", true, 1);

        assertFalse(deliveryRegionRepository.existsByCodeAndTenantId("TASH", TENANT_ID, r.getId()));
    }
}
