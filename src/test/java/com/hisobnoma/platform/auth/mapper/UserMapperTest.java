package com.hisobnoma.platform.auth.mapper;

import com.hisobnoma.platform.auth.dto.UserDto;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private UserMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = Mappers.getMapper(UserMapper.class);
    }

    @Test
    void userMapper_toDto_mapsAllFieldsExcludesPassword() {
        // Given
        Role cashier = Role.builder().code("CASHIER").name("Cashier").build();
        Role manager = Role.builder().code("MANAGER").name("Manager").build();

        User user = User.builder()
                .username("john")
                .phone("+998901234567")
                .passwordHash("$2a$10$secretHash")
                .pinHash("$2a$10$pinHash")
                .firstName("John")
                .lastName("Doe")
                .enabled(true)
                .locked(false)
                .phoneVerified(true)
                .lastLoginAt(Instant.now())
                .roles(new HashSet<>(Set.of(cashier, manager)))
                .build();
        user.setId(10L);
        user.setTenantId(1L);

        // When
        UserDto dto = mapper.toDto(user);

        // Then
        assertEquals(10L, dto.getId());
        assertEquals("john", dto.getUsername());
        assertEquals("+998901234567", dto.getPhone());
        assertEquals("John", dto.getFirstName());
        assertEquals("Doe", dto.getLastName());
        assertEquals("John Doe", dto.getFullName());
        assertTrue(dto.isEnabled());
        assertFalse(dto.isLocked());
        assertTrue(dto.isPhoneVerified());
        assertTrue(dto.isHasPin());
        assertEquals(1L, dto.getTenantId());
        assertEquals(2, dto.getRoles().size());
        assertTrue(dto.getRoles().contains("CASHIER"));
        assertTrue(dto.getRoles().contains("MANAGER"));
    }

    @Test
    void userMapper_toDto_withoutPin_hasPinIsFalse() {
        // Given
        User user = User.builder()
                .username("nopin")
                .passwordHash("$2a$10$hash")
                .pinHash(null)
                .roles(new HashSet<>())
                .build();

        // When
        UserDto dto = mapper.toDto(user);

        // Then
        assertFalse(dto.isHasPin());
    }

    @Test
    void userMapper_toDto_noRoles_returnsEmptyRoleSet() {
        // Given
        User user = User.builder()
                .username("noroles")
                .passwordHash("$2a$10$hash")
                .roles(new HashSet<>())
                .build();

        // When
        UserDto dto = mapper.toDto(user);

        // Then
        assertNotNull(dto.getRoles());
        assertTrue(dto.getRoles().isEmpty());
    }

    @Test
    void userMapper_toDto_fullName_derivedFromFirstAndLast() {
        // Given
        User user = User.builder()
                .username("fulluser")
                .passwordHash("$2a$10$hash")
                .firstName("Jane")
                .lastName("Smith")
                .roles(new HashSet<>())
                .build();

        // When
        UserDto dto = mapper.toDto(user);

        // Then
        assertEquals("Jane Smith", dto.getFullName());
    }
}
