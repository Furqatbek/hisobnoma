package com.hisobnoma.platform.auth.mapper;

import com.hisobnoma.platform.auth.dto.RoleDto;
import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RoleMapperTest {

    private RoleMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(RoleMapper.class);
    }

    @Test
    void roleMapper_toDto_includesPermissionList() {
        // Given
        Permission p1 = Permission.builder().code("INVENTORY_READ").build();
        Permission p2 = Permission.builder().code("POS_READ").build();
        Permission p3 = Permission.builder().code("ADMIN_SETTINGS_VIEW").build();

        Role role = Role.builder()
                .name("Cashier")
                .code("CASHIER")
                .description("Cashier role")
                .systemRole(false)
                .permissions(new HashSet<>(Set.of(p1, p2, p3)))
                .users(new HashSet<>())
                .build();
        role.setId(5L);
        role.setTenantId(1L);

        // When
        RoleDto dto = mapper.toDto(role);

        // Then
        assertEquals(5L, dto.getId());
        assertEquals("Cashier", dto.getName());
        assertEquals("CASHIER", dto.getCode());
        assertEquals("Cashier role", dto.getDescription());
        assertFalse(dto.isSystemRole());
        assertEquals(1L, dto.getTenantId());
        assertEquals(3, dto.getPermissions().size());
        assertTrue(dto.getPermissions().contains("INVENTORY_READ"));
        assertTrue(dto.getPermissions().contains("POS_READ"));
        assertTrue(dto.getPermissions().contains("ADMIN_SETTINGS_VIEW"));
    }

    @Test
    void roleMapper_toDto_noPermissions_returnsEmptySet() {
        // Given
        Role role = Role.builder()
                .name("Empty")
                .code("EMPTY")
                .permissions(new HashSet<>())
                .users(new HashSet<>())
                .build();

        // When
        RoleDto dto = mapper.toDto(role);

        // Then
        assertNotNull(dto.getPermissions());
        assertTrue(dto.getPermissions().isEmpty());
    }

    @Test
    void roleMapper_toDto_systemRole_setsSystemRoleFlag() {
        // Given
        Role role = Role.builder()
                .name("Super Admin")
                .code("SUPER_ADMIN")
                .systemRole(true)
                .permissions(new HashSet<>())
                .users(new HashSet<>())
                .build();

        // When
        RoleDto dto = mapper.toDto(role);

        // Then
        assertTrue(dto.isSystemRole());
    }
}
