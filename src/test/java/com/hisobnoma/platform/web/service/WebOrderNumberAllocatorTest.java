package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.web.entity.WebOrderCounter;
import com.hisobnoma.platform.web.repository.WebOrderCounterRepository;
import com.hisobnoma.platform.web.repository.WebOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebOrderNumberAllocatorTest {

    @Mock private WebOrderCounterRepository counterRepository;
    @Mock private WebOrderRepository orderRepository;
    @InjectMocks private WebOrderNumberAllocator allocator;

    private static final Long TENANT = 4L;

    @Test
    void next_existingCounter_returnsFormattedNumberAndIncrements() {
        when(counterRepository.findByTenantId(TENANT))
                .thenReturn(Optional.of(new WebOrderCounter(TENANT, 42)));

        String number = allocator.next(TENANT);

        assertEquals("WO-000042", number);
        ArgumentCaptor<WebOrderCounter> captor = ArgumentCaptor.forClass(WebOrderCounter.class);
        verify(counterRepository).saveAndFlush(captor.capture());
        assertEquals(43, captor.getValue().getNextNumber());
    }

    @Test
    void next_missingCounter_seedsFromExistingOrderCount() {
        // Tenant created after the V80 seed migration: counter starts at count + 1.
        when(counterRepository.findByTenantId(TENANT)).thenReturn(Optional.empty());
        when(orderRepository.countByTenantIdAndCreatedAtAfter(eq(TENANT), any())).thenReturn(7L);

        String number = allocator.next(TENANT);

        assertEquals("WO-000008", number);
        ArgumentCaptor<WebOrderCounter> captor = ArgumentCaptor.forClass(WebOrderCounter.class);
        verify(counterRepository).saveAndFlush(captor.capture());
        assertEquals(TENANT, captor.getValue().getTenantId());
        assertEquals(9, captor.getValue().getNextNumber());
    }
}
