package com.hisobnoma.platform.auth.security;

import com.hisobnoma.platform.common.exception.UnauthorizedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SecurityContextHelperTest {

    private SecurityContextHelper securityContextHelper;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        securityContextHelper = new SecurityContextHelper();
        userPrincipal = new UserPrincipal(
                1L,
                "testuser",
                "password",
                100L,
                true,
                true,
                Set.of(
                        new SimpleGrantedAuthority("INVENTORY_PRODUCT_READ"),
                        new SimpleGrantedAuthority("INVENTORY_PRODUCT_CREATE"),
                        new SimpleGrantedAuthority("ROLE_INVENTORY_MANAGER")
                )
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnCurrentUserWhenAuthenticated() {
        // Given
        setAuthentication(userPrincipal);

        // When
        var result = securityContextHelper.getCurrentUser();

        // Then
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
    }

    @Test
    void shouldReturnEmptyWhenNotAuthenticated() {
        // When
        var result = securityContextHelper.getCurrentUser();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldThrowWhenRequiredCurrentUserNotAuthenticated() {
        // When/Then
        assertThrows(UnauthorizedException.class, () ->
                securityContextHelper.getRequiredCurrentUser());
    }

    @Test
    void shouldReturnUserIdWhenAuthenticated() {
        // Given
        setAuthentication(userPrincipal);

        // When
        Long userId = securityContextHelper.getCurrentUserId();

        // Then
        assertEquals(1L, userId);
    }

    @Test
    void getCurrentUserId_unauthenticatedContext_throwsUnauthorizedException() {
        // When / Then — actual contract throws UnauthorizedException for missing auth
        assertThrows(UnauthorizedException.class, () ->
                securityContextHelper.getCurrentUserId());
    }

    @Test
    void getCurrentUsername_authenticatedContext_returnsString() {
        // Given
        setAuthentication(userPrincipal);

        // When
        String username = securityContextHelper.getCurrentUsername();

        // Then
        assertEquals("testuser", username);
    }

    @Test
    void getCurrentUsername_unauthenticatedContext_throwsUnauthorizedException() {
        // When / Then
        assertThrows(UnauthorizedException.class, () ->
                securityContextHelper.getCurrentUsername());
    }

    @Test
    void getCurrentTenantId_unauthenticatedContext_throwsUnauthorizedException() {
        // When / Then
        assertThrows(UnauthorizedException.class, () ->
                securityContextHelper.getCurrentTenantId());
    }

    @Test
    void shouldReturnTenantIdWhenAuthenticated() {
        // Given
        setAuthentication(userPrincipal);

        // When
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Then
        assertEquals(100L, tenantId);
    }

    @Test
    void shouldCheckPermissionCorrectly() {
        // Given
        setAuthentication(userPrincipal);

        // When/Then
        assertTrue(securityContextHelper.hasPermission("INVENTORY_PRODUCT_READ"));
        assertFalse(securityContextHelper.hasPermission("FINANCE_MANAGE"));
    }

    @Test
    void shouldCheckRoleCorrectly() {
        // Given
        setAuthentication(userPrincipal);

        // When/Then
        assertTrue(securityContextHelper.hasRole("INVENTORY_MANAGER"));
        assertFalse(securityContextHelper.hasRole("ADMIN"));
    }

    @Test
    void shouldCheckAnyPermissionCorrectly() {
        // Given
        setAuthentication(userPrincipal);

        // When/Then
        assertTrue(securityContextHelper.hasAnyPermission("INVENTORY_PRODUCT_READ", "FINANCE_MANAGE"));
        assertFalse(securityContextHelper.hasAnyPermission("FINANCE_MANAGE", "ADMIN_MANAGE"));
    }

    @Test
    void shouldCheckAllPermissionsCorrectly() {
        // Given
        setAuthentication(userPrincipal);

        // When/Then
        assertTrue(securityContextHelper.hasAllPermissions("INVENTORY_PRODUCT_READ", "INVENTORY_PRODUCT_CREATE"));
        assertFalse(securityContextHelper.hasAllPermissions("INVENTORY_PRODUCT_READ", "FINANCE_MANAGE"));
    }

    @Test
    void shouldReturnIsAuthenticatedCorrectly() {
        // Given - not authenticated
        assertFalse(securityContextHelper.isAuthenticated());

        // When
        setAuthentication(userPrincipal);

        // Then
        assertTrue(securityContextHelper.isAuthenticated());
    }

    private void setAuthentication(UserPrincipal principal) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
