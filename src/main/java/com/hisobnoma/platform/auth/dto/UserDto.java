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
public class UserDto {

    private Long id;
    private String username;
    private String phone;
    private String firstName;
    private String lastName;
    private String fullName;
    private boolean enabled;
    private boolean locked;
    private boolean hasPin;
    private boolean phoneVerified;
    private Instant lastLoginAt;
    private Long tenantId;
    private Set<String> roles;
    private Instant createdAt;
    private Instant updatedAt;
}
