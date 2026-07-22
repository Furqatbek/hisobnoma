package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.UserPrincipal;
import com.hisobnoma.platform.pos.entity.POSTransaction;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduled retry for failed GL postings and AR invoice creations.
 * Runs periodically to pick up transactions that failed during completion
 * (e.g. GL service was temporarily unavailable) and retry them.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class POSRetryScheduler {

    private final POSTransactionRepository transactionRepository;
    private final PosRetryOperations retryOperations;

    /**
     * Retry failed GL postings every 15 minutes.
     */
    @Scheduled(fixedDelay = 900_000, initialDelay = 60_000)
    public void retryFailedGlPostings() {
        List<POSTransaction> failed = transactionRepository.findAllCompletedWithoutGlPosting();
        if (failed.isEmpty()) {
            return;
        }

        log.info("Retrying GL posting for {} transactions", failed.size());
        int success = 0;
        int errors = 0;

        for (POSTransaction transaction : failed) {
            try {
                // Cross-bean call with ids only: PosRetryOperations reloads the entity inside its
                // own transaction. (The previous self-invoked @Transactional never started one, and
                // the detached entity's lazy payments made every retry die on LazyInitialization.)
                runAsSystemUser(transaction.getTenantId(),
                        () -> retryOperations.postGl(transaction.getId(), transaction.getTenantId()));
                success++;
            } catch (Exception e) {
                errors++;
                log.warn("GL retry failed for transaction {} (tenant {}): {}",
                        transaction.getTransactionNumber(), transaction.getTenantId(), e.getMessage());
            }
        }

        log.info("GL retry complete: {} succeeded, {} failed", success, errors);
    }

    /**
     * Retry failed AR invoice creation every 15 minutes.
     */
    @Scheduled(fixedDelay = 900_000, initialDelay = 120_000)
    public void retryFailedArInvoices() {
        List<POSTransaction> failed = transactionRepository.findAllCompletedCreditWithoutArInvoice();
        if (failed.isEmpty()) {
            return;
        }

        log.info("Retrying AR invoice creation for {} transactions", failed.size());
        int success = 0;
        int errors = 0;

        for (POSTransaction transaction : failed) {
            try {
                runAsSystemUser(transaction.getTenantId(),
                        () -> retryOperations.createArInvoice(transaction.getId(), transaction.getTenantId()));
                success++;
            } catch (Exception e) {
                errors++;
                log.warn("AR invoice retry failed for transaction {} (tenant {}): {}",
                        transaction.getTransactionNumber(), transaction.getTenantId(), e.getMessage());
            }
        }

        log.info("AR invoice retry complete: {} succeeded, {} failed", success, errors);
    }

    private void runAsSystemUser(Long tenantId, Runnable action) {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        try {
            UserPrincipal systemPrincipal = new UserPrincipal(
                    0L, "system", "", tenantId, true, true,
                    List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"))
            );
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(systemPrincipal, null, systemPrincipal.getAuthorities())
            );
            action.run();
        } finally {
            SecurityContextHolder.getContext().setAuthentication(previous);
        }
    }
}
