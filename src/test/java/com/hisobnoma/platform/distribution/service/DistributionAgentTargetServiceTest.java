package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.dto.CreateDistributionAgentTargetRequest;
import com.hisobnoma.platform.distribution.dto.DistributionAgentTargetDto;
import com.hisobnoma.platform.distribution.entity.DistributionAgent;
import com.hisobnoma.platform.distribution.entity.DistributionAgentTarget;
import com.hisobnoma.platform.distribution.mapper.DistributionAgentTargetMapper;
import com.hisobnoma.platform.distribution.repository.DistributionAgentRepository;
import com.hisobnoma.platform.distribution.repository.DistributionAgentTargetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributionAgentTargetServiceTest {

    @Mock private DistributionAgentTargetRepository targetRepository;
    @Mock private DistributionAgentTargetMapper targetMapper;
    @Mock private DistributionAgentRepository agentRepository;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks private DistributionAgentTargetService service;

    private static final Long TENANT_ID = 1L;
    private static final LocalDate START = LocalDate.of(2026, 7, 1);
    private static final LocalDate END = LocalDate.of(2026, 7, 31);

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        lenient().when(targetMapper.toDto(any(DistributionAgentTarget.class))).thenReturn(new DistributionAgentTargetDto());
    }

    private CreateDistributionAgentTargetRequest request() {
        return CreateDistributionAgentTargetRequest.builder()
                .agentId(5L).periodStart(START).periodEnd(END)
                .targetRevenue(new BigDecimal("600000")).targetOrders(10).build();
    }

    @Test
    void createTarget_success() {
        when(agentRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(new DistributionAgent()));
        when(targetRepository.existsByTenantIdAndAgentIdAndPeriodStartAndPeriodEnd(TENANT_ID, 5L, START, END)).thenReturn(false);
        when(targetRepository.save(any(DistributionAgentTarget.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createTarget(request());

        ArgumentCaptor<DistributionAgentTarget> captor = ArgumentCaptor.forClass(DistributionAgentTarget.class);
        verify(targetRepository).save(captor.capture());
        DistributionAgentTarget saved = captor.getValue();
        assertEquals(TENANT_ID, saved.getTenantId());
        assertEquals(0, new BigDecimal("600000").compareTo(saved.getTargetRevenue()));
        assertEquals(10, saved.getTargetOrders());
        assertEquals(0, saved.getTargetVisits()); // defaulted
    }

    @Test
    void createTarget_unknownAgent_throws() {
        when(agentRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.createTarget(request()));
        verify(targetRepository, never()).save(any());
    }

    @Test
    void createTarget_endBeforeStart_throws() {
        CreateDistributionAgentTargetRequest bad = CreateDistributionAgentTargetRequest.builder()
                .agentId(5L).periodStart(END).periodEnd(START).build();
        when(agentRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(new DistributionAgent()));
        assertThrows(BusinessException.class, () -> service.createTarget(bad));
    }

    @Test
    void createTarget_duplicatePeriod_throws() {
        when(agentRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(new DistributionAgent()));
        when(targetRepository.existsByTenantIdAndAgentIdAndPeriodStartAndPeriodEnd(TENANT_ID, 5L, START, END)).thenReturn(true);
        assertThrows(DuplicateResourceException.class, () -> service.createTarget(request()));
    }

    @Test
    void deleteTarget_notFound_throws() {
        when(targetRepository.findByIdAndTenantId(9L, TENANT_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.deleteTarget(9L));
    }
}
