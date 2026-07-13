package com.hisobnoma.platform.mobile.repository;

import com.hisobnoma.platform.mobile.entity.MobileQuickSaleIdempotency;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates the quick-sale dedup guarantee against H2: the tenant-scoped finder and the
 * unique (tenant_id, client_request_id) constraint that stops a concurrent duplicate persisting.
 */
@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class MobileQuickSaleIdempotencyRepositoryTest {

    @Autowired private TestEntityManager em;
    @Autowired private MobileQuickSaleIdempotencyRepository repository;

    private MobileQuickSaleIdempotency record(Long tenantId, String clientRequestId, Long transactionId) {
        MobileQuickSaleIdempotency r = MobileQuickSaleIdempotency.builder()
                .clientRequestId(clientRequestId).transactionId(transactionId).tenantId(tenantId).build();
        return r;
    }

    @Test
    void findByTenantIdAndClientRequestId_scopesByTenant() {
        em.persistAndFlush(record(1L, "uuid-A", 500L));
        em.persistAndFlush(record(2L, "uuid-A", 999L)); // same key, other tenant

        Optional<MobileQuickSaleIdempotency> hit = repository.findByTenantIdAndClientRequestId(1L, "uuid-A");
        assertTrue(hit.isPresent());
        assertEquals(500L, hit.get().getTransactionId());

        assertTrue(repository.findByTenantIdAndClientRequestId(1L, "uuid-UNKNOWN").isEmpty());
        assertEquals(999L, repository.findByTenantIdAndClientRequestId(2L, "uuid-A").get().getTransactionId());
    }

    @Test
    void duplicateKeyForSameTenant_isRejected() {
        em.persistAndFlush(record(1L, "uuid-B", 500L));
        assertThrows(Exception.class, () -> em.persistAndFlush(record(1L, "uuid-B", 501L)));
    }
}
