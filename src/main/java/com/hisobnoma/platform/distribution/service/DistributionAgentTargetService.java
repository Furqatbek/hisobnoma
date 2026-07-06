package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.dto.CreateDistributionAgentTargetRequest;
import com.hisobnoma.platform.distribution.dto.DistributionAgentTargetDto;
import com.hisobnoma.platform.distribution.dto.UpdateDistributionAgentTargetRequest;
import com.hisobnoma.platform.distribution.entity.DistributionAgentTarget;
import com.hisobnoma.platform.distribution.entity.TargetPeriodType;
import com.hisobnoma.platform.distribution.mapper.DistributionAgentTargetMapper;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.distribution.repository.DistributionAgentTargetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributionAgentTargetService {

    private final DistributionAgentTargetRepository targetRepository;
    private final DistributionAgentTargetMapper targetMapper;
    private final DistributionAgentRepository agentRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<DistributionAgentTargetDto> getTargetsForAgent(Long agentId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return targetMapper.toDtoList(targetRepository.findByTenantIdAndAgentId(tenantId, agentId));
    }

    @Transactional(readOnly = true)
    public DistributionAgentTargetDto getTarget(Long id) {
        return targetMapper.toDto(loadTarget(id));
    }

    @Transactional
    public DistributionAgentTargetDto createTarget(CreateDistributionAgentTargetRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        if (agentRepository.findByIdAndTenantId(request.getAgentId(), tenantId).isEmpty()) {
            throw new NotFoundException("DistributionAgent", request.getAgentId());
        }
        if (request.getPeriodEnd().isBefore(request.getPeriodStart())) {
            throw new BusinessException("Period end must not be before period start", "INVALID_PERIOD");
        }
        if (targetRepository.existsByTenantIdAndAgentIdAndPeriodStartAndPeriodEnd(
                tenantId, request.getAgentId(), request.getPeriodStart(), request.getPeriodEnd())) {
            throw new DuplicateResourceException("A target for this agent and period already exists");
        }

        DistributionAgentTarget target = DistributionAgentTarget.builder()
                .agentId(request.getAgentId())
                .periodType(request.getPeriodType() != null ? request.getPeriodType() : TargetPeriodType.MONTHLY)
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .targetRevenue(nz(request.getTargetRevenue()))
                .targetOrders(nzi(request.getTargetOrders()))
                .targetVisits(nzi(request.getTargetVisits()))
                .targetNewCustomers(nzi(request.getTargetNewCustomers()))
                .targetCollection(nz(request.getTargetCollection()))
                .notes(request.getNotes())
                .tenantId(tenantId)
                .build();
        target = targetRepository.save(target);
        log.info("Created agent target {} for agent {} [{}..{}]",
                target.getId(), request.getAgentId(), request.getPeriodStart(), request.getPeriodEnd());
        return targetMapper.toDto(target);
    }

    @Transactional
    public DistributionAgentTargetDto updateTarget(Long id, UpdateDistributionAgentTargetRequest request) {
        DistributionAgentTarget target = loadTarget(id);
        if (request.getTargetRevenue() != null) target.setTargetRevenue(request.getTargetRevenue());
        if (request.getTargetOrders() != null) target.setTargetOrders(request.getTargetOrders());
        if (request.getTargetVisits() != null) target.setTargetVisits(request.getTargetVisits());
        if (request.getTargetNewCustomers() != null) target.setTargetNewCustomers(request.getTargetNewCustomers());
        if (request.getTargetCollection() != null) target.setTargetCollection(request.getTargetCollection());
        if (request.getNotes() != null) target.setNotes(request.getNotes());
        return targetMapper.toDto(targetRepository.save(target));
    }

    @Transactional
    public void deleteTarget(Long id) {
        DistributionAgentTarget target = loadTarget(id);
        targetRepository.delete(target);
    }

    private DistributionAgentTarget loadTarget(Long id) {
        return targetRepository.findByIdAndTenantId(id, securityContextHelper.getCurrentTenantId())
                .orElseThrow(() -> new NotFoundException("DistributionAgentTarget", id));
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static Integer nzi(Integer v) {
        return v != null ? v : 0;
    }
}
