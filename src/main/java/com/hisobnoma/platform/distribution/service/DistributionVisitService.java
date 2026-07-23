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
import com.hisobnoma.platform.finance.dto.ARPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateARPaymentAllocationRequest;
import com.hisobnoma.platform.finance.dto.CreateARPaymentRequest;
import com.hisobnoma.platform.finance.entity.ARInvoice;
import com.hisobnoma.platform.finance.entity.ARInvoiceStatus;
import com.hisobnoma.platform.finance.entity.ARPaymentMethod;
import com.hisobnoma.platform.finance.entity.Customer;
import com.hisobnoma.platform.finance.repository.ARInvoiceRepository;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.finance.service.ARPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributionVisitService {

    /** Invoice statuses an agent's collection may settle (posted, not fully paid). */
    private static final List<ARInvoiceStatus> OPEN_INVOICE_STATUSES = List.of(
            ARInvoiceStatus.PENDING, ARInvoiceStatus.SENT,
            ARInvoiceStatus.PARTIAL, ARInvoiceStatus.OVERDUE);

    private final DistributionVisitRepository visitRepository;
    private final DistributionVisitMapper visitMapper;
    private final DistributionAgentRepository agentRepository;
    private final DistributionRouteRepository routeRepository;
    private final CustomerRepository customerRepository;
    private final ARInvoiceRepository arInvoiceRepository;
    private final ARPaymentService arPaymentService;
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
        return doCheckIn(securityContextHelper.getCurrentTenantId(), request);
    }

    /** Agent-app check-in: the agent is the token's, never client-supplied. */
    @Transactional
    public DistributionVisitDto agentCheckIn(Long tenantId, Long agentId, VisitCheckInRequest request) {
        request.setAgentId(agentId);
        return doCheckIn(tenantId, request);
    }

    private DistributionVisitDto doCheckIn(Long tenantId, VisitCheckInRequest request) {
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
        return doCheckOut(loadVisit(id), request);
    }

    /**
     * Agent-app check-out: loads the visit under the token's tenant and asserts the
     * visit belongs to the calling agent (a mismatched id 404s — never a cross-agent edit).
     */
    @Transactional
    public DistributionVisitDto agentCheckOut(Long tenantId, Long agentId, Long visitId,
                                              VisitCheckOutRequest request) {
        DistributionVisit visit = visitRepository.findByIdAndTenantId(visitId, tenantId)
                .filter(v -> agentId.equals(v.getAgentId()))
                .orElseThrow(() -> new NotFoundException("DistributionVisit", visitId));
        return doCheckOut(visit, request);
    }

    private DistributionVisitDto doCheckOut(DistributionVisit visit, VisitCheckOutRequest request) {
        if (request.getCollectedAmount() != null && visit.getArPaymentId() != null) {
            // Money must never be double-recorded on a repeated checkout call.
            throw new BusinessException("Visit already has a recorded collection", "COLLECTION_EXISTS");
        }
        visit.setCheckOutAt(Instant.now());
        visit.setOutcome(request.getOutcome());
        if (request.getLatitude() != null) visit.setCheckOutLat(request.getLatitude());
        if (request.getLongitude() != null) visit.setCheckOutLng(request.getLongitude());
        if (request.getDistributionOrderId() != null) visit.setDistributionOrderId(request.getDistributionOrderId());
        if (request.getNotes() != null) visit.setNotes(request.getNotes());

        // Cash collection is NOT best-effort: if recording the payment fails,
        // the whole checkout fails and the agent retries.
        if (request.getCollectedAmount() != null) {
            recordCollection(visit, request.getCollectedAmount());
        }
        return visitMapper.toDto(visitRepository.save(visit));
    }

    /**
     * Records cash the agent collected as a completed (GL-posted) AR payment,
     * allocated oldest-due-first across the customer's open invoices. Any
     * amount beyond the open balance stays on the payment as an advance.
     */
    private void recordCollection(DistributionVisit visit, BigDecimal amount) {
        Long tenantId = visit.getTenantId() != null
                ? visit.getTenantId()
                : securityContextHelper.getCurrentTenantId();

        List<CreateARPaymentAllocationRequest> allocations = new ArrayList<>();
        BigDecimal remaining = amount;
        List<ARInvoice> openInvoices = arInvoiceRepository.findUnpaidByCustomer(
                tenantId, visit.getCustomerId(), OPEN_INVOICE_STATUSES);
        for (ARInvoice invoice : openInvoices) {
            if (remaining.signum() <= 0) break;
            BigDecimal due = invoice.getBalanceDue();
            if (due == null || due.signum() <= 0) continue;
            BigDecimal alloc = remaining.min(due);
            allocations.add(CreateARPaymentAllocationRequest.builder()
                    .arInvoiceId(invoice.getId())
                    .allocatedAmount(alloc)
                    .build());
            remaining = remaining.subtract(alloc);
        }

        ARPaymentDto payment = arPaymentService.createPayment(CreateARPaymentRequest.builder()
                .customerId(visit.getCustomerId())
                .paymentDate(LocalDate.now())
                .paymentMethod(ARPaymentMethod.CASH)
                .paymentAmount(amount)
                .referenceNumber("VISIT-" + visit.getId())
                .notes("Дистрибуция ташрифида йиғилган нақд пул (агент #" + visit.getAgentId() + ")")
                .allocations(allocations)
                .build());
        arPaymentService.completePayment(payment.getId());

        visit.setCollectedAmount(amount);
        visit.setArPaymentId(payment.getId());
        log.info("Visit {} collected {} from customer {} (payment {}, {} invoice(s) settled)",
                visit.getId(), amount, visit.getCustomerId(), payment.getPaymentNumber(),
                allocations.size());
    }

    private DistributionVisit loadVisit(Long id) {
        return visitRepository.findByIdAndTenantId(id, securityContextHelper.getCurrentTenantId())
                .orElseThrow(() -> new NotFoundException("DistributionVisit", id));
    }
}
