package com.hisobnoma.platform.auth.service;

import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.PermissionRepository;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PermissionRepository permissionRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        Permission viewPerm = Permission.builder().code("INVENTORY_READ").build();
        Role cashier = Role.builder()
                .code("CASHIER")
                .name("Cashier")
                .permissions(Set.of(viewPerm))
                .build();

        sampleUser = User.builder()
                .username("john")
                .phone("+998901234567")
                .passwordHash("$2a$10$hash")
                .enabled(true)
                .locked(false)
                .roles(new HashSet<>(Set.of(cashier)))
                .build();
        sampleUser.setId(10L);
        sampleUser.setTenantId(1L);
    }

    // ---- loadUserByUsername ----

    @Test
    void loadUserByUsername_existingUser_returnsUserPrincipalWithAuthorities() {
        // Given
        when(userRepository.findByUsernameWithRolesAndPermissions("john"))
                .thenReturn(Optional.of(sampleUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername("john");

        // Then
        assertNotNull(result);
        assertTrue(result instanceof UserPrincipal);
        UserPrincipal principal = (UserPrincipal) result;
        assertEquals("john", principal.getUsername());
        assertEquals("$2a$10$hash", principal.getPassword());
        assertFalse(principal.getAuthorities().isEmpty());
        // Should have role authority + permission authority
        assertTrue(principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CASHIER")));
        assertTrue(principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("INVENTORY_READ")));
    }

    @Test
    void loadUserByUsername_nonExistentUser_throwsUsernameNotFoundException() {
        // Given
        when(userRepository.findByUsernameWithRolesAndPermissions("ghost"))
                .thenReturn(Optional.empty());
        when(userRepository.findByPhoneWithRolesAndPermissions("ghost"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(UsernameNotFoundException.class, () ->
                customUserDetailsService.loadUserByUsername("ghost"));
    }

    // ---- loadUserByUsername — phone fallback ----

    @Test
    void loadUserByUsername_withPhoneAsInput_fallsBackToPhoneLookup() {
        // Given — username lookup fails, phone lookup succeeds
        when(userRepository.findByUsernameWithRolesAndPermissions("+998901234567"))
                .thenReturn(Optional.empty());
        when(userRepository.findByPhoneWithRolesAndPermissions("+998901234567"))
                .thenReturn(Optional.of(sampleUser));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername("+998901234567");

        // Then
        assertNotNull(result);
        assertEquals("john", result.getUsername());
    }

    @Test
    void loadUserByUsername_unknownPhone_throwsUsernameNotFoundException() {
        // Given — neither username nor phone match
        when(userRepository.findByUsernameWithRolesAndPermissions("+998900000000"))
                .thenReturn(Optional.empty());
        when(userRepository.findByPhoneWithRolesAndPermissions("+998900000000"))
                .thenReturn(Optional.empty());

        // When / Then
        assertThrows(UsernameNotFoundException.class, () ->
                customUserDetailsService.loadUserByUsername("+998900000000"));
    }

    // ---- God access (SUPER_ADMIN gets all permissions) ----

    @Test
    void loadUserByUsername_superAdmin_grantsAllPermissions() {
        // Given
        Role superAdmin = Role.builder().code("SUPER_ADMIN").permissions(new HashSet<>()).build();
        sampleUser.setRoles(Set.of(superAdmin));

        Permission p1 = Permission.builder().code("PERM_1").build();
        Permission p2 = Permission.builder().code("PERM_2").build();

        when(userRepository.findByUsernameWithRolesAndPermissions("john"))
                .thenReturn(Optional.of(sampleUser));
        when(permissionRepository.findAll()).thenReturn(List.of(p1, p2));

        // When
        UserDetails result = customUserDetailsService.loadUserByUsername("john");

        // Then
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PERM_1")));
        assertTrue(result.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("PERM_2")));
    }
}
