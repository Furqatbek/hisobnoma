package com.hisobnoma.platform.web.repository;

import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebOrder;
import com.hisobnoma.platform.web.entity.WebOrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the campaign segment queries against seeded customers + orders.
 */
@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class WebCustomerSegmentTest {

    @Autowired private TestEntityManager em;
    @Autowired private WebCustomerRepository customerRepository;

    private static final Long TENANT = 1L;
    private static final Long OTHER_TENANT = 2L;

    private Instant now;
    private Instant cutoff;       // now - 30 days
    private Instant longAgo;      // now - 60 days

    @BeforeEach
    void setUp() {
        now = Instant.now();
        cutoff = now.minus(30, ChronoUnit.DAYS);
        longAgo = now.minus(60, ChronoUnit.DAYS);
    }

    private WebCustomer customer(String phone, Long tenant, boolean optOut) {
        return em.persistAndFlush(WebCustomer.builder()
                .phone(phone).name("C" + phone).verifiedAt(now)
                .smsOptOut(optOut).tenantId(tenant).build());
    }

    private void order(String phone, Long tenant, WebOrderStatus status, String amount, Instant createdAt) {
        WebOrder order = em.persistAndFlush(WebOrder.builder()
                .orderNumber("WO-" + phone + "-" + System.nanoTime())
                .status(status).customerName("C").phone(phone).phoneNormalized(phone)
                .totalAmount(new BigDecimal(amount)).tenantId(tenant).build());
        // Backdate created_at past JPA auditing to simulate order age
        em.getEntityManager().createNativeQuery(
                        "UPDATE \"web_orders\" SET \"created_at\" = :ts WHERE \"id\" = :id")
                .setParameter("ts", Timestamp.from(createdAt))
                .setParameter("id", order.getId())
                .executeUpdate();
        em.clear();
    }

    @Test
    void segments_partitionCustomersCorrectly() {
        // A: recent order (NEW)
        customer("998900000001", TENANT, false);
        order("998900000001", TENANT, WebOrderStatus.NEW, "12000", now);
        // B: old completed order (60 days ago, 50000)
        customer("998900000002", TENANT, false);
        order("998900000002", TENANT, WebOrderStatus.COMPLETED, "50000", longAgo);
        // C: never ordered
        customer("998900000003", TENANT, false);
        // D: opted out, recent order
        customer("998900000004", TENANT, true);
        order("998900000004", TENANT, WebOrderStatus.NEW, "9000", now);
        // X: other tenant
        customer("998900000009", OTHER_TENANT, false);
        order("998900000009", OTHER_TENANT, WebOrderStatus.NEW, "9000", now);

        assertEquals(List.of("998900000001", "998900000002", "998900000003"),
                phones(customerRepository.segmentAll(TENANT)));

        assertEquals(List.of("998900000001"),
                phones(customerRepository.segmentOrderedSince(TENANT, cutoff)));

        assertEquals(List.of("998900000002"),
                phones(customerRepository.segmentNoOrderSince(TENANT, cutoff)));

        assertEquals(List.of("998900000003"),
                phones(customerRepository.segmentNeverOrdered(TENANT)));

        // Only B has completed spend ≥ 40000 (A's order is NEW, not COMPLETED)
        assertEquals(List.of("998900000002"),
                phones(customerRepository.segmentMinSpent(TENANT, new BigDecimal("40000"))));
        assertTrue(phones(customerRepository.segmentMinSpent(TENANT, new BigDecimal("60000"))).isEmpty());
    }

    private List<String> phones(List<WebCustomer> customers) {
        return customers.stream().map(WebCustomer::getPhone).sorted().toList();
    }
}
