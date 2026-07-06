package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.dto.DistributionVisitDto;
import com.hisobnoma.platform.distribution.dto.VisitCheckInRequest;
import com.hisobnoma.platform.distribution.dto.VisitCheckOutRequest;
import com.hisobnoma.platform.distribution.entity.DistributionRoute;
import com.hisobnoma.platform.distribution.entity.DistributionVisit;
import com.hisobnoma.platform.distribution.entity.VisitOutcome;
import com.hisobnoma.platform.distribution.entity.VisitType;
import com.hisobnoma.platform.distribution.mapper.DistributionVisitMapper;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.distribution.repository.DistributionRouteRepository;
import com.hisobnoma.platform.distribution.repository.DistributionVisitRepository;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributionVisitService {

    private final DistributionVisitRepository visitRepository;
    private final DistributionVisitMapper visitMapper;
    private final DistributionAgentRepository agentRepository;
    private final DistributionRouteRepository routeRepository;
    private final CustomerRepository customerRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<DistributionVisitDto> getVisits(Long agentId, Long routeId, LocalDate date, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        if (agentId != null) {
            return PageResponse.of(visitRepository.findByTenantIdAndAgentId(tenantId, agentId, pageable).map(visitMapper::toDto));
        }
        if (routeId != null) {
            return PageResponse.of(visitRepository.findByTenantIdAndRouteId(tenantId, routeId, pageable).map(visitMapper::toDto));
        }
        if (date != null) {
            Instant from = date.atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            return PageResponse.of(visitRepository.findByTenantIdAndCheckInBetween(tenantId, from, to, pageable).map(visitMapper::toDto));
        }
        return PageResponse.of(visitRepository.findAllByTenantId(tenantId, pageable).map(visitMapper::toDto));
    }

    @Transactional(readOnly = true)
    public DistributionVisitDto getVisit(Long id) {
        return visitMapper.toDto(loadVisit(id));
    }

    @Transactional
    public DistributionVisitDto checkIn(VisitCheckInRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        if (agentRepository.findByIdAndTenantId(request.getAgentId(), tenantId).isEmpty()) {
            throw new NotFoundException("DistributionAgent", request.getAgentId());
        }
        Customer customer = customerRepository.findByIdAndTenantId(request.getCustomerId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Customer", request.getCustomerId()));
        // Tenant-scope the optional route/stop link (mirrors the agent/customer checks) so a
        // visit can't reference another tenant's route.
        if (request.getRouteId() != null) {
            DistributionRoute route = routeRepository.findByIdAndTenantId(request.getRouteId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("DistributionRoute", request.getRouteId()));
            if (request.getRouteStopId() != null
                    && route.getStops().stream().noneMatch(s -> request.getRouteStopId().equals(s.getId()))) {
                throw new NotFoundException("DistributionRouteStop", request.getRouteStopId());
            }
        } else if (request.getRouteStopId() != null) {
            throw new BusinessException("routeStopId requires routeId", "ROUTE_STOP_WITHOUT_ROUTE");
        }

        DistributionVisit visit = DistributionVisit.builder()
                .agentId(request.getAgentId())
                .customerId(customer.getId())
                .customerName(customer.getName())
                .routeId(request.getRouteId())
                .routeStopId(request.getRouteStopId())
                .checkInAt(Instant.now())
                .checkInLat(request.getLatitude())
                .checkInLng(request.getLongitude())
                .visitType(request.getVisitType() != null ? request.getVisitType() : VisitType.PLANNED)
                .outcome(VisitOutcome.PENDING)
                .notes(request.getNotes())
                .tenantId(tenantId)
                .build();
        visit = visitRepository.save(visit);
        log.info("Visit check-in {} agent {} customer {}", visit.getId(), request.getAgentId(), customer.getCode());
        return visitMapper.toDto(visit);
    }

    @Transactional
    public DistributionVisitDto checkOut(Long id, VisitCheckOutRequest request) {
        DistributionVisit visit = loadVisit(id);
        visit.setCheckOutAt(Instant.now());
        visit.setOutcome(request.getOutcome());
        if (request.getLatitude() != null) visit.setCheckOutLat(request.getLatitude());
        if (request.getLongitude() != null) visit.setCheckOutLng(request.getLongitude());
        if (request.getDistributionOrderId() != null) visit.setDistributionOrderId(request.getDistributionOrderId());
        if (request.getNotes() != null) visit.setNotes(request.getNotes());
        return visitMapper.toDto(visitRepository.save(visit));
    }

    private DistributionVisit loadVisit(Long id) {
        return visitRepository.findByIdAndTenantId(id, securityContextHelper.getCurrentTenantId())
                .orElseThrow(() -> new NotFoundException("DistributionVisit", id));
    }
}
