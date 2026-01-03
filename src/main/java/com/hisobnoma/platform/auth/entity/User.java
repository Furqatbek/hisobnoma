package com.hisobnoma.platform.auth.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username"),
    @Index(name = "idx_users_phone", columnList = "phone"),
    @Index(name = "idx_users_tenant", columnList = "tenant_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
