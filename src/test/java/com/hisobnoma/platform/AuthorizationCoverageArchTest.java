package com.hisobnoma.platform;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards authorization coverage: the {@code @RequiresPermission} aspect
 * silently proceeds when the annotation is absent, and {@code @PreAuthorize}
 * only applies where written — so a handler method without either is
 * protected by nothing but "is authenticated". Every request-mapped method
 * must carry one of the two (method or class level) or live in a controller
 * that is deliberately public/authenticated-only and allowlisted below.
 */
class AuthorizationCoverageArchTest {

    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
            "org.springframework.web.bind.annotation.RequestMapping",
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.PatchMapping",
            "org.springframework.web.bind.annotation.DeleteMapping");

    private static final String REQUIRES_PERMISSION = "com.hisobnoma.platform.auth.security.RequiresPermission";
    private static final String PRE_AUTHORIZE = "org.springframework.security.access.prepost.PreAuthorize";

    /**
     * Controllers that are intentionally NOT permission-guarded. Public
     * controllers sit on the /api/v1/web/** (or auth) whitelist; the
     * remaining entries are authenticated-only by design — each needs a
     * justification before being added.
     */
    private static final Set<String> ALLOWLIST = Set.of(
            // Anonymous by design — the public shop API on the /api/v1/web/**
            // whitelist; /me endpoints validate the web-customer token themselves
            "WebCatalogPublicController",
            "WebAuthPublicController",
            "WebOrderPublicController",
            "WebCartPublicController",
            // Login/refresh are anonymous; the rest operate only on the
            // caller's own account (id from their token)
            "AuthController",
            "MobileAuthController",
            // Self-service: links/unlinks the caller's own Telegram account
            "TelegramController",
            // Authenticated-only via an explicit SecurityConfig matcher
            // (more specific than the /web/** whitelist); no permission
            // model for expenses yet
            "ExpenseRecordController"
    );

    private static JavaClasses mainClasses;

    @BeforeAll
    static void importClasses() {
        mainClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.hisobnoma.platform");
    }

    @Test
    void everyHandlerMethod_isPermissionGuarded_orAllowlisted() {
        Set<String> violations = new TreeSet<>();

        for (JavaClass javaClass : mainClasses) {
            boolean isController = javaClass.isAnnotatedWith(
                    "org.springframework.web.bind.annotation.RestController");
            if (!isController || ALLOWLIST.contains(javaClass.getSimpleName())) {
                continue;
            }
            boolean classGuarded = javaClass.isAnnotatedWith(REQUIRES_PERMISSION)
                    || javaClass.isAnnotatedWith(PRE_AUTHORIZE);
            if (classGuarded) {
                continue;
            }
            for (JavaMethod method : javaClass.getMethods()) {
                boolean isHandler = method.getAnnotations().stream()
                        .anyMatch(a -> MAPPING_ANNOTATIONS.contains(a.getRawType().getName()));
                if (!isHandler) {
                    continue;
                }
                boolean guarded = method.isAnnotatedWith(REQUIRES_PERMISSION)
                        || method.isAnnotatedWith(PRE_AUTHORIZE);
                if (!guarded) {
                    violations.add(javaClass.getSimpleName() + "." + method.getName());
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Handler methods without @RequiresPermission/@PreAuthorize (add a permission "
                        + "check, or allowlist the controller with a justification):\n  "
                        + String.join("\n  ", violations));
    }
}
