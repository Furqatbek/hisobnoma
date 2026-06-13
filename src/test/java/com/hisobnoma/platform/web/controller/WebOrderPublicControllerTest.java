package com.hisobnoma.platform.web.controller;

import com.hisobnoma.platform.common.util.ClientIpResolver;
import com.hisobnoma.platform.web.exception.TooManyRequestsException;
import com.hisobnoma.platform.web.service.CheckoutRateLimiter;
import com.hisobnoma.platform.web.service.WebOrderPublicService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebOrderPublicControllerTest {

    @Mock private ClientIpResolver clientIpResolver;
    @Mock private WebOrderPublicService publicService;
    @Mock private CheckoutRateLimiter rateLimiter;

    @InjectMocks
    private WebOrderPublicController controller;

    @Test
    void orderLookup_throttledReturns429AndNeverHitsService() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(false);

        assertThrows(TooManyRequestsException.class, () ->
                controller.getOrderStatus("WO-000001", "+998901234567", new MockHttpServletRequest()));

        // The enumeration guard must short-circuit before the DB lookup.
        verify(publicService, never()).getOrderStatus(any(), any());
    }

    @Test
    void orderLookup_withinLimitReachesService() {
        when(rateLimiter.tryAcquire(anyString())).thenReturn(true);

        controller.getOrderStatus("WO-000001", "+998901234567", new MockHttpServletRequest());

        verify(publicService).getOrderStatus("WO-000001", "+998901234567");
    }
}
