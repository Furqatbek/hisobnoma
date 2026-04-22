package com.hisobnoma.platform.auth.security;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock private JwtTokenProvider tokenProvider;
    @Mock private UserDetailsService userDetailsService;
    @Mock private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private UserPrincipal buildPrincipal() {
        return new UserPrincipal(
                10L, "alice", "pwd", 1L, true, true, Collections.emptyList());
    }

    @Test
    void doFilterInternal_validBearerToken_populatesSecurityContext() throws Exception {
        // Given
        UserPrincipal principal = buildPrincipal();
        request.addHeader("Authorization", "Bearer valid-token");
        when(tokenProvider.validateToken("valid-token")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("valid-token")).thenReturn("alice");
        when(userDetailsService.loadUserByUsername("alice")).thenReturn(principal);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(principal, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_missingAuthorizationHeader_contextEmpty() throws Exception {
        // Given — no Authorization header

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(tokenProvider);
    }

    @Test
    void doFilterInternal_invalidToken_contextNotPopulated() throws Exception {
        // Given
        request.addHeader("Authorization", "Bearer invalid-token");
        when(tokenProvider.validateToken("invalid-token")).thenReturn(false);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
    }

    @Test
    void doFilterInternal_expiredToken_contextNotPopulatedAndChainContinues() throws Exception {
        // Given — expired tokens return false from validateToken (exception caught internally)
        request.addHeader("Authorization", "Bearer expired-token");
        when(tokenProvider.validateToken("expired-token")).thenReturn(false);

        // When
        filter.doFilter(request, response, filterChain);

        // Then — filter does not set 401 itself; chain is called; context empty
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_tokenForNonExistentUser_contextNotPopulated() throws Exception {
        // Given
        request.addHeader("Authorization", "Bearer valid-token-ghost-user");
        when(tokenProvider.validateToken("valid-token-ghost-user")).thenReturn(true);
        when(tokenProvider.getUsernameFromToken("valid-token-ghost-user")).thenReturn("ghost");
        when(userDetailsService.loadUserByUsername("ghost"))
                .thenThrow(new UsernameNotFoundException("User not found"));

        // When — should NOT throw
        assertDoesNotThrow(() -> filter.doFilter(request, response, filterChain));

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void doFilterInternal_headerWithoutBearerPrefix_contextNotPopulated() throws Exception {
        // Given — Authorization header present but not Bearer
        request.addHeader("Authorization", "Basic dXNlcjpwYXNz");

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, times(1)).doFilter(request, response);
        verifyNoInteractions(tokenProvider);
    }
}
