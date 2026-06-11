package com.hisobnoma.platform;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.ResolvableType;
import org.springframework.data.repository.Repository;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards tenant isolation: loading a tenant-scoped entity by raw
 * {@code findById} skips the tenant filter, so a request-supplied id can
 * reach another tenant's row (the bug class fixed in ProductService,
 * JournalEntryService, ARPaymentService et al.).
 *
 * Every call to findById/getById/getReferenceById on a repository whose
 * entity extends {@link TenantAwareEntity} must either be replaced with
 * findByIdAndTenantId or be explicitly allowlisted below with a reason
 * (e.g. the result is validated against a tenant-scoped parent).
 */
class TenantIsolationArchTest {

    private static final Set<String> ID_LOOKUP_METHODS = Set.of("findById", "getById", "getReferenceById");

    /**
     * Call sites allowed to use raw id lookups on tenant-scoped
     * repositories. Format: "SimpleClassName.methodName". Add entries only
     * with a justification comment — new unscoped lookups are bugs.
     */
    private static final Set<String> ALLOWLIST = Set.of(
            // Self-lookups: the id is the caller's own user id taken from the JWT
            "AuthService.changePassword",
            "AuthService.logout",
            "AuthService.setPin",
            "ShiftService.openShift",
            "ShiftService.cashOperation",
            "TelegramController.getStatus",
            "TelegramController.unlink",
            // Tenant validated explicitly after the fetch
            "ExpenseRecordController.deleteExpense",
            // Result filtered/validated against a tenant-scoped parent entity
            "POSTransactionService.createTransaction",
            "POSTransactionService.addLine",
            "POSReturnService.createReturn",
            "PriceListService.addPriceListItem",
            "PriceListService.updatePriceListItem",
            "PriceListService.enrichPriceListItemDto",
            "PricingService.calculateItemPrice",
            "PricingService.getBasePrice",
            "ProductImageService.deleteImage",
            "ProductImageService.setPrimaryImage",
            "ProductService.updateVariant",
            "ProductService.deleteVariant",
            // Internal ids from rows the caller just created or owns
            "JournalEntryService.reverseEntry",
            "WebCampaignDispatcher.finalize",
            "WebCampaignService.toDto",
            // Id resolved from a short-lived link code / system notification fan-out
            "TelegramBotService.handleLinkCode",
            "TelegramNotificationService.sendAlert",
            "TelegramNotificationService.sendBroadcastAlert"
    );

    private static JavaClasses mainClasses;

    @BeforeAll
    static void importClasses() {
        mainClasses = new ClassFileImporter()
                .withImportOption(new ImportOption.DoNotIncludeTests())
                .importPackages("com.hisobnoma.platform");
    }

    @Test
    void rawIdLookups_onTenantScopedRepositories_areAllowlisted() {
        Set<String> tenantScopedRepos = findTenantScopedRepositories();
        Set<String> violations = new TreeSet<>();

        for (var javaClass : mainClasses) {
            for (JavaMethodCall call : javaClass.getMethodCallsFromSelf()) {
                if (!ID_LOOKUP_METHODS.contains(call.getTarget().getName())) {
                    continue;
                }
                if (!tenantScopedRepos.contains(call.getTargetOwner().getName())) {
                    continue;
                }
                String origin = call.getOriginOwner().getSimpleName() + "." + call.getOrigin().getName();
                if (!ALLOWLIST.contains(origin)) {
                    violations.add(origin + " -> " + call.getTargetOwner().getSimpleName()
                            + "." + call.getTarget().getName()
                            + " (line " + call.getLineNumber() + ")");
                }
            }
        }

        assertTrue(violations.isEmpty(),
                "Unscoped id lookups on tenant-scoped repositories (use findByIdAndTenantId, "
                        + "or allowlist with a justification):\n  "
                        + String.join("\n  ", violations));
    }

    /**
     * Repository interfaces whose entity type extends TenantAwareEntity,
     * resolved from the real classpath via Spring's ResolvableType.
     */
    private Set<String> findTenantScopedRepositories() {
        Set<String> result = new HashSet<>();
        for (var javaClass : mainClasses) {
            if (!javaClass.isInterface() || !javaClass.getName().endsWith("Repository")) {
                continue;
            }
            try {
                Class<?> repoClass = Class.forName(javaClass.getName());
                if (!Repository.class.isAssignableFrom(repoClass)) {
                    continue;
                }
                Class<?> entityType = ResolvableType.forClass(repoClass)
                        .as(Repository.class).getGeneric(0).resolve();
                if (entityType != null && TenantAwareEntity.class.isAssignableFrom(entityType)) {
                    result.add(repoClass.getName());
                }
            } catch (ClassNotFoundException | NoClassDefFoundError e) {
                // not loadable in the test classpath — skip
            }
        }
        assertTrue(result.size() > 10, "Sanity check: expected to find tenant-scoped repositories");
        return result;
    }
}
