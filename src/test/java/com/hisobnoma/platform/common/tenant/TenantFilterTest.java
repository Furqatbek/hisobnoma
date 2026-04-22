package com.hisobnoma.platform.common.tenant;

import com.hisobnoma.platform.auth.security.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantFilterTest {

    @Mock private JwtTokenProvider tokenProvider;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private TenantFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        TenantContext.clear();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void extractAndSet_jwtContainsTenantId_setsContextDuringChain() throws Exception {
        // Given
        request.addHeader("Authorization", "Bearer valid-token");
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getTenantIdFromToken("valid-token")).thenReturn(7L);

        // Capture tenant inside the chain (before finally-clear runs)
        AtomicReference<Long> tenantDuringChain = new AtomicReference<>();
        doAnswer(inv -> {
            tenantDuringChain.set(TenantContext.getCurrentTenant());
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertEquals(7L, tenantDuringChain.get());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void extractAndSet_noJwt_tenantContextNotSet() throws Exception {
        // Given — no Authorization header, no X-Tenant-ID header
        AtomicReference<Long> tenantDuringChain = new AtomicReference<>();
        doAnswer(inv -> {
            tenantDuringChain.set(TenantContext.getCurrentTenant());
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertNull(tenantDuringChain.get());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void afterFilterCompletes_contextCleared_noLeak() throws Exception {
        // Given
        request.addHeader("Authorization", "Bearer valid-token");
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getTenantIdFromToken("valid-token")).thenReturn(42L);

        // When
        filter.doFilter(request, response, filterChain);

        // Then — context cleared after chain completion
        assertNull(TenantContext.getCurrentTenant());
        assertFalse(TenantContext.hasTenant());
    }

    @Test
    void afterFilterException_contextStillCleared() throws Exception {
        // Given — chain throws, filter must still clear context
        request.addHeader("Authorization", "Bearer valid-token");
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getTenantIdFromToken("valid-token")).thenReturn(42L);
        doThrow(new IOException("simulated chain failure")).when(filterChain).doFilter(request, response);

        // When / Then
        assertThrows(IOException.class, () ->
                filter.doFilter(request, response, filterChain));
        // Context is cleared despite exception
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void extractAndSet_invalidJwtButTenantHeaderPresent_usesHeader() throws Exception {
        // Given — JWT invalid, X-Tenant-ID header fallback kicks in
        request.addHeader("Authorization", "Bearer bad-token");
        request.addHeader("X-Tenant-ID", "99");
        when(tokenProvider.validateToken("bad-token")).thenReturn(false);

        AtomicReference<Long> tenantDuringChain = new AtomicReference<>();
        doAnswer(inv -> {
            tenantDuringChain.set(TenantContext.getCurrentTenant());
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertEquals(99L, tenantDuringChain.get());
    }

    @Test
    void extractAndSet_malformedTenantHeader_tenantContextNotSet() throws Exception {
        // Given — header is non-numeric
        request.addHeader("X-Tenant-ID", "not-a-number");

        AtomicReference<Long> tenantDuringChain = new AtomicReference<>();
        doAnswer(inv -> {
            tenantDuringChain.set(TenantContext.getCurrentTenant());
            return null;
        }).when(filterChain).doFilter(request, response);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertNull(tenantDuringChain.get());
    }
}
