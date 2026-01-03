package com.hisobnoma.platform.auth.controller;

import com.hisobnoma.platform.auth.dto.*;
import com.hisobnoma.platform.auth.security.RequiresPermission;
import com.hisobnoma.platform.auth.service.UserService;
import com.hisobnoma.platform.common.dto.ApiResponse;
import com.hisobnoma.platform.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User management APIs")
public class UserController {

    private final UserService userService;

    @GetMapping
    @RequiresPermission("ADMIN_USER_MANAGE")
    @Operation(summary = "List users with pagination and search")
    public ResponseEntity<ApiResponse<PageResponse<UserDto>>> getUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        PageResponse<UserDto> users = userService.getUsers(search, pageable);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/{id}")
    @RequiresPermission("ADMIN_USER_MANAGE")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
        UserDto user = userService.getUser(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping
    @RequiresPermission("ADMIN_USER_MANAGE")
    @Operation(summary = "Create a new user")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody CreateUserRequest request) {

        UserDto user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(user, "User created successfully"));
    }

    @PutMapping("/{id}")
    @RequiresPermission("ADMIN_USER_MANAGE")
    @Operation(summary = "Update user")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request) {

        UserDto user = userService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success(user, "User updated successfully"));
    }

    @DeleteMapping("/{id}")
    @RequiresPermission("ADMIN_USER_MANAGE")
    @Operation(summary = "Delete user")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully"));
    }

    @PutMapping("/{id}/roles")
    @RequiresPermission("ADMIN_USER_MANAGE")
    @Operation(summary = "Assign roles to user")
    public ResponseEntity<ApiResponse<UserDto>> assignRoles(
            @PathVariable Long id,
            @RequestBody Set<String> roleCodes) {

        UserDto user = userService.assignRoles(id, roleCodes);
        return ResponseEntity.ok(ApiResponse.success(user, "Roles updated successfully"));
    }

    @PutMapping("/{id}/lock")
    @RequiresPermission("ADMIN_USER_MANAGE")
    @Operation(summary = "Lock or unlock user")
    public ResponseEntity<ApiResponse<UserDto>> lockUser(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {

        boolean lock = request.getOrDefault("locked", false);
        UserDto user = userService.lockUser(id, lock);
        return ResponseEntity.ok(ApiResponse.success(user,
                lock ? "User locked successfully" : "User unlocked successfully"));
    }

    @PutMapping("/{id}/reset-password")
    @RequiresPermission("ADMIN_USER_MANAGE")
    @Operation(summary = "Reset user password (admin)")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @RequestBody Map<String, @NotBlank String> request) {

        String newPassword = request.get("password");
        userService.resetPassword(id, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully"));
    }
}
