package com.hisobnoma.platform.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleDto {

    private Long id;
    private String name;
    private String code;
    private String description;
    private boolean systemRole;
    private Long tenantId;
    private Set<String> permissions;
    private Instant createdAt;
    private Instant updatedAt;
}
