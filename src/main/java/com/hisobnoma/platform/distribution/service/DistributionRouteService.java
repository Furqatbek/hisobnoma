package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.dto.CreateDistributionRouteRequest;
import com.hisobnoma.platform.distribution.dto.DistributionRouteDto;
import com.hisobnoma.platform.distribution.dto.DistributionRouteStopRequest;
import com.hisobnoma.platform.distribution.dto.UpdateDistributionRouteRequest;
import com.hisobnoma.platform.distribution.entity.DistributionRoute;
import com.hisobnoma.platform.distribution.entity.DistributionRouteStop;
import com.hisobnoma.platform.distribution.entity.RouteStatus;
import com.hisobnoma.platform.distribution.mapper.DistributionRouteMapper;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.distribution.repository.DistributionRouteRepository;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributionRouteService {

    private final DistributionRouteRepository routeRepository;
    private final DistributionRouteMapper routeMapper;
    private final DistributionAgentRepository agentRepository;
    private final CustomerRepository customerRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<DistributionRouteDto> getRoutes(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<DistributionRoute> page = routeRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(routeMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<DistributionRouteDto> getRoutesByAgent(Long agentId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return routeMapper.toDtoList(routeRepository.findByTenantIdAndAgentId(tenantId, agentId));
    }

    @Transactional(readOnly = true)
    public PageResponse<DistributionRouteDto> searchRoutes(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return PageResponse.of(routeRepository.searchByTenantId(tenantId, search, pageable).map(routeMapper::toDto));
    }

    @Transactional(readOnly = true)
    public DistributionRouteDto getRoute(Long id) {
        return routeMapper.toDto(loadRoute(id));
    }

    @Transactional
    public DistributionRouteDto createRoute(CreateDistributionRouteRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        if (routeRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new DuplicateResourceException("DistributionRoute", "code", request.getCode());
        }
        validateAgent(request.getAgentId(), tenantId);

        DistributionRoute route = DistributionRoute.builder()
                .code(request.getCode())
                .name(request.getName())
                .agentId(request.getAgentId())
                .territoryRegionId(request.getTerritoryRegionId())
                .dayOfWeek(request.getDayOfWeek())
                .estimatedDurationMinutes(request.getEstimatedDurationMinutes())
                .distanceKm(request.getDistanceKm())
                .status(request.getStatus() != null ? request.getStatus() : RouteStatus.DRAFT)
                .notes(request.getNotes())
                .tenantId(tenantId)
                .build();
        replaceStops(route, request.getStops(), tenantId);

        route = routeRepository.save(route);
        log.info("Created distribution route {} ({})", route.getName(), route.getCode());
        return routeMapper.toDto(route);
    }

    @Transactional
    public DistributionRouteDto updateRoute(Long id, UpdateDistributionRouteRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        DistributionRoute route = loadRoute(id);

        if (request.getName() != null) route.setName(request.getName());
        if (request.getAgentId() != null) {
            validateAgent(request.getAgentId(), tenantId);
            route.setAgentId(request.getAgentId());
        }
        if (request.getTerritoryRegionId() != null) route.setTerritoryRegionId(request.getTerritoryRegionId());
        if (request.getDayOfWeek() != null) route.setDayOfWeek(request.getDayOfWeek());
        if (request.getEstimatedDurationMinutes() != null) route.setEstimatedDurationMinutes(request.getEstimatedDurationMinutes());
        if (request.getDistanceKm() != null) route.setDistanceKm(request.getDistanceKm());
        if (request.getStatus() != null) route.setStatus(request.getStatus());
        if (request.getNotes() != null) route.setNotes(request.getNotes());
        if (request.getStops() != null) {
            replaceStops(route, request.getStops(), tenantId); // replaceStops clears then rebuilds
        }

        route = routeRepository.save(route);
        return routeMapper.toDto(route);
    }

    @Transactional
    public void deleteRoute(Long id) {
        DistributionRoute route = loadRoute(id);
        routeRepository.delete(route);
        log.info("Deleted distribution route {}", id);
    }

    // ---- helpers ----

    private DistributionRoute loadRoute(Long id) {
        return routeRepository.findByIdAndTenantId(id, securityContextHelper.getCurrentTenantId())
                .orElseThrow(() -> new NotFoundException("DistributionRoute", id));
    }

    private void validateAgent(Long agentId, Long tenantId) {
        if (agentId != null && agentRepository.findByIdAndTenantId(agentId, tenantId).isEmpty()) {
            throw new NotFoundException("DistributionAgent", agentId);
        }
    }

    private void replaceStops(DistributionRoute route, List<DistributionRouteStopRequest> requests, Long tenantId) {
        route.getStops().clear();
        if (requests == null) {
            return;
        }
        for (DistributionRouteStopRequest req : requests) {
            Customer customer = customerRepository.findByIdAndTenantId(req.getCustomerId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Customer", req.getCustomerId()));
            DistributionRouteStop stop = DistributionRouteStop.builder()
                    .customerId(customer.getId())
                    .customerName(customer.getName())
                    .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
                    .visitWindowStart(req.getVisitWindowStart())
                    .visitWindowEnd(req.getVisitWindowEnd())
                    .latitude(req.getLatitude())
                    .longitude(req.getLongitude())
                    .address(req.getAddress())
                    .notes(req.getNotes())
                    .tenantId(tenantId)
                    .build();
            route.addStop(stop);
        }
    }
}
