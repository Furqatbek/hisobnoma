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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributionAgentServiceTest {

    @Mock
    private DistributionAgentRepository agentRepository;
    @Mock
    private DistributionAgentMapper agentMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private DistributionAgentService agentService;

    private static final Long TENANT_ID = 1L;
    private DistributionAgent agent;
    private DistributionAgentDto agentDto;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        agent = DistributionAgent.builder()
                .id(1L)
                .code("AG-001")
                .name("Alisher")
                .phone("+998901112233")
                .status(AgentStatus.ACTIVE)
                .tenantId(TENANT_ID)
                .territories(new ArrayList<>())
                .build();

        agentDto = DistributionAgentDto.builder()
                .id(1L)
                .code("AG-001")
                .name("Alisher")
                .status(AgentStatus.ACTIVE)
                .build();
    }

    @Test
    void getAgents_returnsPaged() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<DistributionAgent> page = new PageImpl<>(List.of(agent), pageable, 1);
        when(agentRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(agentMapper.toDto(agent)).thenReturn(agentDto);

        PageResponse<DistributionAgentDto> result = agentService.getAgents(pageable);

        assertEquals(1, result.getContent().size());
        assertEquals("Alisher", result.getContent().get(0).getName());
    }

    @Test
    void getAllAgents_returnsList() {
        when(agentRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(agent));
        when(agentMapper.toDtoList(List.of(agent))).thenReturn(List.of(agentDto));

        assertEquals(1, agentService.getAllAgents().size());
    }

    @Test
    void getActiveAgents_returnsActiveOnly() {
        when(agentRepository.findAllByTenantIdAndStatus(TENANT_ID, AgentStatus.ACTIVE))
                .thenReturn(List.of(agent));
        when(agentMapper.toDtoList(List.of(agent))).thenReturn(List.of(agentDto));

        List<DistributionAgentDto> result = agentService.getActiveAgents();

        assertEquals(1, result.size());
        assertEquals(AgentStatus.ACTIVE, result.get(0).getStatus());
    }

    @Test
    void getAgent_found_returnsDto() {
        when(agentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(agent));
        when(agentMapper.toDto(agent)).thenReturn(agentDto);

        assertEquals("Alisher", agentService.getAgent(1L).getName());
    }

    @Test
    void getAgent_notFound_throws() {
        when(agentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> agentService.getAgent(999L));
    }

    @Test
    void searchAgents_returnsMatches() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<DistributionAgent> page = new PageImpl<>(List.of(agent), pageable, 1);
        when(agentRepository.searchByTenantId(TENANT_ID, "Ali", pageable)).thenReturn(page);
        when(agentMapper.toDto(agent)).thenReturn(agentDto);

        PageResponse<DistributionAgentDto> result = agentService.searchAgents("Ali", pageable);

        assertEquals(1, result.getContent().size());
    }

    @Test
    void createAgent_success_attachesTerritoriesWithTenantAndBackref() {
        CreateDistributionAgentRequest request = CreateDistributionAgentRequest.builder()
                .code("AG-002")
                .name("Bek")
                .territories(List.of(
                        DistributionTerritoryRequest.builder().regionId(5L).priority(1).build(),
                        DistributionTerritoryRequest.builder().regionId(6L).villageId(60L).exclusive(true).build()))
                .build();

        DistributionAgent mapped = DistributionAgent.builder()
                .code("AG-002").name("Bek").territories(new ArrayList<>()).build();

        when(agentRepository.existsByCodeAndTenantId("AG-002", TENANT_ID)).thenReturn(false);
        when(agentMapper.toEntity(request)).thenReturn(mapped);
        when(agentRepository.save(any(DistributionAgent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentMapper.toDto(any(DistributionAgent.class))).thenReturn(agentDto);

        agentService.createAgent(request);

        ArgumentCaptor<DistributionAgent> captor = ArgumentCaptor.forClass(DistributionAgent.class);
        verify(agentRepository).save(captor.capture());
        DistributionAgent saved = captor.getValue();

        assertEquals(TENANT_ID, saved.getTenantId());
        assertEquals(AgentStatus.ACTIVE, saved.getStatus());
        assertEquals(2, saved.getTerritories().size());
        DistributionTerritory first = saved.getTerritories().get(0);
        assertEquals(5L, first.getRegionId());
        assertEquals(TENANT_ID, first.getTenantId());
        assertSame(saved, first.getAgent(), "territory back-reference must point to the agent");
        assertTrue(saved.getTerritories().get(1).isExclusive());
    }

    @Test
    void createAgent_defaultsStatusToActiveWhenNull() {
        CreateDistributionAgentRequest request = CreateDistributionAgentRequest.builder()
                .code("AG-003").name("Dilnoza").build();
        DistributionAgent mapped = DistributionAgent.builder()
                .code("AG-003").name("Dilnoza").territories(new ArrayList<>()).build();
        mapped.setStatus(null);

        when(agentRepository.existsByCodeAndTenantId("AG-003", TENANT_ID)).thenReturn(false);
        when(agentMapper.toEntity(request)).thenReturn(mapped);
        when(agentRepository.save(any(DistributionAgent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentMapper.toDto(any(DistributionAgent.class))).thenReturn(agentDto);

        agentService.createAgent(request);

        ArgumentCaptor<DistributionAgent> captor = ArgumentCaptor.forClass(DistributionAgent.class);
        verify(agentRepository).save(captor.capture());
        assertEquals(AgentStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void createAgent_duplicateCode_throws() {
        CreateDistributionAgentRequest request = CreateDistributionAgentRequest.builder()
                .code("AG-001").name("Alisher").build();
        when(agentRepository.existsByCodeAndTenantId("AG-001", TENANT_ID)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> agentService.createAgent(request));
        verify(agentRepository, never()).save(any());
    }

    @Test
    void updateAgent_appliesScalarFieldsAndStatus() {
        UpdateDistributionAgentRequest request = UpdateDistributionAgentRequest.builder()
                .name("Alisher Updated")
                .status(AgentStatus.SUSPENDED)
                .build();

        when(agentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(DistributionAgent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentMapper.toDto(agent)).thenReturn(agentDto);

        agentService.updateAgent(1L, request);

        assertEquals("Alisher Updated", agent.getName());
        assertEquals(AgentStatus.SUSPENDED, agent.getStatus());
    }

    @Test
    void updateAgent_replacesTerritoriesWhenProvided() {
        agent.addTerritory(DistributionTerritory.builder().regionId(99L).build());
        UpdateDistributionAgentRequest request = UpdateDistributionAgentRequest.builder()
                .territories(List.of(DistributionTerritoryRequest.builder().regionId(7L).build()))
                .build();

        when(agentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(DistributionAgent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentMapper.toDto(agent)).thenReturn(agentDto);

        agentService.updateAgent(1L, request);

        assertEquals(1, agent.getTerritories().size());
        assertEquals(7L, agent.getTerritories().get(0).getRegionId());
    }

    @Test
    void updateAgent_nullTerritoriesLeavesExistingUntouched() {
        agent.addTerritory(DistributionTerritory.builder().regionId(99L).build());
        UpdateDistributionAgentRequest request = UpdateDistributionAgentRequest.builder()
                .name("Renamed").build();

        when(agentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(agent));
        when(agentRepository.save(any(DistributionAgent.class))).thenAnswer(inv -> inv.getArgument(0));
        when(agentMapper.toDto(agent)).thenReturn(agentDto);

        agentService.updateAgent(1L, request);

        assertEquals(1, agent.getTerritories().size());
        assertEquals(99L, agent.getTerritories().get(0).getRegionId());
    }

    @Test
    void updateAgent_notFound_throws() {
        UpdateDistributionAgentRequest request = UpdateDistributionAgentRequest.builder().name("x").build();
        when(agentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> agentService.updateAgent(999L, request));
    }

    @Test
    void deleteAgent_success() {
        when(agentRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(agent));

        agentService.deleteAgent(1L);

        verify(agentRepository).delete(agent);
    }

    @Test
    void deleteAgent_notFound_throws() {
        when(agentRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> agentService.deleteAgent(999L));
    }
}
