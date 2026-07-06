package com.hisobnoma.platform.distribution.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DistributionRouteServiceTest {

    @Mock private DistributionRouteRepository routeRepository;
    @Mock private DistributionRouteMapper routeMapper;
    @Mock private DistributionAgentRepository agentRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks private DistributionRouteService service;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        lenient().when(routeMapper.toDto(any(DistributionRoute.class))).thenReturn(new DistributionRouteDto());
    }

    private Customer customer(Long id, String name) {
        Customer c = Customer.builder().code("C-" + id).name(name).build();
        c.setId(id);
        return c;
    }

    private DistributionRoute route() {
        DistributionRoute r = DistributionRoute.builder()
                .code("R-1").name("Chilonzor").status(RouteStatus.DRAFT)
                .tenantId(TENANT_ID).stops(new ArrayList<>()).build();
        r.setId(80L);
        return r;
    }

    @Test
    void createRoute_snapshotsStopCustomerNames() {
        CreateDistributionRouteRequest request = CreateDistributionRouteRequest.builder()
                .code("R-1").name("Chilonzor")
                .stops(List.of(
                        DistributionRouteStopRequest.builder().customerId(100L).sortOrder(1).build(),
                        DistributionRouteStopRequest.builder().customerId(101L).sortOrder(2).build()))
                .build();

        when(routeRepository.existsByCodeAndTenantId("R-1", TENANT_ID)).thenReturn(false);
        when(customerRepository.findByIdAndTenantId(100L, TENANT_ID)).thenReturn(Optional.of(customer(100L, "Osiyo")));
        when(customerRepository.findByIdAndTenantId(101L, TENANT_ID)).thenReturn(Optional.of(customer(101L, "Baraka")));
        when(routeRepository.save(any(DistributionRoute.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createRoute(request);

        ArgumentCaptor<DistributionRoute> captor = ArgumentCaptor.forClass(DistributionRoute.class);
        verify(routeRepository).save(captor.capture());
        DistributionRoute saved = captor.getValue();
        assertEquals(RouteStatus.DRAFT, saved.getStatus());
        assertEquals(2, saved.getStops().size());
        assertEquals("Osiyo", saved.getStops().get(0).getCustomerName());
        assertEquals(TENANT_ID, saved.getStops().get(0).getTenantId());
        assertSame(saved, saved.getStops().get(0).getRoute());
    }

    @Test
    void createRoute_duplicateCode_throws() {
        CreateDistributionRouteRequest request = CreateDistributionRouteRequest.builder().code("R-1").name("X").build();
        when(routeRepository.existsByCodeAndTenantId("R-1", TENANT_ID)).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.createRoute(request));
        verify(routeRepository, never()).save(any());
    }

    @Test
    void createRoute_unknownStopCustomer_throws() {
        CreateDistributionRouteRequest request = CreateDistributionRouteRequest.builder()
                .code("R-2").name("X")
                .stops(List.of(DistributionRouteStopRequest.builder().customerId(999L).build()))
                .build();
        when(routeRepository.existsByCodeAndTenantId("R-2", TENANT_ID)).thenReturn(false);
        when(customerRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.createRoute(request));
    }

    @Test
    void updateRoute_replacesStopsWhenProvided() {
        DistributionRoute route = route();
        route.addStop(DistributionRouteStop.builder().customerId(1L).customerName("Old").build());
        when(routeRepository.findByIdAndTenantId(80L, TENANT_ID)).thenReturn(Optional.of(route));
        when(customerRepository.findByIdAndTenantId(200L, TENANT_ID)).thenReturn(Optional.of(customer(200L, "New")));
        when(routeRepository.save(any(DistributionRoute.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateRoute(80L, UpdateDistributionRouteRequest.builder()
                .stops(List.of(DistributionRouteStopRequest.builder().customerId(200L).build())).build());

        assertEquals(1, route.getStops().size());
        assertEquals("New", route.getStops().get(0).getCustomerName());
    }

    @Test
    void updateRoute_nullStopsLeavesExisting() {
        DistributionRoute route = route();
        route.addStop(DistributionRouteStop.builder().customerId(1L).customerName("Keep").build());
        when(routeRepository.findByIdAndTenantId(80L, TENANT_ID)).thenReturn(Optional.of(route));
        when(routeRepository.save(any(DistributionRoute.class))).thenAnswer(inv -> inv.getArgument(0));

        service.updateRoute(80L, UpdateDistributionRouteRequest.builder().name("Renamed").status(RouteStatus.ACTIVE).build());

        assertEquals("Renamed", route.getName());
        assertEquals(RouteStatus.ACTIVE, route.getStatus());
        assertEquals(1, route.getStops().size());
    }

    @Test
    void getRoute_notFound_throws() {
        when(routeRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.getRoute(999L));
    }

    @Test
    void deleteRoute_deletes() {
        DistributionRoute route = route();
        when(routeRepository.findByIdAndTenantId(80L, TENANT_ID)).thenReturn(Optional.of(route));
        service.deleteRoute(80L);
        verify(routeRepository).delete(route);
    }
}
