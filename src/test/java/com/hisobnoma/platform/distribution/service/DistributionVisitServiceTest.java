package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.distribution.dto.DistributionVisitDto;
import com.hisobnoma.platform.distribution.dto.VisitCheckInRequest;
import com.hisobnoma.platform.distribution.dto.VisitCheckOutRequest;
import com.hisobnoma.platform.distribution.entity.DistributionAgent;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributionVisitServiceTest {

    @Mock private DistributionVisitRepository visitRepository;
    @Mock private DistributionVisitMapper visitMapper;
    @Mock private DistributionAgentRepository agentRepository;
    @Mock private DistributionRouteRepository routeRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks private DistributionVisitService service;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        lenient().when(visitMapper.toDto(any(DistributionVisit.class))).thenReturn(new DistributionVisitDto());
    }

    private Customer customer() {
        Customer c = Customer.builder().code("C-1").name("Osiyo").build();
        c.setId(100L);
        return c;
    }

    @Test
    void checkIn_createsPendingVisitWithGps() {
        VisitCheckInRequest request = VisitCheckInRequest.builder()
                .agentId(5L).customerId(100L).routeId(80L)
                .visitType(VisitType.PLANNED)
                .latitude(new BigDecimal("41.3111")).longitude(new BigDecimal("69.2797"))
                .build();
        when(agentRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(new DistributionAgent()));
        when(customerRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(customer()));
        DistributionRoute route = DistributionRoute.builder().stops(new java.util.ArrayList<>()).build();
        route.setId(80L);
        when(routeRepository.findByIdAndTenantId(80L, TENANT_ID)).thenReturn(Optional.of(route));
        when(visitRepository.save(any(DistributionVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        service.checkIn(request);

        ArgumentCaptor<DistributionVisit> captor = ArgumentCaptor.forClass(DistributionVisit.class);
        verify(visitRepository).save(captor.capture());
        DistributionVisit v = captor.getValue();
        assertEquals(VisitOutcome.PENDING, v.getOutcome());
        assertEquals("Osiyo", v.getCustomerName());
        assertNotNull(v.getCheckInAt());
        assertEquals(0, new BigDecimal("41.3111").compareTo(v.getCheckInLat()));
        assertEquals(TENANT_ID, v.getTenantId());
    }

    @Test
    void checkIn_unknownAgent_throws() {
        VisitCheckInRequest request = VisitCheckInRequest.builder().agentId(9L).customerId(100L).build();
        when(agentRepository.findByIdAndTenantId(9L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.checkIn(request));
        verify(visitRepository, never()).save(any());
    }

    @Test
    void checkIn_routeFromAnotherTenant_throws() {
        VisitCheckInRequest request = VisitCheckInRequest.builder()
                .agentId(5L).customerId(100L).routeId(999L).build();
        when(agentRepository.findByIdAndTenantId(5L, TENANT_ID)).thenReturn(Optional.of(new DistributionAgent()));
        when(customerRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(customer()));
        when(routeRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.checkIn(request));
        verify(visitRepository, never()).save(any());
    }

    @Test
    void checkOut_setsOutcomeCheckoutTimeAndOrderLink() {
        DistributionVisit visit = DistributionVisit.builder()
                .agentId(5L).customerId(100L).outcome(VisitOutcome.PENDING).tenantId(TENANT_ID).build();
        visit.setId(300L);
        when(visitRepository.findByIdAndTenantId(300L, TENANT_ID)).thenReturn(Optional.of(visit));
        when(visitRepository.save(any(DistributionVisit.class))).thenAnswer(inv -> inv.getArgument(0));

        service.checkOut(300L, VisitCheckOutRequest.builder()
                .outcome(VisitOutcome.ORDER_PLACED).distributionOrderId(50L)
                .latitude(new BigDecimal("41.0")).build());

        assertEquals(VisitOutcome.ORDER_PLACED, visit.getOutcome());
        assertNotNull(visit.getCheckOutAt());
        assertEquals(50L, visit.getDistributionOrderId());
        assertEquals(0, new BigDecimal("41.0").compareTo(visit.getCheckOutLat()));
    }

    @Test
    void checkOut_notFound_throws() {
        when(visitRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> service.checkOut(999L, VisitCheckOutRequest.builder().outcome(VisitOutcome.CLOSED).build()));
    }
}
