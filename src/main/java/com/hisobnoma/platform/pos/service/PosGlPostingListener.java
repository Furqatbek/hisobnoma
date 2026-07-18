package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.finance.service.GLIntegrationService;
import com.hisobnoma.platform.pos.entity.POSTransaction;
import com.hisobnoma.platform.pos.event.PosSaleCompletedEvent;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Posts a completed POS sale to the general ledger AFTER its transaction commits. Because the sale
 * is already durable when this runs, the GL entry can never be a "phantom" for a sale that rolled
 * back — the defect of posting GL inline in a REQUIRES_NEW transaction during completion.
 *
 * <p>Runs synchronously on the completing thread, so the security/tenant context
 * {@link GLIntegrationService#postPOSTransaction} needs is still present. A failure here leaves the
 * transaction {@code glPosted=false}, which {@code POSRetryScheduler} picks up and retries — the sale
 * itself is never affected.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PosGlPostingListener {

    private final POSTransactionRepository transactionRepository;
    private final GLIntegrationService glIntegrationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSaleCompleted(PosSaleCompletedEvent event) {
        try {
            POSTransaction transaction = transactionRepository.findById(event.transactionId()).orElse(null);
            if (transaction == null || transaction.isGlPosted()) {
                return; // already posted (or gone) — nothing to do
            }
            // Initialize the lazy collections postPOSTransaction reflects over before it opens its
            // own transaction.
            transaction.getLines().size();
            transaction.getPayments().size();

            Long journalEntryId = glIntegrationService.postPOSTransaction(transaction);
            transaction.setGlJournalEntryId(journalEntryId);
            transaction.setGlPosted(true);
            transactionRepository.save(transaction);
            log.debug("Posted GL for completed transaction {}", transaction.getTransactionNumber());
        } catch (Exception e) {
            // Non-blocking: the sale is committed. POSRetryScheduler will retry the GL post.
            log.error("Deferred GL posting failed for transaction {}; retry scheduler will handle it: {}",
                    event.transactionId(), e.getMessage());
        }
    }
}
