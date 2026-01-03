package com.hisobnoma.platform.auth.service;

import com.hisobnoma.platform.auth.dto.CreateRoleRequest;
import com.hisobnoma.platform.auth.dto.RoleDto;
import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.mapper.RoleMapper;
import com.hisobnoma.platform.auth.repository.PermissionRepository;
import com.hisobnoma.platform.auth.repository.RoleRepository;
import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RoleMapper roleMapper;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<RoleDto> getRoles(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<Role> roles = roleRepository.findAllByTenantIdOrSystem(tenantId, pageable);
        return PageResponse.of(roles.map(roleMapper::toDto));
    }

    @Transactional(readOnly = true)
    public RoleDto getRole(Long id) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new NotFoundException("Role", id));
        return roleMapper.toDto(role);
    }

    @Transactional(readOnly = true)
    public List<RoleDto> getSystemRoles() {
        return roleRepository.findAllSystemRoles().stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        if (roleRepository.existsByCodeAndTenantId(request.getCode(), tenantId)) {
            throw new DuplicateResourceException("Role", "code", request.getCode());
        }

        Role role = Role.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .tenantId(tenantId)
                .systemRole(false)
                .build();

        if (request.getPermissionCodes() != null && !request.getPermissionCodes().isEmpty()) {
            Set<Permission> permissions = permissionRepository.findByCodeIn(request.getPermissionCodes());
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        log.info("Created role: {}", role.getCode());

        return roleMapper.toDto(role);
    }

    @Transactional
    public RoleDto updateRole(Long id, CreateRoleRequest request) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new NotFoundException("Role", id));

        if (role.isSystemRole()) {
            throw new BusinessException("Cannot modify system roles", "SYSTEM_ROLE_PROTECTED");
        }

        role.setName(request.getName());
        role.setDescription(request.getDescription());

        if (request.getPermissionCodes() != null) {
            Set<Permission> permissions = permissionRepository.findByCodeIn(request.getPermissionCodes());
            role.setPermissions(permissions);
        }

        role = roleRepository.save(role);
        log.info("Updated role: {}", role.getCode());

        return roleMapper.toDto(role);
    }

    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Role", id));

        if (role.isSystemRole()) {
            throw new BusinessException("Cannot delete system roles", "SYSTEM_ROLE_PROTECTED");
        }

        if (!role.getUsers().isEmpty()) {
            throw new BusinessException("Cannot delete role with assigned users", "ROLE_IN_USE");
        }

        roleRepository.delete(role);
        log.info("Deleted role: {}", role.getCode());
    }

    @Transactional
    public RoleDto assignPermissions(Long id, Set<String> permissionCodes) {
        Role role = roleRepository.findByIdWithPermissions(id)
                .orElseThrow(() -> new NotFoundException("Role", id));

        if (role.isSystemRole()) {
            throw new BusinessException("Cannot modify system role permissions", "SYSTEM_ROLE_PROTECTED");
        }

        Set<Permission> permissions = permissionRepository.findByCodeIn(permissionCodes);
        role.setPermissions(permissions);

        role = roleRepository.save(role);
        log.info("Updated permissions for role: {}", role.getCode());

        return roleMapper.toDto(role);
    }

    @Transactional(readOnly = true)
    public List<String> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(Permission::getCode)
                .sorted()
                .collect(Collectors.toList());
    }
}
