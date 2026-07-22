package com.hisobnoma.platform.auth.service;

import com.hisobnoma.platform.auth.dto.CreateUserRequest;
import com.hisobnoma.platform.auth.dto.UpdateUserRequest;
import com.hisobnoma.platform.auth.dto.UserDto;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.mapper.UserMapper;
import com.hisobnoma.platform.auth.repository.RefreshTokenRepository;
import com.hisobnoma.platform.auth.repository.RoleRepository;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private UserService userService;

    private static final Long TENANT_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long CURRENT_USER_ID = 99L;

    private User sampleUser;
    private UserDto sampleDto;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        lenient().when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        pageable = PageRequest.of(0, 20);

        sampleUser = User.builder()
                .username("john")
                .phone("+998901234567")
                .passwordHash("$2a$10$hash")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .build();
        sampleUser.setId(USER_ID);
        sampleUser.setTenantId(TENANT_ID);

        sampleDto = UserDto.builder()
                .id(USER_ID)
                .username("john")
                .firstName("John")
                .lastName("Doe")
                .build();
    }

    // ---- getUsers ----

    @Test
    void getUsers_noSearch_returnsAllTenantUsersPaged() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<User> page = new PageImpl<>(List.of(sampleUser), pageable, 1L);
        when(userRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(userMapper.toDto(sampleUser)).thenReturn(sampleDto);

        // When
        PageResponse<UserDto> result = userService.getUsers(null, pageable);

        // Then
        assertEquals(1, result.getContent().size());
        assertEquals(1L, result.getPage().getTotalElements());
    }

    @Test
    void getUsers_withSearch_returnsFilteredUsers() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<User> page = new PageImpl<>(List.of(sampleUser), pageable, 1L);
        when(userRepository.searchByTenantId(TENANT_ID, "john", pageable)).thenReturn(page);
        when(userMapper.toDto(sampleUser)).thenReturn(sampleDto);

        // When
        PageResponse<UserDto> result = userService.getUsers("john", pageable);

        // Then
        assertEquals(1, result.getContent().size());
        verify(userRepository).searchByTenantId(TENANT_ID, "john", pageable);
    }

    // ---- getUser ----

    @Test
    void getUser_existingId_returnsUserDto() {
        // Given
        when(userRepository.findByIdAndTenantIdWithRolesAndPermissions(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));
        when(userMapper.toDto(sampleUser)).thenReturn(sampleDto);

        // When
        UserDto result = userService.getUser(USER_ID);

        // Then
        assertNotNull(result);
        assertEquals(USER_ID, result.getId());
    }

    @Test
    void getUser_nonExistentId_throwsNotFoundException() {
        // Given
        when(userRepository.findByIdAndTenantIdWithRolesAndPermissions(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> userService.getUser(999L));
    }

    // ---- createUser ----

    @Test
    void createUser_validRequest_createsUserWithHashedPassword() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .password("plainPass123")
                .phone("+998907777777")
                .firstName("New")
                .lastName("User")
                .build();
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByPhone("+998907777777")).thenReturn(false);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(passwordEncoder.encode("plainPass123")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(USER_ID);
            return u;
        });
        when(userMapper.toDto(any(User.class))).thenReturn(sampleDto);

        // When
        UserDto result = userService.createUser(request);

        // Then
        assertNotNull(result);
        verify(passwordEncoder).encode("plainPass123");
        verify(userRepository).save(argThat(u ->
                "$2a$10$hashed".equals(u.getPasswordHash()) &&
                !"plainPass123".equals(u.getPasswordHash())
        ));
    }

    @Test
    void createUser_duplicateUsername_throwsDuplicateResourceException() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .username("john")
                .password("plainPass123")
                .build();
        when(userRepository.existsByUsername("john")).thenReturn(true);

        // When / Then
        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void createUser_duplicatePhone_throwsDuplicateResourceException() {
        // Given
        CreateUserRequest request = CreateUserRequest.builder()
                .username("newuser")
                .phone("+998901234567")
                .password("plainPass123")
                .build();
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByPhone("+998901234567")).thenReturn(true);

        // When / Then
        assertThrows(DuplicateResourceException.class, () -> userService.createUser(request));
        verify(userRepository, never()).save(any());
    }

    // ---- updateUser ----

    @Test
    void updateUser_validRequest_updatesFieldsAndReturnsDto() {
        // Given
        UpdateUserRequest request = UpdateUserRequest.builder()
                .firstName("Jane")
                .lastName("Smith")
                .enabled(true)
                .build();
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toDto(sampleUser)).thenReturn(sampleDto);

        // When
        UserDto result = userService.updateUser(USER_ID, request);

        // Then
        assertNotNull(result);
        assertEquals("Jane", sampleUser.getFirstName());
        assertEquals("Smith", sampleUser.getLastName());
    }

    @Test
    void updateUser_nonExistentId_throwsNotFoundException() {
        // Given
        UpdateUserRequest request = UpdateUserRequest.builder().firstName("X").build();
        when(userRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> userService.updateUser(999L, request));
    }

    @Test
    void updateUser_duplicatePhone_throwsDuplicateResourceException() {
        // Given — plan's "duplicate username" isn't applicable (UpdateUserRequest has no username)
        // Testing phone duplicate instead, which is the equivalent uniqueness check
        UpdateUserRequest request = UpdateUserRequest.builder()
                .phone("+998909999999")
                .build();
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));
        when(userRepository.existsByPhone("+998909999999")).thenReturn(true);

        // When / Then
        assertThrows(DuplicateResourceException.class, () -> userService.updateUser(USER_ID, request));
    }

    // ---- deleteUser ----

    @Test
    void deleteUser_existingId_deletesUser() {
        // Given
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));
        when(securityContextHelper.getCurrentUserId()).thenReturn(CURRENT_USER_ID);

        // When
        userService.deleteUser(USER_ID);

        // Then
        verify(userRepository).delete(sampleUser);
    }

    @Test
    void deleteUser_nonExistentId_throwsNotFoundException() {
        // Given
        when(userRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> userService.deleteUser(999L));
    }

    @Test
    void deleteUser_currentUserId_throwsValidationException() {
        // Given — plan says BusinessException, actual impl throws ValidationException
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));
        when(securityContextHelper.getCurrentUserId()).thenReturn(USER_ID);

        // When / Then
        assertThrows(ValidationException.class, () -> userService.deleteUser(USER_ID));
        verify(userRepository, never()).delete(any());
    }

    // ---- assignRoles ----

    @Test
    void assignRoles_validRoleCodes_replacesRolesOnUser() {
        // Given
        Role cashier = Role.builder().code("CASHIER").build();
        Role manager = Role.builder().code("MANAGER").build();
        when(userRepository.findByIdAndTenantIdWithRolesAndPermissions(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(roleRepository.findByCodeAndTenantId("CASHIER", TENANT_ID)).thenReturn(Optional.of(cashier));
        when(roleRepository.findByCodeAndTenantId("MANAGER", TENANT_ID)).thenReturn(Optional.of(manager));
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toDto(sampleUser)).thenReturn(sampleDto);

        // When
        UserDto result = userService.assignRoles(USER_ID, Set.of("CASHIER", "MANAGER"));

        // Then
        assertNotNull(result);
        assertEquals(2, sampleUser.getRoles().size());
        verify(refreshTokenRepository).revokeAllByUser(sampleUser);
    }

    @Test
    void assignRoles_invalidRoleCode_throwsNotFoundException() {
        // Given
        when(userRepository.findByIdAndTenantIdWithRolesAndPermissions(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(roleRepository.findByCodeAndTenantId("INVALID_CODE", TENANT_ID)).thenReturn(Optional.empty());
        when(roleRepository.findSystemRoleByCode("INVALID_CODE")).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () ->
                userService.assignRoles(USER_ID, Set.of("INVALID_CODE")));
    }

    // ---- lockUser ----

    @Test
    void lockUser_lockTrue_disablesUser() {
        // Given
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));
        when(securityContextHelper.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toDto(sampleUser)).thenReturn(sampleDto);

        // When
        userService.lockUser(USER_ID, true);

        // Then
        assertTrue(sampleUser.isLocked());
        verify(refreshTokenRepository).revokeAllByUser(sampleUser);
    }

    @Test
    void lockUser_lockFalse_unlocksUser() {
        // Given
        sampleUser.setLocked(true);
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));
        when(securityContextHelper.getCurrentUserId()).thenReturn(CURRENT_USER_ID);
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(userMapper.toDto(sampleUser)).thenReturn(sampleDto);

        // When
        userService.lockUser(USER_ID, false);

        // Then
        assertFalse(sampleUser.isLocked());
        assertNull(sampleUser.getLockedUntil());
    }

    @Test
    void lockUser_nonExistentId_throwsNotFoundException() {
        // Given
        when(userRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> userService.lockUser(999L, true));
    }

    // ---- resetPassword ----

    @Test
    void resetPassword_existingUser_hashesAndSavesPassword() {
        // Given
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("newAdminPass")).thenReturn("$2a$10$adminhash");
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);

        // When
        userService.resetPassword(USER_ID, "newAdminPass");

        // Then
        assertEquals("$2a$10$adminhash", sampleUser.getPasswordHash());
        verify(refreshTokenRepository).revokeAllByUser(sampleUser);
    }

    // ---- setUserPin ----

    @Test
    void setUserPin_validPin_hashesAndSavesPin() {
        // Given
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("1234")).thenReturn("$2a$10$pinhash");
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);

        // When
        userService.setUserPin(USER_ID, "1234");

        // Then
        assertEquals("$2a$10$pinhash", sampleUser.getPinHash());
    }

    @Test
    void setUserPin_tooShortPin_throwsValidationException() {
        // Given
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));

        // When / Then
        assertThrows(ValidationException.class, () -> userService.setUserPin(USER_ID, "12"));
    }

    @Test
    void setUserPin_nonNumericPin_throwsValidationException() {
        // Given
        when(userRepository.findByIdAndTenantId(USER_ID, TENANT_ID)).thenReturn(Optional.of(sampleUser));

        // When / Then
        assertThrows(ValidationException.class, () -> userService.setUserPin(USER_ID, "abcd"));
    }
}
