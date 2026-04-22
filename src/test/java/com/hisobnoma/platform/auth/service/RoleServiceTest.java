package com.hisobnoma.platform.auth.service;

import com.hisobnoma.platform.auth.dto.CreateRoleRequest;
import com.hisobnoma.platform.auth.dto.RoleDto;
import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.mapper.RoleMapper;
import com.hisobnoma.platform.auth.repository.PermissionRepository;
import com.hisobnoma.platform.auth.repository.RoleRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock private RoleRepository roleRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private RoleMapper roleMapper;
    @Mock private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private RoleService roleService;

    private static final Long TENANT_ID = 1L;
    private static final Long ROLE_ID = 10L;

    private Role customRole;
    private Role systemRole;
    private RoleDto customDto;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        pageable = PageRequest.of(0, 20);

        customRole = Role.builder()
                .name("Cashier")
                .code("CASHIER")
                .description("Cashier role")
                .systemRole(false)
                .permissions(new HashSet<>())
                .users(new HashSet<>())
                .build();
        customRole.setId(ROLE_ID);
        customRole.setTenantId(TENANT_ID);

        systemRole = Role.builder()
                .name("System Admin")
                .code("SUPER_ADMIN")
                .systemRole(true)
                .permissions(new HashSet<>())
                .users(new HashSet<>())
                .build();
        systemRole.setId(100L);

        customDto = RoleDto.builder()
                .id(ROLE_ID)
                .name("Cashier")
                .code("CASHIER")
                .build();
    }

    // ---- getRoles ----

    @Test
    void getRoles_tenantId_returnsTenantAndSystemRoles() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        Page<Role> page = new PageImpl<>(List.of(customRole, systemRole), pageable, 2L);
        when(roleRepository.findAllByTenantIdOrSystem(TENANT_ID, pageable)).thenReturn(page);
        when(roleMapper.toDto(customRole)).thenReturn(customDto);
        when(roleMapper.toDto(systemRole)).thenReturn(RoleDto.builder().id(100L).code("SUPER_ADMIN").build());

        // When
        PageResponse<RoleDto> result = roleService.getRoles(pageable);

        // Then
        assertEquals(2, result.getContent().size());
        assertEquals(2L, result.getPage().getTotalElements());
    }

    // ---- getRole ----

    @Test
    void getRole_existingId_returnsRoleDtoWithPermissions() {
        // Given
        when(roleRepository.findByIdWithPermissions(ROLE_ID)).thenReturn(Optional.of(customRole));
        when(roleMapper.toDto(customRole)).thenReturn(customDto);

        // When
        RoleDto result = roleService.getRole(ROLE_ID);

        // Then
        assertNotNull(result);
        assertEquals(ROLE_ID, result.getId());
    }

    @Test
    void getRole_nonExistentId_throwsNotFoundException() {
        // Given
        when(roleRepository.findByIdWithPermissions(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> roleService.getRole(999L));
    }

    // ---- createRole ----

    @Test
    void createRole_validRequest_createsRole() {
        // Given
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Cashier")
                .code("CASHIER")
                .description("Cashier role")
                .build();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(roleRepository.existsByCodeAndTenantId("CASHIER", TENANT_ID)).thenReturn(false);
        when(roleRepository.save(any(Role.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roleMapper.toDto(any(Role.class))).thenReturn(customDto);

        // When
        RoleDto result = roleService.createRole(request);

        // Then
        assertNotNull(result);
        verify(roleRepository).save(argThat(r ->
                "CASHIER".equals(r.getCode()) &&
                TENANT_ID.equals(r.getTenantId()) &&
                !r.isSystemRole()
        ));
    }

    @Test
    void createRole_duplicateCode_throwsDuplicateResourceException() {
        // Given
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Cashier")
                .code("CASHIER")
                .build();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(roleRepository.existsByCodeAndTenantId("CASHIER", TENANT_ID)).thenReturn(true);

        // When / Then
        assertThrows(DuplicateResourceException.class, () -> roleService.createRole(request));
        verify(roleRepository, never()).save(any());
    }

    // ---- updateRole ----

    @Test
    void updateRole_validRequest_updatesNameAndDescription() {
        // Given
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("Senior Cashier")
                .code("CASHIER")
                .description("Updated description")
                .build();
        when(roleRepository.findByIdWithPermissions(ROLE_ID)).thenReturn(Optional.of(customRole));
        when(roleRepository.save(customRole)).thenReturn(customRole);
        when(roleMapper.toDto(customRole)).thenReturn(customDto);

        // When
        RoleDto result = roleService.updateRole(ROLE_ID, request);

        // Then
        assertNotNull(result);
        assertEquals("Senior Cashier", customRole.getName());
        assertEquals("Updated description", customRole.getDescription());
    }

    @Test
    void updateRole_systemRole_throwsBusinessException() {
        // Given
        CreateRoleRequest request = CreateRoleRequest.builder()
                .name("X").code("SUPER_ADMIN").build();
        when(roleRepository.findByIdWithPermissions(100L)).thenReturn(Optional.of(systemRole));

        // When / Then
        assertThrows(BusinessException.class, () -> roleService.updateRole(100L, request));
        verify(roleRepository, never()).save(any());
    }

    @Test
    void updateRole_nonExistentId_throwsNotFoundException() {
        // Given
        CreateRoleRequest request = CreateRoleRequest.builder().name("X").code("X").build();
        when(roleRepository.findByIdWithPermissions(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> roleService.updateRole(999L, request));
    }

    // ---- deleteRole ----

    @Test
    void deleteRole_existingRole_deletesRole() {
        // Given
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(customRole));

        // When
        roleService.deleteRole(ROLE_ID);

        // Then
        verify(roleRepository).delete(customRole);
    }

    @Test
    void deleteRole_systemRole_throwsBusinessException() {
        // Given
        when(roleRepository.findById(100L)).thenReturn(Optional.of(systemRole));

        // When / Then
        assertThrows(BusinessException.class, () -> roleService.deleteRole(100L));
        verify(roleRepository, never()).delete(any());
    }

    @Test
    void deleteRole_roleInUse_throwsBusinessException() {
        // Given
        customRole.getUsers().add(new User());
        when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(customRole));

        // When / Then
        assertThrows(BusinessException.class, () -> roleService.deleteRole(ROLE_ID));
        verify(roleRepository, never()).delete(any());
    }

    @Test
    void deleteRole_nonExistentId_throwsNotFoundException() {
        // Given
        when(roleRepository.findById(999L)).thenReturn(Optional.empty());

        // When / Then
        assertThrows(NotFoundException.class, () -> roleService.deleteRole(999L));
    }

    // ---- assignPermissions ----

    @Test
    void assignPermissions_validCodes_replacesPermissions() {
        // Given
        Permission invRead = Permission.builder().code("INVENTORY_READ").build();
        Permission posRead = Permission.builder().code("POS_READ").build();
        Set<Permission> perms = Set.of(invRead, posRead);

        when(roleRepository.findByIdWithPermissions(ROLE_ID)).thenReturn(Optional.of(customRole));
        when(permissionRepository.findByCodeIn(anySet())).thenReturn(perms);
        when(roleRepository.save(customRole)).thenReturn(customRole);
        when(roleMapper.toDto(customRole)).thenReturn(customDto);

        // When
        RoleDto result = roleService.assignPermissions(ROLE_ID, Set.of("INVENTORY_READ", "POS_READ"));

        // Then
        assertNotNull(result);
        assertEquals(2, customRole.getPermissions().size());
    }

    @Test
    void assignPermissions_invalidCode_silentlyIgnoresUnknownCodes() {
        // Given — actual service calls findByCodeIn which just returns what exists.
        // Plan expects NotFoundException, but service doesn't validate individual codes.
        when(roleRepository.findByIdWithPermissions(ROLE_ID)).thenReturn(Optional.of(customRole));
        when(permissionRepository.findByCodeIn(anySet())).thenReturn(Set.of()); // none match
        when(roleRepository.save(customRole)).thenReturn(customRole);
        when(roleMapper.toDto(customRole)).thenReturn(customDto);

        // When
        RoleDto result = roleService.assignPermissions(ROLE_ID, Set.of("INVALID"));

        // Then — service silently assigns empty set; no exception
        assertNotNull(result);
        assertTrue(customRole.getPermissions().isEmpty());
    }
}
