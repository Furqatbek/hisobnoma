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
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private long expiresIn;
    private boolean rememberMe;
    private UserInfo user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String phone;
        private String firstName;
        private String lastName;
        private String fullName;
        private Long tenantId;
        private String tenantName;
        private String tenantCode;
        private Instant subscriptionExpiresAt;
        private int maxUsers;
        private int maxLocations;
        private boolean tenantActive;
        private boolean phoneVerified;
        private boolean enabled;
        private Instant lastLoginAt;
        private Instant createdAt;
        private Set<String> roles;
        private Set<String> permissions;
    }
}
