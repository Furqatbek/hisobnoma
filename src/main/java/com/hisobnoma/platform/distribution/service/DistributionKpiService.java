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

        // AR cash collected during visits (in addition to cash-on-delivery order cash)
        Map<Long, BigDecimal> collections = new HashMap<>();
        for (Object[] row : visitRepository.sumCollectedByAgent(tenantId, fromInstant, toInstant)) {
            collections.put((Long) row[0], (BigDecimal) row[1]);
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
            cash = cash.add(collections.getOrDefault(agent.getId(), BigDecimal.ZERO));
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

            if (visits > 0) {
                dto.strikeRatePercent(BigDecimal.valueOf(orders)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(visits), 1, RoundingMode.HALF_UP));
            }
            if (orders > 0) {
                dto.avgDropSize(revenue.divide(BigDecimal.valueOf(orders), 0, RoundingMode.HALF_UP));
            }

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

    /**
     * Daily revenue/orders/visits/collections over {@code [from, to]} for the trend
     * chart. Every day in the range is present (zero-filled), so the chart never has
     * gaps.
     */
    @Transactional(readOnly = true)
    public List<com.hisobnoma.platform.distribution.dto.DailyTrendDto> getTrend(LocalDate from, LocalDate to) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Map<LocalDate, Object[]> orderByDay = new HashMap<>();
        for (Object[] row : orderRepository.aggregateByDate(tenantId, DistributionOrderStatus.CANCELLED, from, to)) {
            orderByDay.put((LocalDate) row[0], new Object[]{row[1], row[2]});
        }

        Instant fromInstant = from.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Map<LocalDate, Long> visitsByDay = new HashMap<>();
        Map<LocalDate, BigDecimal> collectedByDay = new HashMap<>();
        for (Object[] row : visitRepository.visitTimesAndCollections(tenantId, fromInstant, toInstant)) {
            LocalDate day = ((Instant) row[0]).atZone(ZoneOffset.UTC).toLocalDate();
            visitsByDay.merge(day, 1L, Long::sum);
            if (row[1] != null) {
                collectedByDay.merge(day, (BigDecimal) row[1], BigDecimal::add);
            }
        }

        List<com.hisobnoma.platform.distribution.dto.DailyTrendDto> trend = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            Object[] o = orderByDay.get(day);
            trend.add(com.hisobnoma.platform.distribution.dto.DailyTrendDto.builder()
                    .date(day)
                    .orders(o != null ? ((Number) o[0]).longValue() : 0)
                    .revenue(o != null ? (BigDecimal) o[1] : BigDecimal.ZERO)
                    .visits(visitsByDay.getOrDefault(day, 0L))
                    .collected(collectedByDay.getOrDefault(day, BigDecimal.ZERO))
                    .build());
        }
        return trend;
    }
}
