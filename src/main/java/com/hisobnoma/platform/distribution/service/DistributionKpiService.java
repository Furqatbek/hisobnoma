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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the per-agent KPI dashboard by combining stored targets with actuals
 * aggregated live from distribution orders and visits.
 */
@Service
@RequiredArgsConstructor
public class DistributionKpiService {

    private final DistributionOrderRepository orderRepository;
    private final DistributionVisitRepository visitRepository;
    private final DistributionAgentTargetRepository targetRepository;
    private final DistributionAgentRepository agentRepository;
    private final SecurityContextHelper securityContextHelper;

    /**
     * KPIs for every agent over {@code [from, to]} (both dates inclusive), ordered by
     * revenue descending (leaderboard order). A target is attached when one exists for
     * exactly this period.
     */
    @Transactional(readOnly = true)
    public List<AgentKpiDto> getDashboard(LocalDate from, LocalDate to) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Order aggregates: agentId -> [orderCount, revenue, cash, distinctCustomers]
        Map<Long, Object[]> orderAgg = new HashMap<>();
        for (Object[] row : orderRepository.aggregateByAgent(tenantId, DistributionOrderStatus.CANCELLED, from, to)) {
            orderAgg.put((Long) row[0], new Object[]{row[1], row[2], row[3], row[4]});
        }

        // Visit counts: agentId -> visitCount (check-in within the whole [from, to] span)
        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Map<Long, Long> visitCounts = new HashMap<>();
        for (Object[] row : visitRepository.countVisitsByAgent(tenantId, fromInstant, toInstant)) {
            visitCounts.put((Long) row[0], (Long) row[1]);
        }

        // Targets set for exactly this period
        Map<Long, DistributionAgentTarget> targets = new HashMap<>();
        for (DistributionAgentTarget t : targetRepository.findByTenantIdAndPeriod(tenantId, from, to)) {
            targets.put(t.getAgentId(), t);
        }

        List<AgentKpiDto> result = new ArrayList<>();
        for (DistributionAgent agent : agentRepository.findAllByTenantId(tenantId)) {
            Object[] o = orderAgg.get(agent.getId());
            long orders = o != null ? ((Number) o[0]).longValue() : 0;
            BigDecimal revenue = o != null ? (BigDecimal) o[1] : BigDecimal.ZERO;
            BigDecimal cash = o != null ? (BigDecimal) o[2] : BigDecimal.ZERO;
            long customers = o != null ? ((Number) o[3]).longValue() : 0;
            long visits = visitCounts.getOrDefault(agent.getId(), 0L);
            DistributionAgentTarget target = targets.get(agent.getId());

            AgentKpiDto.AgentKpiDtoBuilder dto = AgentKpiDto.builder()
                    .agentId(agent.getId())
                    .agentName(agent.getName())
                    .revenue(revenue)
                    .orders((int) orders)
                    .visits((int) visits)
                    .cashCollected(cash)
                    .customersReached((int) customers);

            if (target != null) {
                dto.targetRevenue(target.getTargetRevenue())
                        .targetOrders(target.getTargetOrders())
                        .targetVisits(target.getTargetVisits())
                        .targetNewCustomers(target.getTargetNewCustomers())
                        .targetCollection(target.getTargetCollection());
                if (target.getTargetRevenue() != null && target.getTargetRevenue().signum() > 0) {
                    dto.revenueAchievementPercent(revenue
                            .multiply(BigDecimal.valueOf(100))
                            .divide(target.getTargetRevenue(), 1, RoundingMode.HALF_UP));
                }
            }
            result.add(dto.build());
        }

        result.sort(Comparator.comparing(AgentKpiDto::getRevenue, Comparator.reverseOrder()));
        return result;
    }
}
