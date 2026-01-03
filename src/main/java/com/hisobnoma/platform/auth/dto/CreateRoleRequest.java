package com.hisobnoma.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoleRequest {

    @NotBlank(message = "Role name is required")
    @Size(max = 50, message = "Role name cannot exceed 50 characters")
    private String name;

    @NotBlank(message = "Role code is required")
    @Size(max = 50, message = "Role code cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "Role code must be uppercase with underscores only")
    private String code;

    @Size(max = 255, message = "Description cannot exceed 255 characters")
    private String description;

    private Set<String> permissionCodes;
}
