package com.hisobnoma.platform.auth.repository;

import com.hisobnoma.platform.auth.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class RoleRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private RoleRepository roleRepository;

    private Role persistRole(String code, String name, Long tenantId, boolean systemRole) {
        Role r = Role.builder()
                .code(code).name(name).systemRole(systemRole)
                .permissions(new HashSet<>()).users(new HashSet<>())
                .build();
        r.setTenantId(tenantId);
        return em.persistAndFlush(r);
    }

    @Test
    void findSystemRoleByCode_systemRole_returnsRole() {
        // Given
        persistRole("ADMIN", "Administrator", null, true);

        // When
        Optional<Role> result = roleRepository.findSystemRoleByCode("ADMIN");

        // Then
        assertTrue(result.isPresent());
        assertEquals("ADMIN", result.get().getCode());
    }

    @Test
    void findSystemRoleByCode_nonExistentCode_returnsEmpty() {
        // When
        Optional<Role> result = roleRepository.findSystemRoleByCode("NONEXISTENT");

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void findSystemRoleByCode_neverReturnsAnotherTenantsCustomRole() {
        // A tenant-owned role with the same code must NOT satisfy the system fallback
        persistRole("MANAGER2", "Tenant 2 Manager", 2L, false);

        assertTrue(roleRepository.findSystemRoleByCode("MANAGER2").isEmpty(),
                "Tenant-owned role must not be returned by the system-role lookup");
    }

    @Test
    void findByCodeAndTenantId_tenantScopedLookup_returnsCorrectRole() {
        // Given — same code in two tenants
        Role tenant1Role = persistRole("MANAGER", "T1 Manager", 1L, false);
        persistRole("MANAGER", "T2 Manager", 2L, false);

        // When
        Optional<Role> result = roleRepository.findByCodeAndTenantId("MANAGER", 1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(tenant1Role.getId(), result.get().getId());
        assertEquals(1L, result.get().getTenantId());
    }

    @Test
    void findAllSystemRoles_mixedRoles_returnsOnlySystemRoles() {
        // Given
        persistRole("SUPER_ADMIN", "Super Admin", null, true);
        persistRole("ADMIN", "Admin", null, true);
        persistRole("CASHIER", "Cashier", 1L, false);

        // When
        List<Role> result = roleRepository.findAllSystemRoles();

        // Then
        assertEquals(2, result.size());
        result.forEach(r -> assertTrue(r.isSystemRole()));
        result.forEach(r -> assertNull(r.getTenantId()));
    }
}
