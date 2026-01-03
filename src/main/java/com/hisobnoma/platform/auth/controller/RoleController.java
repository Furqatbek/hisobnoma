package com.hisobnoma.platform.auth.controller;

import com.hisobnoma.platform.auth.dto.CreateRoleRequest;
import com.hisobnoma.platform.auth.dto.RoleDto;
import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.auth.service.RoleService;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@Tag(name = "Role Management", description = "Role management APIs")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @RequiresPermission("ADMIN_ROLE_MANAGE")
    @Operation(summary = "List roles with pagination")
    public ResponseEntity<ApiResponse<PageResponse<RoleDto>>> getRoles(
            @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {

        PageResponse<RoleDto> roles = roleService.getRoles(pageable);
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/system")
    @RequiresPermission("ADMIN_ROLE_MANAGE")
    @Operation(summary = "List system roles")
    public ResponseEntity<ApiResponse<List<RoleDto>>> getSystemRoles() {
        List<RoleDto> roles = roleService.getSystemRoles();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }

    @GetMapping("/{id}")
    @RequiresPermission("ADMIN_ROLE_MANAGE")
    @Operation(summary = "Get role by ID")
    public ResponseEntity<ApiResponse<RoleDto>> getRole(@PathVariable Long id) {
        RoleDto role = roleService.getRole(id);
        return ResponseEntity.ok(ApiResponse.success(role));
    }

    @PostMapping
    @RequiresPermission("ADMIN_ROLE_MANAGE")
    @Operation(summary = "Create a new role")
    public ResponseEntity<ApiResponse<RoleDto>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {

        RoleDto role = roleService.createRole(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(role, "Role created successfully"));
    }

    @PutMapping("/{id}")
    @RequiresPermission("ADMIN_ROLE_MANAGE")
    @Operation(summary = "Update role")
    public ResponseEntity<ApiResponse<RoleDto>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody CreateRoleRequest request) {

        RoleDto role = roleService.updateRole(id, request);
        return ResponseEntity.ok(ApiResponse.success(role, "Role updated successfully"));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("ADMIN_ROLE_MANAGE")
    @Operation(summary = "Delete role")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.success("Role deleted successfully"));
    }

    @PutMapping("/{id}/permissions")
    @RequiresPermission("ADMIN_PERMISSION_MANAGE")
    @Operation(summary = "Assign permissions to role")
    public ResponseEntity<ApiResponse<RoleDto>> assignPermissions(
            @PathVariable Long id,
            @RequestBody Set<String> permissionCodes) {

        RoleDto role = roleService.assignPermissions(id, permissionCodes);
        return ResponseEntity.ok(ApiResponse.success(role, "Permissions updated successfully"));
    }

    @GetMapping("/permissions")
    @RequiresPermission("ADMIN_ROLE_MANAGE")
    @Operation(summary = "List all available permissions")
    public ResponseEntity<ApiResponse<List<String>>> getAllPermissions() {
        List<String> permissions = roleService.getAllPermissions();
        return ResponseEntity.ok(ApiResponse.success(permissions));
    }
}
