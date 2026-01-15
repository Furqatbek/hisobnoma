package com.hisobnoma.platform.config;

import com.hisobnoma.platform.auth.entity.Permission;
import com.hisobnoma.platform.auth.entity.Permission.Action;
import com.hisobnoma.platform.auth.entity.Permission.Module;
import com.hisobnoma.platform.auth.entity.Role;
import com.hisobnoma.platform.auth.entity.User;
import com.hisobnoma.platform.auth.repository.PermissionRepository;
import com.hisobnoma.platform.auth.repository.RoleRepository;
import com.hisobnoma.platform.auth.repository.UserRepository;
import com.hisobnoma.platform.common.entity.Tenant;
import com.hisobnoma.platform.common.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Initializes default data for development and first-time setup.
 * Creates default tenant, roles, permissions, and admin user.
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already initialized, skipping data seeding");
            return;
        }

        log.info("Initializing default data...");

        // Create default tenant
        Tenant tenant = createDefaultTenant();

        // Create permissions
        Set<Permission> allPermissions = createPermissions();

        // Create roles
        Role adminRole = createRole(tenant, "ADMIN", "Administrator", allPermissions);
        Role managerRole = createRole(tenant, "MANAGER", "Manager", filterPermissions(allPermissions, "VIEW", "CREATE", "UPDATE"));
        Role cashierRole = createRole(tenant, "CASHIER", "Cashier", filterPermissions(allPermissions, "POS", "CUSTOMER"));

        // Create admin user
        createAdminUser(tenant, adminRole);

        log.info("Default data initialization completed!");
        log.info("===========================================");
        log.info("Default admin credentials:");
        log.info("  Username: admin");
        log.info("  Password: admin123");
        log.info("===========================================");
    }

    private Tenant createDefaultTenant() {
        Tenant tenant = tenantRepository.findByCode("DEFAULT").orElse(null);
        if (tenant == null) {
            tenant = Tenant.builder()
                    .code("DEFAULT")
                    .name("Default Company")
                    .active(true)
                    .build();
            tenant = tenantRepository.save(tenant);
            log.info("Created default tenant: {}", tenant.getName());
        }
        return tenant;
    }

    private Set<Permission> createPermissions() {
        Set<Permission> permissions = new HashSet<>();

        // Define permissions: code, name, module, action
        Object[][] permissionData = {
                // User permissions
                {"USER_VIEW", "View Users", Module.ADMIN, Action.VIEW},
                {"USER_CREATE", "Create Users", Module.ADMIN, Action.CREATE},
                {"USER_UPDATE", "Update Users", Module.ADMIN, Action.UPDATE},
                {"USER_DELETE", "Delete Users", Module.ADMIN, Action.DELETE},
                // Inventory permissions
                {"PRODUCT_VIEW", "View Products", Module.INVENTORY, Action.VIEW},
                {"PRODUCT_CREATE", "Create Products", Module.INVENTORY, Action.CREATE},
                {"PRODUCT_UPDATE", "Update Products", Module.INVENTORY, Action.UPDATE},
                {"PRODUCT_DELETE", "Delete Products", Module.INVENTORY, Action.DELETE},
                {"CATEGORY_MANAGE", "Manage Categories", Module.INVENTORY, Action.MANAGE},
                {"BRAND_MANAGE", "Manage Brands", Module.INVENTORY, Action.MANAGE},
                {"STOCK_VIEW", "View Stock", Module.INVENTORY, Action.VIEW},
                {"STOCK_MANAGE", "Manage Stock", Module.INVENTORY, Action.MANAGE},
                // POS permissions
                {"POS_ACCESS", "Access POS", Module.POS, Action.VIEW},
                {"POS_VOID", "Void Transactions", Module.POS, Action.DELETE},
                {"POS_DISCOUNT", "Apply Discounts", Module.POS, Action.UPDATE},
                // Customer permissions
                {"CUSTOMER_VIEW", "View Customers", Module.FINANCE, Action.VIEW},
                {"CUSTOMER_CREATE", "Create Customers", Module.FINANCE, Action.CREATE},
                {"CUSTOMER_UPDATE", "Update Customers", Module.FINANCE, Action.UPDATE},
                {"CUSTOMER_DELETE", "Delete Customers", Module.FINANCE, Action.DELETE},
                // Purchase permissions
                {"PURCHASE_VIEW", "View Purchases", Module.INVENTORY, Action.VIEW},
                {"PURCHASE_CREATE", "Create Purchases", Module.INVENTORY, Action.CREATE},
                {"PURCHASE_APPROVE", "Approve Purchases", Module.INVENTORY, Action.APPROVE},
                // Report permissions
                {"REPORT_SALES", "Sales Reports", Module.REPORTS, Action.VIEW},
                {"REPORT_INVENTORY", "Inventory Reports", Module.REPORTS, Action.VIEW},
                {"REPORT_FINANCIAL", "Financial Reports", Module.REPORTS, Action.VIEW},
                // Admin permissions
                {"SETTINGS_MANAGE", "Manage Settings", Module.ADMIN, Action.MANAGE},
                {"AUDIT_VIEW", "View Audit Logs", Module.ADMIN, Action.VIEW}
        };

        for (Object[] data : permissionData) {
            String code = (String) data[0];
            Permission permission = permissionRepository.findByCode(code).orElse(null);
            if (permission == null) {
                permission = Permission.builder()
                        .code(code)
                        .name((String) data[1])
                        .module((Module) data[2])
                        .action((Action) data[3])
                        .build();
                permission = permissionRepository.save(permission);
            }
            permissions.add(permission);
        }

        log.info("Created {} permissions", permissions.size());
        return permissions;
    }

    private Role createRole(Tenant tenant, String code, String name, Set<Permission> permissions) {
        Role role = roleRepository.findByCode(code).orElse(null);
        if (role == null) {
            role = Role.builder()
                    .code(code)
                    .name(name)
                    .tenantId(tenant.getId())
                    .permissions(new HashSet<>(permissions))
                    .build();
            role = roleRepository.save(role);
            log.info("Created role: {} with {} permissions", role.getName(), permissions.size());
        }
        return role;
    }

    private Set<Permission> filterPermissions(Set<Permission> allPermissions, String... patterns) {
        Set<Permission> filtered = new HashSet<>();
        for (Permission p : allPermissions) {
            for (String pattern : patterns) {
                if (p.getCode().contains(pattern)) {
                    filtered.add(p);
                    break;
                }
            }
        }
        return filtered;
    }

    private void createAdminUser(Tenant tenant, Role adminRole) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .firstName("System")
                    .lastName("Administrator")
                    .phone("+1234567890")
                    .enabled(true)
                    .phoneVerified(true)
                    .tenantId(tenant.getId())
                    .roles(Set.of(adminRole))
                    .build();
            userRepository.save(admin);
            log.info("Created admin user: admin");
        }
    }
}
