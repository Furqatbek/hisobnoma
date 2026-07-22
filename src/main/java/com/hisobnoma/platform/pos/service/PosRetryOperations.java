package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
import com.hisobnoma.platform.finance.service.GLIntegrationService;
import com.hisobnoma.platform.pos.entity.POSPaymentStatus;
import com.hisobnoma.platform.pos.entity.POSPaymentType;
import com.hisobnoma.platform.pos.entity.POSTransaction;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * The single transactional implementation of deferred POS follow-up work — GL posting and AR
 * invoice creation for an already-committed sale. Used by both {@link PosGlPostingListener}
 * (after-commit) and {@link POSRetryScheduler} (15-minute sweep).
 *
 * <p>Each method opens its own transaction and RELOADS the transaction by id + tenant, so callers
 * may hand over ids from detached/stale entities without lazy-initialization risk — the previous
 * design passed detached entities into a self-invoked {@code @Transactional} method (which never
 * started a transaction), so every scheduled retry died on a {@code LazyInitializationException}
 * and the recovery path was effectively dead code.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosRetryOperations {

    private final POSTransactionRepository transactionRepository;
    private final GLIntegrationService glIntegrationService;
    private final ARInvoiceService arInvoiceService;

    /**
     * Posts the sale to the GL and flags it, unless it is already posted (idempotent re-entry).
     *
     * @return true when a GL entry was posted by this call
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean postGl(Long transactionId, Long tenantId) {
        POSTransaction transaction = transactionRepository
                .findByIdAndTenantId(transactionId, tenantId).orElse(null);
        if (transaction == null || transaction.isGlPosted()) {
            return false; // already posted (or gone) — nothing to do
        }
        // Initialize the lazy collections postPOSTransaction reflects over before it opens its
        // own REQUIRES_NEW transaction.
        transaction.getLines().size();
        transaction.getPayments().size();

        Long journalEntryId = glIntegrationService.postPOSTransaction(transaction);
        transaction.setGlJournalEntryId(journalEntryId);
        transaction.setGlPosted(true);
        transactionRepository.save(transaction);
        log.info("Posted GL for transaction {} (journal entry {})",
                transaction.getTransactionNumber(), journalEntryId);
        return true;
    }

    /**
     * Creates the AR invoice for a completed credit sale that is missing one (idempotent re-entry).
     *
     * @return true when an invoice was created by this call
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean createArInvoice(Long transactionId, Long tenantId) {
        POSTransaction transaction = transactionRepository
                .findByIdAndTenantId(transactionId, tenantId).orElse(null);
        if (transaction == null || transaction.getArInvoiceId() != null) {
            return false;
        }

        boolean hasCredit = transaction.getPayments().stream()
                .anyMatch(p -> p.getPaymentType() == POSPaymentType.CREDIT
                        && p.getStatus() == POSPaymentStatus.APPROVED
                        && p.getAmount().compareTo(BigDecimal.ZERO) > 0);
        if (!hasCredit) {
            log.warn("Transaction {} no longer has credit payments, skipping AR invoice retry",
                    transaction.getTransactionNumber());
            return false;
        }

        ARInvoiceDto arInvoice = arInvoiceService.createFromPOSTransaction(transaction);
        transaction.setArInvoiceId(arInvoice.getId());
        transactionRepository.save(transaction);
        log.info("Created AR invoice {} for transaction {}",
                arInvoice.getInvoiceNumber(), transaction.getTransactionNumber());
        return true;
    }
}
