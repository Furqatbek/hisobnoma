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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final GLIntegrationService glIntegrationService;
    private final ARInvoiceService arInvoiceService;

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
                retryGlForTransaction(transaction);
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
                retryArInvoiceForTransaction(transaction);
                success++;
            } catch (Exception e) {
                errors++;
                log.warn("AR invoice retry failed for transaction {} (tenant {}): {}",
                        transaction.getTransactionNumber(), transaction.getTenantId(), e.getMessage());
            }
        }

        log.info("AR invoice retry complete: {} succeeded, {} failed", success, errors);
    }

    @Transactional
    protected void retryGlForTransaction(POSTransaction transaction) {
        Long journalEntryId = glIntegrationService.postPOSTransaction(transaction);
        transaction.setGlJournalEntryId(journalEntryId);
        transaction.setGlPosted(true);
        transactionRepository.save(transaction);
        log.info("GL retry succeeded for transaction {}", transaction.getTransactionNumber());
    }

    @Transactional
    protected void retryArInvoiceForTransaction(POSTransaction transaction) {
        boolean hasCredit = transaction.getPayments().stream()
                .anyMatch(p -> p.getPaymentType() == POSPaymentType.CREDIT
                        && p.getStatus() == POSPaymentStatus.APPROVED
                        && p.getAmount().compareTo(BigDecimal.ZERO) > 0);

        if (!hasCredit) {
            log.warn("Transaction {} no longer has credit payments, skipping AR invoice retry",
                    transaction.getTransactionNumber());
            return;
        }

        ARInvoiceDto arInvoice = arInvoiceService.createFromPOSTransaction(transaction);
        transaction.setArInvoiceId(arInvoice.getId());
        transactionRepository.save(transaction);
        log.info("AR invoice retry succeeded for transaction {} -> invoice {}",
                transaction.getTransactionNumber(), arInvoice.getInvoiceNumber());
    }
}
