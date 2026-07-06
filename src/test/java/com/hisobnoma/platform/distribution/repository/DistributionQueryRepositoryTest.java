package com.hisobnoma.platform.distribution.repository;

import com.hisobnoma.platform.distribution.entity.DistributionAgentTarget;
import com.hisobnoma.platform.distribution.entity.DistributionOrder;
import com.hisobnoma.platform.distribution.entity.DistributionOrderStatus;
import com.hisobnoma.platform.distribution.entity.DistributionVisit;
import com.hisobnoma.platform.distribution.entity.VisitOutcome;
import com.hisobnoma.platform.distribution.entity.VisitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Executes the hand-written JPQL against H2 — the aggregate/projection queries the
 * service unit tests can only mock. Catches column-order, GROUP BY, date-range and
 * tenant-scoping bugs that a mocked repository cannot.
 */
@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class DistributionQueryRepositoryTest {

    private static final Long TENANT = 1L;
    private static final Long OTHER_TENANT = 2L;
    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);
    private static final LocalDate TO = LocalDate.of(2026, 7, 31);

    @Autowired private TestEntityManager em;
    @Autowired private DistributionOrderRepository orderRepository;
    @Autowired private DistributionVisitRepository visitRepository;
    @Autowired private DistributionAgentTargetRepository targetRepository;

    private DistributionOrder order(Long tenant, Long agentId, Long customerId, DistributionOrderStatus status,
                                    LocalDate date, String number, String total, String cash) {
        DistributionOrder o = DistributionOrder.builder()
                .orderNumber(number).status(status).agentId(agentId).customerId(customerId)
                .orderDate(date).totalAmount(new BigDecimal(total)).cashCollected(new BigDecimal(cash))
                .tenantId(tenant).build();
        return em.persistAndFlush(o);
    }

    private void visit(Long tenant, Long agentId, Instant checkIn) {
        DistributionVisit v = DistributionVisit.builder()
                .agentId(agentId).customerId(100L).checkInAt(checkIn)
                .visitType(VisitType.PLANNED).outcome(VisitOutcome.PENDING).tenantId(tenant).build();
        em.persistAndFlush(v);
    }

    @Test
    void aggregateByAgent_groupsNonCancelledOrdersInRangeByAgent() {
        order(TENANT, 5L, 100L, DistributionOrderStatus.DELIVERED, LocalDate.of(2026, 7, 5), "DO-1", "100", "60");
        order(TENANT, 5L, 101L, DistributionOrderStatus.CONFIRMED, TO, "DO-2", "200", "0"); // boundary 'to' day included
        order(TENANT, 5L, 100L, DistributionOrderStatus.CANCELLED, LocalDate.of(2026, 7, 10), "DO-3", "999", "0"); // excluded
        order(TENANT, 6L, 100L, DistributionOrderStatus.DRAFT, LocalDate.of(2026, 7, 15), "DO-4", "50", "50");
        order(TENANT, 5L, 100L, DistributionOrderStatus.DELIVERED, LocalDate.of(2026, 8, 1), "DO-5", "77", "0"); // out of range
        order(OTHER_TENANT, 5L, 100L, DistributionOrderStatus.DELIVERED, LocalDate.of(2026, 7, 5), "DO-6", "500", "0"); // other tenant

        Map<Long, Object[]> byAgent = new HashMap<>();
        for (Object[] row : orderRepository.aggregateByAgent(TENANT, DistributionOrderStatus.CANCELLED, FROM, TO)) {
            byAgent.put((Long) row[0], row);
        }

        assertEquals(2, byAgent.size());
        // agent 5: 2 orders (100 + 200), cash 60, 2 distinct customers
        Object[] a5 = byAgent.get(5L);
        assertEquals(2L, ((Number) a5[1]).longValue());
        assertEquals(0, new BigDecimal("300").compareTo((BigDecimal) a5[2]));
        assertEquals(0, new BigDecimal("60").compareTo((BigDecimal) a5[3]));
        assertEquals(2L, ((Number) a5[4]).longValue());
        // agent 6: 1 order (50), cash 50, 1 customer
        Object[] a6 = byAgent.get(6L);
        assertEquals(1L, ((Number) a6[1]).longValue());
        assertEquals(0, new BigDecimal("50").compareTo((BigDecimal) a6[2]));
        assertEquals(1L, ((Number) a6[4]).longValue());
    }

    @Test
    void countVisitsByAgent_countsWithinInstantRangeAndTenant() {
        Instant fromI = FROM.atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        Instant toI = TO.plusDays(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant();
        visit(TENANT, 5L, Instant.parse("2026-07-05T10:00:00Z"));
        visit(TENANT, 5L, Instant.parse("2026-07-31T23:59:00Z")); // last day, inside half-open range
        visit(TENANT, 6L, Instant.parse("2026-07-20T09:00:00Z"));
        visit(TENANT, 5L, Instant.parse("2026-08-02T10:00:00Z")); // out of range
        visit(OTHER_TENANT, 5L, Instant.parse("2026-07-05T10:00:00Z")); // other tenant

        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : visitRepository.countVisitsByAgent(TENANT, fromI, toI)) {
            counts.put((Long) row[0], ((Number) row[1]).longValue());
        }

        assertEquals(2L, counts.get(5L));
        assertEquals(1L, counts.get(6L));
    }

    @Test
    void orderFinders_scopeByTenantAndNumber() {
        order(TENANT, 5L, 100L, DistributionOrderStatus.DRAFT, FROM, "DO20260701-00001", "100", "0");
        order(OTHER_TENANT, 5L, 100L, DistributionOrderStatus.DRAFT, FROM, "DO20260701-00001", "100", "0");

        assertTrue(orderRepository.findByTenantIdAndOrderNumber(TENANT, "DO20260701-00001").isPresent());
        assertTrue(orderRepository.findByTenantIdAndOrderNumber(TENANT, "DO-NONE").isEmpty());
        assertEquals("DO20260701-00001", orderRepository.findMaxOrderNumberByPrefix(TENANT, "DO20260701-"));
        assertEquals(1, orderRepository.findByTenantIdAndCustomerId(TENANT, 100L, PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void agentTargetFinders_matchExactPeriodAndTenant() {
        DistributionAgentTarget t = DistributionAgentTarget.builder()
                .agentId(5L).periodStart(FROM).periodEnd(TO)
                .targetRevenue(new BigDecimal("600000")).tenantId(TENANT).build();
        em.persistAndFlush(t);

        List<DistributionAgentTarget> hit = targetRepository.findByTenantIdAndPeriod(TENANT, FROM, TO);
        assertEquals(1, hit.size());
        assertTrue(targetRepository.existsByTenantIdAndAgentIdAndPeriodStartAndPeriodEnd(TENANT, 5L, FROM, TO));
        assertFalse(targetRepository.existsByTenantIdAndAgentIdAndPeriodStartAndPeriodEnd(TENANT, 5L, FROM, FROM));
        assertTrue(targetRepository.findByTenantIdAndPeriod(OTHER_TENANT, FROM, TO).isEmpty());
    }
}
