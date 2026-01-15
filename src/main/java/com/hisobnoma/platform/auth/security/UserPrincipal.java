package com.hisobnoma.platform.auth.security;

import com.hisobnoma.platform.auth.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String username;
    private final String password;
    private final Long tenantId;
    private final boolean enabled;
    private final boolean accountNonLocked;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String username, String password, Long tenantId,
                         boolean enabled, boolean accountNonLocked,
                         Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.tenantId = tenantId;
        this.enabled = enabled;
        this.accountNonLocked = accountNonLocked;
        this.authorities = authorities;
    }

    public static UserPrincipal create(User user) {
        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> new SimpleGrantedAuthority(permission.getCode()))
                .collect(Collectors.toSet());

        // Add role authorities
        user.getRoles().forEach(role ->
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getCode())));

        // SUPER_ADMIN gets all permissions
        boolean isSuperAdmin = user.getRoles().stream()
                .anyMatch(role -> "SUPER_ADMIN".equals(role.getCode()));
        if (isSuperAdmin) {
            // Grant all inventory permissions
            authorities.add(new SimpleGrantedAuthority("INVENTORY_PRODUCT_READ"));
            authorities.add(new SimpleGrantedAuthority("INVENTORY_PRODUCT_CREATE"));
            authorities.add(new SimpleGrantedAuthority("INVENTORY_PRODUCT_UPDATE"));
            authorities.add(new SimpleGrantedAuthority("INVENTORY_PRODUCT_DELETE"));
            authorities.add(new SimpleGrantedAuthority("INVENTORY_STOCK_READ"));
            authorities.add(new SimpleGrantedAuthority("INVENTORY_STOCK_ADJUST"));
            authorities.add(new SimpleGrantedAuthority("INVENTORY_STOCK_TRANSFER"));
            // Grant all POS permissions
            authorities.add(new SimpleGrantedAuthority("POS_SALE_CREATE"));
            authorities.add(new SimpleGrantedAuthority("POS_SALE_VIEW"));
            authorities.add(new SimpleGrantedAuthority("POS_SALE_VOID"));
            // Grant all finance permissions
            authorities.add(new SimpleGrantedAuthority("FINANCE_CUSTOMER_READ"));
            authorities.add(new SimpleGrantedAuthority("FINANCE_CUSTOMER_CREATE"));
            authorities.add(new SimpleGrantedAuthority("FINANCE_CUSTOMER_UPDATE"));
            authorities.add(new SimpleGrantedAuthority("FINANCE_CUSTOMER_DELETE"));
            // Grant all purchasing permissions
            authorities.add(new SimpleGrantedAuthority("PURCHASING_VENDOR_READ"));
            authorities.add(new SimpleGrantedAuthority("PURCHASING_VENDOR_CREATE"));
            authorities.add(new SimpleGrantedAuthority("PURCHASING_PO_READ"));
            authorities.add(new SimpleGrantedAuthority("PURCHASING_PO_CREATE"));
            authorities.add(new SimpleGrantedAuthority("PURCHASING_PO_APPROVE"));
            // Grant all report permissions
            authorities.add(new SimpleGrantedAuthority("REPORT_VIEW"));
            authorities.add(new SimpleGrantedAuthority("REPORT_INVENTORY_VIEW"));
            authorities.add(new SimpleGrantedAuthority("REPORT_SALES_VIEW"));
            authorities.add(new SimpleGrantedAuthority("REPORT_FINANCIAL_VIEW"));
            authorities.add(new SimpleGrantedAuthority("REPORT_SCHEDULE_VIEW"));
            authorities.add(new SimpleGrantedAuthority("REPORT_SCHEDULE_MANAGE"));
            // Grant all admin permissions
            authorities.add(new SimpleGrantedAuthority("USER_VIEW"));
            authorities.add(new SimpleGrantedAuthority("USER_CREATE"));
            authorities.add(new SimpleGrantedAuthority("USER_UPDATE"));
            authorities.add(new SimpleGrantedAuthority("USER_DELETE"));
            authorities.add(new SimpleGrantedAuthority("AUDIT_LOG_VIEW"));
            authorities.add(new SimpleGrantedAuthority("SETTINGS_VIEW"));
            authorities.add(new SimpleGrantedAuthority("SETTINGS_MANAGE"));
        }

        return new UserPrincipal(
                user.getId(),
                user.getUsername(),
                user.getPasswordHash(),
                user.getTenantId(),
                user.isEnabled(),
                user.isAccountNonLocked(),
                authorities
        );
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    public boolean hasPermission(String permissionCode) {
        return authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals(permissionCode));
    }

    public boolean hasRole(String roleCode) {
        return authorities.stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_" + roleCode));
    }
}
