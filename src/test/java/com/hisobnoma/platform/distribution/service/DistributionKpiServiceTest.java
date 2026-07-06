package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.distribution.dto.AgentKpiDto;
import com.hisobnoma.platform.distribution.entity.DistributionAgent;
import com.hisobnoma.platform.distribution.entity.DistributionAgentTarget;
import com.hisobnoma.platform.distribution.entity.DistributionOrderStatus;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.distribution.repository.DistributionAgentTargetRepository;
import com.hisobnoma.platform.distribution.repository.DistributionOrderRepository;
import com.hisobnoma.platform.distribution.repository.DistributionVisitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributionKpiServiceTest {

    @Mock private DistributionOrderRepository orderRepository;
    @Mock private DistributionVisitRepository visitRepository;
    @Mock private DistributionAgentTargetRepository targetRepository;
    @Mock private DistributionAgentRepository agentRepository;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks private DistributionKpiService service;

    private static final Long TENANT_ID = 1L;
    private static final LocalDate FROM = LocalDate.of(2026, 7, 1);
    private static final LocalDate TO = LocalDate.of(2026, 7, 31);

    private DistributionAgent agent(Long id, String name) {
        DistributionAgent a = DistributionAgent.builder().name(name).build();
        a.setId(id);
        return a;
    }

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
    }

    @Test
    void dashboard_combinesActualsTargetsAndSortsByRevenue() {
        // agent 5: 3 orders, 300k revenue, 250k cash, 4 customers; agent 6: 1 order, 500k revenue
        when(orderRepository.aggregateByAgent(eq(TENANT_ID), eq(DistributionOrderStatus.CANCELLED), eq(FROM), eq(TO)))
                .thenReturn(List.of(
                        new Object[]{5L, 3L, new BigDecimal("300000"), new BigDecimal("250000"), 4L},
                        new Object[]{6L, 1L, new BigDecimal("500000"), new BigDecimal("0"), 1L}));
        when(visitRepository.countVisitsByAgent(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of(new Object[]{5L, 12L}));
        DistributionAgentTarget target = DistributionAgentTarget.builder()
                .agentId(5L).targetRevenue(new BigDecimal("600000")).targetOrders(10).targetVisits(20).build();
        when(targetRepository.findByTenantIdAndPeriod(TENANT_ID, FROM, TO)).thenReturn(List.of(target));
        when(agentRepository.findAllByTenantId(TENANT_ID))
                .thenReturn(List.of(agent(5L, "Alisher"), agent(6L, "Bek"), agent(7L, "Dilnoza")));

        List<AgentKpiDto> dash = service.getDashboard(FROM, TO);

        assertEquals(3, dash.size());
        // sorted by revenue desc: Bek (500k), Alisher (300k), Dilnoza (0)
        assertEquals("Bek", dash.get(0).getAgentName());
        assertEquals("Alisher", dash.get(1).getAgentName());
        assertEquals("Dilnoza", dash.get(2).getAgentName());

        AgentKpiDto alisher = dash.get(1);
        assertEquals(3, alisher.getOrders());
        assertEquals(12, alisher.getVisits());
        assertEquals(4, alisher.getCustomersReached());
        assertEquals(0, new BigDecimal("250000").compareTo(alisher.getCashCollected()));
        // achievement = 300000 / 600000 * 100 = 50.0
        assertEquals(0, new BigDecimal("50.0").compareTo(alisher.getRevenueAchievementPercent()));
        assertEquals(10, alisher.getTargetOrders());

        // agent with no activity shows zeros and no target
        AgentKpiDto dilnoza = dash.get(2);
        assertEquals(0, dilnoza.getOrders());
        assertEquals(0, dilnoza.getVisits());
        assertEquals(0, BigDecimal.ZERO.compareTo(dilnoza.getRevenue()));
        assertNull(dilnoza.getRevenueAchievementPercent());
        assertNull(dilnoza.getTargetRevenue());

        // agent with activity but no target: no achievement percent
        AgentKpiDto bek = dash.get(0);
        assertNull(bek.getRevenueAchievementPercent());
        assertEquals(0, bek.getVisits()); // no visits recorded
    }

    @Test
    void dashboard_zeroRevenueTargetYieldsNullAchievement() {
        when(orderRepository.aggregateByAgent(eq(TENANT_ID), eq(DistributionOrderStatus.CANCELLED), eq(FROM), eq(TO)))
                .thenReturn(List.<Object[]>of(new Object[]{5L, 2L, new BigDecimal("100000"), new BigDecimal("0"), 2L}));
        when(visitRepository.countVisitsByAgent(eq(TENANT_ID), any(Instant.class), any(Instant.class)))
                .thenReturn(List.<Object[]>of());
        DistributionAgentTarget target = DistributionAgentTarget.builder()
                .agentId(5L).targetRevenue(BigDecimal.ZERO).targetVisits(5).build();
        when(targetRepository.findByTenantIdAndPeriod(TENANT_ID, FROM, TO)).thenReturn(List.of(target));
        when(agentRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(agent(5L, "Alisher")));

        List<AgentKpiDto> dash = service.getDashboard(FROM, TO);

        assertEquals(1, dash.size());
        assertNull(dash.get(0).getRevenueAchievementPercent());
        assertEquals(5, dash.get(0).getTargetVisits());
    }
}
