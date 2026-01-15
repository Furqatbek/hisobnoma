package com.hisobnoma.platform.auth.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * User entity for authentication and authorization.
 *
 * <h3>Design Decision: Phone-Based Authentication</h3>
 * <p>
 * This platform uses phone number as the primary identifier for user authentication
 * instead of email. This design decision was made for the following reasons:
 * </p>
 * <ul>
 *   <li>Higher adoption rates in target markets (Asia, Middle East, Africa) where
 *       phone-based auth is the standard</li>
 *   <li>Faster onboarding with OTP verification vs email confirmation</li>
 *   <li>Better suited for retail/POS environments where staff may not have
 *       corporate email addresses</li>
 *   <li>Simplified password recovery via SMS OTP</li>
 * </ul>
 * <p>
 * If email-based authentication is required in the future, add optional
 * {@code email} and {@code emailVerified} fields without removing phone support.
 * </p>
 *
 * @see com.hisobnoma.platform.auth.service.AuthService
 */
@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_phone", columnList = "phone"),
    @Index(name = "idx_users_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class User extends TenantAwareEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false)
    private String passwordHash;

    @Column(length = 50)
    private String firstName;

    @Column(length = 50)
    private String lastName;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = true;

    @Builder.Default
    @Column(nullable = false)
    private boolean locked = false;

    @Builder.Default
    @Column(nullable = false)
    private boolean phoneVerified = false;

    private Instant lastLoginAt;

    @Builder.Default
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    private Instant lockedUntil;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public String getFullName() {
        if (firstName == null && lastName == null) {
            return username;
        }
        return String.format("%s %s",
            firstName != null ? firstName : "",
            lastName != null ? lastName : "").trim();
    }

    public boolean isAccountNonLocked() {
        if (!locked) return true;
        if (lockedUntil == null) return false;
        return Instant.now().isAfter(lockedUntil);
    }

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
        this.lockedUntil = null;
    }

    public void addRole(Role role) {
        this.roles.add(role);
    }

    public void removeRole(Role role) {
        this.roles.remove(role);
    }

    public boolean hasRole(String roleCode) {
        return roles.stream().anyMatch(r -> r.getCode().equals(roleCode));
    }

    public boolean hasPermission(String permissionCode) {
        return roles.stream()
            .flatMap(r -> r.getPermissions().stream())
            .anyMatch(p -> p.getCode().equals(permissionCode));
    }
}
