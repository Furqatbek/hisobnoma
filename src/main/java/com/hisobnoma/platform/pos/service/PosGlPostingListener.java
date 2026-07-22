package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.pos.event.PosSaleCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Posts a completed POS sale to the general ledger AFTER its transaction commits. Because the sale
 * is already durable when this runs, the GL entry can never be a "phantom" for a sale that rolled
 * back — the defect of posting GL inline in a REQUIRES_NEW transaction during completion.
 *
 * <p>Runs synchronously on the completing thread, so the security/tenant context GL posting needs
 * is still present. The actual work lives in {@link PosRetryOperations#postGl} (its own
 * transaction, reloads by id + tenant) — the same implementation the {@link POSRetryScheduler}
 * sweep uses, so the after-commit path and the recovery path cannot drift apart. A failure here
 * leaves the transaction {@code glPosted=false}, which the scheduler picks up and retries — the
 * sale itself is never affected.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PosGlPostingListener {

    private final PosRetryOperations retryOperations;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaleCompleted(PosSaleCompletedEvent event) {
        try {
            retryOperations.postGl(event.transactionId(), event.tenantId());
        } catch (Exception e) {
            // Non-blocking: the sale is committed. POSRetryScheduler will retry the GL post.
            log.error("Deferred GL posting failed for transaction {}; retry scheduler will handle it: {}",
                    event.transactionId(), e.getMessage());
        }
    }
}
