package com.hisobnoma.platform.auth.repository;

import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@EnableJpaAuditing
class UserRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private UserRepository userRepository;

    private static final Long TENANT_ID_1 = 1L;
    private static final Long TENANT_ID_2 = 2L;

    private User persistUser(String username, String phone, Long tenantId, boolean enabled, Long telegramId) {
        User u = User.builder()
                .username(username)
                .phone(phone)
                .passwordHash("$2a$10$hash")
                .enabled(enabled)
                .telegramChatId(telegramId)
                .roles(new HashSet<>())
                .build();
        u.setTenantId(tenantId);
        return em.persistAndFlush(u);
    }

    private User persistUserWithRolesAndPermissions(String username, String phone, Long tenantId) {
        Permission perm = Permission.builder().code("INVENTORY_READ").build();
        em.persistAndFlush(perm);

        Role role = Role.builder()
                .name("Cashier").code("CASHIER_" + username).systemRole(false)
                .permissions(Set.of(perm)).users(new HashSet<>())
                .build();
        role.setTenantId(tenantId);
        em.persistAndFlush(role);

        User u = User.builder()
                .username(username)
                .phone(phone)
                .passwordHash("$2a$10$hash")
                .enabled(true)
                .roles(new HashSet<>(Set.of(role)))
                .build();
        u.setTenantId(tenantId);
        return em.persistAndFlush(u);
    }

    @Test
    void findByUsernameWithRolesAndPermissions_existingUser_loadsGraph() {
        // Given
        persistUserWithRolesAndPermissions("john_doe", "+998901111111", TENANT_ID_1);

        // When
        Optional<User> result = userRepository.findByUsernameWithRolesAndPermissions("john_doe");

        // Then
        assertTrue(result.isPresent());
        assertFalse(result.get().getRoles().isEmpty());
        result.get().getRoles().forEach(r -> assertFalse(r.getPermissions().isEmpty()));
    }

    @Test
    void findByPhoneWithRolesAndPermissions_existingPhone_loadsGraph() {
        // Given
        persistUserWithRolesAndPermissions("jane_doe", "+998902222222", TENANT_ID_1);

        // When
        Optional<User> result = userRepository.findByPhoneWithRolesAndPermissions("+998902222222");

        // Then
        assertTrue(result.isPresent());
        assertEquals("jane_doe", result.get().getUsername());
        assertFalse(result.get().getRoles().isEmpty());
    }

    @Test
    void findByIdWithRolesAndPermissions_existingId_loadsGraph() {
        // Given
        User persisted = persistUserWithRolesAndPermissions("alice", "+998903333333", TENANT_ID_1);

        // When
        Optional<User> result = userRepository.findByIdWithRolesAndPermissions(persisted.getId());

        // Then
        assertTrue(result.isPresent());
        assertFalse(result.get().getRoles().isEmpty());
    }

    @Test
    void findAllByTenantId_multiTenantDb_returnsOnlyTenantUsers() {
        // Given
        persistUser("tenant1_user1", "+1111111111", TENANT_ID_1, true, null);
        persistUser("tenant1_user2", "+1111111112", TENANT_ID_1, true, null);
        persistUser("tenant2_user1", "+2222222221", TENANT_ID_2, true, null);

        // When
        Page<User> result = userRepository.findAllByTenantId(TENANT_ID_1, PageRequest.of(0, 10));

        // Then
        assertEquals(2, result.getTotalElements());
        result.getContent().forEach(u -> assertEquals(TENANT_ID_1, u.getTenantId()));
    }

    @Test
    void searchByTenantId_partialName_returnsMatchingUsersOnly() {
        // Given
        persistUser("john_doe", "+3333333331", TENANT_ID_1, true, null);
        persistUser("jane_doe", "+3333333332", TENANT_ID_1, true, null);
        persistUser("john_smith", "+3333333333", TENANT_ID_1, true, null);

        // When
        Page<User> result = userRepository.searchByTenantId(TENANT_ID_1, "john", PageRequest.of(0, 10));

        // Then
        assertEquals(2, result.getTotalElements());
        result.getContent().forEach(u ->
                assertTrue(u.getUsername().toLowerCase().contains("john")));
    }

    @Test
    void countByTenantIdAndEnabledTrue_mixedEnabledStatus_returnsCorrectCount() {
        // Given — 3 enabled, 2 disabled
        persistUser("e1", "+4444444441", TENANT_ID_1, true, null);
        persistUser("e2", "+4444444442", TENANT_ID_1, true, null);
        persistUser("e3", "+4444444443", TENANT_ID_1, true, null);
        persistUser("d1", "+4444444444", TENANT_ID_1, false, null);
        persistUser("d2", "+4444444445", TENANT_ID_1, false, null);

        // When
        long count = userRepository.countByTenantIdAndEnabledTrue(TENANT_ID_1);

        // Then
        assertEquals(3L, count);
    }

    @Test
    void findUsersWithTelegramByTenantId_someNullChatId_returnsOnlyNonNull() {
        // Given
        persistUser("tg1", "+5555555551", TENANT_ID_1, true, 1001L);
        persistUser("tg2", "+5555555552", TENANT_ID_1, true, 1002L);
        persistUser("notg", "+5555555553", TENANT_ID_1, true, null);

        // When
        List<User> result = userRepository.findUsersWithTelegramByTenantId(TENANT_ID_1);

        // Then
        assertEquals(2, result.size());
        result.forEach(u -> assertNotNull(u.getTelegramChatId()));
    }
}
