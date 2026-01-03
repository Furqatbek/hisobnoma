package com.hisobnoma.platform.auth.service;

import com.hisobnoma.platform.auth.dto.*;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<UserDto> getUsers(String search, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Page<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userRepository.searchByTenantId(tenantId, search.trim(), pageable);
        } else {
            users = userRepository.findAllByTenantId(tenantId, pageable);
        }

        return PageResponse.of(users.map(userMapper::toDto));
    }

    @Transactional(readOnly = true)
    public UserDto getUser(Long id) {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto createUser(CreateUserRequest request) {
        // Check for duplicates
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("User", "username", request.getUsername());
        }

        if (request.getPhone() != null && userRepository.existsByPhone(request.getPhone())) {
            throw new DuplicateResourceException("User", "phone", request.getPhone());
        }

        Long tenantId = securityContextHelper.getCurrentTenantId();

        User user = User.builder()
                .username(request.getUsername())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .enabled(true)
                .tenantId(tenantId)
                .build();

        // Assign roles
        if (request.getRoleCodes() != null && !request.getRoleCodes().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            for (String roleCode : request.getRoleCodes()) {
                Role role = roleRepository.findByCodeAndTenantId(roleCode, tenantId)
                        .or(() -> roleRepository.findByCode(roleCode)) // Try system role
                        .orElseThrow(() -> new NotFoundException("Role", "code", roleCode));
                roles.add(role);
            }
            user.setRoles(roles);
        }

        user = userRepository.save(user);
        log.info("Created user: {}", user.getUsername());

        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto updateUser(Long id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        // Check phone uniqueness if changing
        if (request.getPhone() != null && !request.getPhone().equals(user.getPhone())) {
            if (userRepository.existsByPhone(request.getPhone())) {
                throw new DuplicateResourceException("User", "phone", request.getPhone());
            }
            user.setPhone(request.getPhone());
        }

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }

        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }

        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }

        user = userRepository.save(user);
        log.info("Updated user: {}", user.getUsername());

        return userMapper.toDto(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        // Prevent self-deletion
        if (user.getId().equals(securityContextHelper.getCurrentUserId())) {
            throw new ValidationException("Cannot delete your own account");
        }

        userRepository.delete(user);
        log.info("Deleted user: {}", user.getUsername());
    }

    @Transactional
    public UserDto assignRoles(Long id, Set<String> roleCodes) {
        User user = userRepository.findByIdWithRolesAndPermissions(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        Long tenantId = securityContextHelper.getCurrentTenantId();

        Set<Role> roles = new HashSet<>();
        for (String roleCode : roleCodes) {
            Role role = roleRepository.findByCodeAndTenantId(roleCode, tenantId)
                    .or(() -> roleRepository.findByCode(roleCode))
                    .orElseThrow(() -> new NotFoundException("Role", "code", roleCode));
            roles.add(role);
        }

        user.setRoles(roles);
        user = userRepository.save(user);

        // Invalidate refresh tokens to force re-authentication
        refreshTokenRepository.revokeAllByUser(user);

        log.info("Updated roles for user: {}", user.getUsername());
        return userMapper.toDto(user);
    }

    @Transactional
    public UserDto lockUser(Long id, boolean lock) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        // Prevent self-locking
        if (user.getId().equals(securityContextHelper.getCurrentUserId())) {
            throw new ValidationException("Cannot lock your own account");
        }

        user.setLocked(lock);
        if (!lock) {
            user.setLockedUntil(null);
            user.resetFailedLoginAttempts();
        }

        user = userRepository.save(user);

        if (lock) {
            refreshTokenRepository.revokeAllByUser(user);
        }

        log.info("User {} {} by admin", user.getUsername(), lock ? "locked" : "unlocked");
        return userMapper.toDto(user);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User", id));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Invalidate all sessions
        refreshTokenRepository.revokeAllByUser(user);

        log.info("Password reset for user: {} by admin", user.getUsername());
    }
}
