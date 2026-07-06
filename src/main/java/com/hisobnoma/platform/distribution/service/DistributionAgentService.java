package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.dto.CreateDistributionAgentRequest;
import com.hisobnoma.platform.distribution.dto.DistributionAgentDto;
import com.hisobnoma.platform.distribution.dto.DistributionTerritoryRequest;
import com.hisobnoma.platform.distribution.dto.UpdateDistributionAgentRequest;
import com.hisobnoma.platform.distribution.entity.AgentStatus;
import com.hisobnoma.platform.distribution.entity.DistributionAgent;
import com.hisobnoma.platform.distribution.entity.DistributionTerritory;
import com.hisobnoma.platform.distribution.mapper.DistributionAgentMapper;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
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
public class DistributionAgentService {

    private final DistributionAgentRepository agentRepository;
    private final DistributionAgentMapper agentMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public List<DistributionAgentDto> getAllAgents() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return agentMapper.toDtoList(agentRepository.findAllByTenantId(tenantId));
    }

    @Transactional(readOnly = true)
    public PageResponse<DistributionAgentDto> getAgents(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<DistributionAgent> page = agentRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(agentMapper::toDto));
    }

    @Transactional(readOnly = true)
    public List<DistributionAgentDto> getActiveAgents() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return agentMapper.toDtoList(
                agentRepository.findAllByTenantIdAndStatus(tenantId, AgentStatus.ACTIVE));
    }

    @Transactional(readOnly = true)
    public DistributionAgentDto getAgent(Long id) {
        return agentMapper.toDto(loadAgent(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<DistributionAgentDto> searchAgents(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<DistributionAgent> page = agentRepository.searchByTenantId(tenantId, search, pageable);
        return PageResponse.of(page.map(agentMapper::toDto));
    }

    @Transactional
    public DistributionAgentDto createAgent(CreateDistributionAgentRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        if (agentRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new DuplicateResourceException("DistributionAgent", "code", request.getCode());
        }

        DistributionAgent agent = agentMapper.toEntity(request);
        agent.setTenantId(tenantId);
        agent.setStatus(request.getStatus() != null ? request.getStatus() : AgentStatus.ACTIVE);

        replaceTerritories(agent, request.getTerritories(), tenantId);

        agent = agentRepository.save(agent);
        log.info("Created distribution agent: {} ({})", agent.getName(), agent.getCode());
        return agentMapper.toDto(agent);
    }

    @Transactional
    public DistributionAgentDto updateAgent(Long id, UpdateDistributionAgentRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        DistributionAgent agent = loadAgent(id);

        if (request.getName() != null) agent.setName(request.getName());
        if (request.getPhone() != null) agent.setPhone(request.getPhone());
        if (request.getEmail() != null) agent.setEmail(request.getEmail());
        if (request.getPhotoUrl() != null) agent.setPhotoUrl(request.getPhotoUrl());
        if (request.getUserId() != null) agent.setUserId(request.getUserId());
        if (request.getEmployeeId() != null) agent.setEmployeeId(request.getEmployeeId());
        if (request.getVehiclePlate() != null) agent.setVehiclePlate(request.getVehiclePlate());
        if (request.getVehicleName() != null) agent.setVehicleName(request.getVehicleName());
        if (request.getCommissionPercent() != null) agent.setCommissionPercent(request.getCommissionPercent());
        if (request.getHiredAt() != null) agent.setHiredAt(request.getHiredAt());
        if (request.getTerminatedAt() != null) agent.setTerminatedAt(request.getTerminatedAt());
        if (request.getNotes() != null) agent.setNotes(request.getNotes());
        if (request.getStatus() != null) agent.setStatus(request.getStatus());

        if (request.getTerritories() != null) {
            replaceTerritories(agent, request.getTerritories(), tenantId);
        }

        agent = agentRepository.save(agent);
        log.info("Updated distribution agent: {}", agent.getId());
        return agentMapper.toDto(agent);
    }

    @Transactional
    public void deleteAgent(Long id) {
        DistributionAgent agent = loadAgent(id);
        agentRepository.delete(agent);
        log.info("Deleted distribution agent: {}", id);
    }

    private DistributionAgent loadAgent(Long id) {
        return agentRepository.findByIdAndTenantId(id, securityContextHelper.getCurrentTenantId())
                .orElseThrow(() -> new NotFoundException("DistributionAgent", id));
    }

    /**
     * Replaces the agent's territory collection in place so JPA orphan-removal deletes the
     * old rows and cascades the new ones. Safe for both create (empty collection) and update.
     */
    private void replaceTerritories(DistributionAgent agent,
                                    List<DistributionTerritoryRequest> requests, Long tenantId) {
        agent.getTerritories().clear();
        if (requests == null) {
            return;
        }
        for (DistributionTerritoryRequest req : requests) {
            DistributionTerritory territory = DistributionTerritory.builder()
                    .regionId(req.getRegionId())
                    .villageId(req.getVillageId())
                    .priority(req.getPriority() != null ? req.getPriority() : 0)
                    .exclusive(req.isExclusive())
                    .active(req.isActive())
                    .tenantId(tenantId)
                    .build();
            agent.addTerritory(territory);
        }
    }
}
