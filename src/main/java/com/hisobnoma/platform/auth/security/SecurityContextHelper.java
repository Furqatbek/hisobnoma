package com.hisobnoma.platform.auth.security;

import com.hisobnoma.platform.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SecurityContextHelper {

    public Optional<UserPrincipal> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal) {
            return Optional.of((UserPrincipal) principal);
        }

        return Optional.empty();
    }

    public UserPrincipal getRequiredCurrentUser() {
        return getCurrentUser()
                .orElseThrow(() -> new UnauthorizedException("User not authenticated"));
    }

    public Long getCurrentUserId() {
        return getRequiredCurrentUser().getId();
    }

    public String getCurrentUsername() {
        return getRequiredCurrentUser().getUsername();
    }

    public Long getCurrentTenantId() {
        return getRequiredCurrentUser().getTenantId();
    }

    /**
     * Gets the current tenant ID, throwing UnauthorizedException if not authenticated.
     */
    public Long getRequiredTenantId() {
        Long tenantId = getCurrentTenantId();
        if (tenantId == null) {
            throw new UnauthorizedException("Tenant context not available");
        }
        return tenantId;
    }

    public boolean isAuthenticated() {
        return getCurrentUser().isPresent();
    }

    public boolean hasPermission(String permissionCode) {
        return getCurrentUser()
                .map(user -> user.hasPermission(permissionCode))
                .orElse(false);
    }

    public boolean hasRole(String roleCode) {
        return getCurrentUser()
                .map(user -> user.hasRole(roleCode))
                .orElse(false);
    }

    public boolean hasAnyPermission(String... permissionCodes) {
        return getCurrentUser()
                .map(user -> {
                    for (String code : permissionCodes) {
                        if (user.hasPermission(code)) {
                            return true;
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    public boolean hasAllPermissions(String... permissionCodes) {
        return getCurrentUser()
                .map(user -> {
                    for (String code : permissionCodes) {
                        if (!user.hasPermission(code)) {
                            return false;
                        }
                    }
                    return true;
                })
                .orElse(false);
    }
}
