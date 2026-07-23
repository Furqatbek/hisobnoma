package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.distribution.dto.DistributionOrderDto;
import com.hisobnoma.platform.distribution.dto.DistributionRouteDto;
import com.hisobnoma.platform.distribution.dto.DistributionVisitDto;
import com.hisobnoma.platform.distribution.dto.VanLoadoutDto;
import com.hisobnoma.platform.distribution.dto.VisitCheckOutRequest;
import com.hisobnoma.platform.distribution.entity.DistributionAgent;
import com.hisobnoma.platform.distribution.entity.VanLoadout;
import com.hisobnoma.platform.distribution.entity.VanLoadoutStatus;
import com.hisobnoma.platform.distribution.mapper.DistributionOrderMapper;
import com.hisobnoma.platform.distribution.mapper.DistributionRouteMapper;
import com.hisobnoma.platform.distribution.mapper.VanLoadoutMapper;
import com.hisobnoma.platform.distribution.repository.DistributionOrderRepository;
import com.hisobnoma.platform.distribution.repository.DistributionRouteRepository;
import com.hisobnoma.platform.distribution.repository.DistributionVisitRepository;
import com.hisobnoma.platform.distribution.repository.VanLoadoutRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Self-service read + visit-action surface for the agent mobile app. Identity
 * (tenant + agentId) is resolved from the bearer token by {@link AgentAuthService};
 * every query here is scoped by that pair — a client never supplies an agentId.
 */
@Service
@RequiredArgsConstructor
public class AgentPortalService {

    private final AgentAuthService agentAuthService;
    private final DistributionVisitRepository visitRepository;
    private final DistributionRouteRepository routeRepository;
    private final VanLoadoutRepository loadoutRepository;
    private final DistributionOrderRepository orderRepository;
    private final DistributionRouteMapper routeMapper;
    private final VanLoadoutMapper loadoutMapper;
    private final DistributionOrderMapper orderMapper;
    private final com.hisobnoma.platform.distribution.mapper.DistributionVisitMapper visitMapper;
    private final DistributionVisitService visitService;
    private final DistributionOrderService orderService;

    /** Today's snapshot for the home screen. */
    @Transactional(readOnly = true)
    public Map<String, Object> getTodaySummary(String bearer) {
        DistributionAgent agent = agentAuthService.requireAgent(bearer);
        Long tenantId = agent.getTenantId();
        Long agentId = agent.getId();

        LocalDate today = LocalDate.now();
        Instant from = today.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        long visitsToday = visitRepository
                .findByTenantIdAndCheckInBetween(tenantId, from, to, Pageable.unpaged())
                .stream().filter(v -> agentId.equals(v.getAgentId())).count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("agentId", agentId);
        summary.put("agentName", agent.getName());
        summary.put("date", today.toString());
        summary.put("routes", routeRepository.findByTenantIdAndAgentId(tenantId, agentId).size());
        summary.put("visitsToday", visitsToday);
        summary.put("hasActiveLoadout", currentLoadout(tenantId, agentId) != null);
        return summary;
    }

    @Transactional(readOnly = true)
    public List<DistributionRouteDto> getMyRoutes(String bearer) {
        DistributionAgent agent = agentAuthService.requireAgent(bearer);
        return routeRepository.findByTenantIdAndAgentId(agent.getTenantId(), agent.getId())
                .stream().map(routeMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<DistributionVisitDto> getMyVisits(String bearer, Pageable pageable) {
        DistributionAgent agent = agentAuthService.requireAgent(bearer);
        return PageResponse.of(visitRepository
                .findByTenantIdAndAgentId(agent.getTenantId(), agent.getId(), pageable)
                .map(visitMapper::toDto));
    }

    /** The agent's current (most recent LOADED) van loadout, or null if none is out. */
    @Transactional(readOnly = true)
    public VanLoadoutDto getCurrentLoadout(String bearer) {
        DistributionAgent agent = agentAuthService.requireAgent(bearer);
        VanLoadout loadout = currentLoadout(agent.getTenantId(), agent.getId());
        return loadout != null ? loadoutMapper.toDto(loadout) : null;
    }

    @Transactional(readOnly = true)
    public PageResponse<DistributionOrderDto> getMyOrders(String bearer, Pageable pageable) {
        DistributionAgent agent = agentAuthService.requireAgent(bearer);
        return PageResponse.of(orderRepository
                .findByTenantIdAndAgentId(agent.getTenantId(), agent.getId(), pageable)
                .map(orderMapper::toDto));
    }

    @Transactional
    public DistributionVisitDto checkIn(String bearer,
                                        com.hisobnoma.platform.distribution.dto.AgentCheckInRequest request) {
        DistributionAgent agent = agentAuthService.requireAgent(bearer);
        return visitService.agentCheckIn(agent.getTenantId(), agent.getId(),
                request.toVisitCheckInRequest());
    }

    @Transactional
    public DistributionVisitDto checkOut(String bearer, Long visitId, VisitCheckOutRequest request) {
        DistributionAgent agent = agentAuthService.requireAgent(bearer);
        return visitService.agentCheckOut(agent.getTenantId(), agent.getId(), visitId, request);
    }

    /** Field order placement: source defaults to the agent's current van loadout location. */
    @Transactional
    public DistributionOrderDto placeOrder(String bearer,
                                           com.hisobnoma.platform.distribution.dto.AgentCreateOrderRequest request) {
        DistributionAgent agent = agentAuthService.requireAgent(bearer);
        VanLoadout loadout = currentLoadout(agent.getTenantId(), agent.getId());
        Long vanLocationId = loadout != null ? loadout.getVehicleLocationId() : null;
        return orderService.agentCreateOrder(agent.getTenantId(), agent.getId(),
                request.toCreateRequest(), vanLocationId, request.isConfirmNow());
    }

    private VanLoadout currentLoadout(Long tenantId, Long agentId) {
        return loadoutRepository
                .findByTenantIdAndAgentId(tenantId, agentId, Pageable.unpaged())
                .stream()
                .filter(l -> l.getStatus() == VanLoadoutStatus.LOADED)
                .max((a, b) -> a.getLoadoutDate().compareTo(b.getLoadoutDate()))
                .orElse(null);
    }
}
