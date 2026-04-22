package com.hisobnoma.platform.auth.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserPrincipalTest {

    private UserPrincipal build(Long id, String username, Long tenantId,
                                 boolean enabled, boolean accountNonLocked,
                                 List<GrantedAuthority> authorities) {
        return new UserPrincipal(id, username, "pwd", tenantId,
                enabled, accountNonLocked, authorities);
    }

    @Test
    void getAuthorities_returnsGrantedAuthoritiesForPermissions() {
        // Given
        UserPrincipal principal = build(1L, "dave", 12L, true, true,
                List.of(new SimpleGrantedAuthority("INVOICE_READ"),
                        new SimpleGrantedAuthority("INVOICE_WRITE")));

        // When
        Collection<? extends GrantedAuthority> authorities = principal.getAuthorities();

        // Then
        assertEquals(2, authorities.size());
        Set<String> codes = authorities.stream().map(GrantedAuthority::getAuthority)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(codes.contains("INVOICE_READ"));
        assertTrue(codes.contains("INVOICE_WRITE"));
    }

    @Test
    void isAccountNonExpired_activeUser_returnsTrue() {
        // Given
        UserPrincipal principal = build(1L, "dave", 1L, true, true, List.of());

        // Then — UserPrincipal always returns true (no expiry tracking)
        assertTrue(principal.isAccountNonExpired());
    }

    @Test
    void isAccountNonLocked_enabledUser_returnsTrue() {
        // Given
        UserPrincipal principal = build(1L, "dave", 1L, true, true, List.of());

        // Then
        assertTrue(principal.isAccountNonLocked());
    }

    @Test
    void isAccountNonLocked_lockedUser_returnsFalse() {
        // Given
        UserPrincipal principal = build(1L, "dave", 1L, true, false, List.of());

        // Then
        assertFalse(principal.isAccountNonLocked());
    }

    @Test
    void isEnabled_enabledUser_returnsTrue() {
        // Given
        UserPrincipal principal = build(1L, "dave", 1L, true, true, List.of());

        // Then
        assertTrue(principal.isEnabled());
    }

    @Test
    void isEnabled_disabledUser_returnsFalse() {
        // Given
        UserPrincipal principal = build(1L, "dave", 1L, false, true, List.of());

        // Then
        assertFalse(principal.isEnabled());
    }

    @Test
    void getUsername_returnsUsernameString() {
        // Given
        UserPrincipal principal = build(1L, "dave", 1L, true, true, List.of());

        // Then
        assertEquals("dave", principal.getUsername());
    }

    @Test
    void getTenantId_returnsLong() {
        // Given
        UserPrincipal principal = build(1L, "dave", 12L, true, true, List.of());

        // Then
        assertEquals(12L, principal.getTenantId());
    }

    @Test
    void isCredentialsNonExpired_alwaysReturnsTrue() {
        // Given
        UserPrincipal principal = build(1L, "dave", 1L, true, true, List.of());

        // Then
        assertTrue(principal.isCredentialsNonExpired());
    }

    @Test
    void hasPermission_permissionPresent_returnsTrue() {
        // Given
        UserPrincipal principal = build(1L, "dave", 1L, true, true,
                List.of(new SimpleGrantedAuthority("INVENTORY_READ")));

        // Then
        assertTrue(principal.hasPermission("INVENTORY_READ"));
        assertFalse(principal.hasPermission("OTHER"));
    }

    @Test
    void hasRole_rolePresent_returnsTrue() {
        // Given — role authorities carry "ROLE_" prefix
        UserPrincipal principal = build(1L, "dave", 1L, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        // Then
        assertTrue(principal.hasRole("ADMIN"));
        assertFalse(principal.hasRole("CASHIER"));
    }

    @Test
    void getId_returnsLong() {
        // Given
        UserPrincipal principal = build(77L, "dave", 1L, true, true, List.of());

        // Then
        assertEquals(77L, principal.getId());
    }

    @Test
    void getPassword_returnsPassword() {
        // Given
        UserPrincipal principal = new UserPrincipal(
                1L, "dave", "secret-hash", 1L, true, true, List.of());

        // Then
        assertEquals("secret-hash", principal.getPassword());
    }
}
