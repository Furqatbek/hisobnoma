package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.service.ARInvoiceService;
import com.hisobnoma.platform.finance.service.GLIntegrationService;
import com.hisobnoma.platform.pos.entity.POSPayment;
import com.hisobnoma.platform.pos.entity.POSPaymentStatus;
import com.hisobnoma.platform.pos.entity.POSPaymentType;
import com.hisobnoma.platform.pos.entity.POSTransaction;
import com.hisobnoma.platform.pos.entity.POSTransactionLine;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * The shared deferred-work implementation for POS follow-ups. Critically, both methods reload the
 * transaction by id + tenant INSIDE their own transaction — the regression that motivated this
 * class was the scheduler passing detached entities into a self-invoked (and therefore
 * non-transactional) method, killing every retry with a LazyInitializationException.
 */
@ExtendWith(MockitoExtension.class)
class PosRetryOperationsTest {

    @Mock private POSTransactionRepository transactionRepository;
    @Mock private GLIntegrationService glIntegrationService;
    @Mock private ARInvoiceService arInvoiceService;
    @InjectMocks private PosRetryOperations ops;

    private POSTransaction tx(boolean glPosted, Long arInvoiceId) {
        POSTransaction t = POSTransaction.builder()
                .transactionNumber("TXN-1").glPosted(glPosted).arInvoiceId(arInvoiceId)
                .lines(new ArrayList<POSTransactionLine>())
                .payments(new ArrayList<POSPayment>())
                .build();
        t.setId(5L);
        t.setTenantId(3L);
        return t;
    }

    private POSPayment credit(BigDecimal amount, POSPaymentStatus status) {
        return POSPayment.builder()
                .paymentType(POSPaymentType.CREDIT).status(status).amount(amount).build();
    }

    // ---- postGl ----

    @Test
    void postGl_reloadsByIdAndTenant_postsAndFlags() {
        POSTransaction t = tx(false, null);
        when(transactionRepository.findByIdAndTenantId(5L, 3L)).thenReturn(Optional.of(t));
        when(glIntegrationService.postPOSTransaction(t)).thenReturn(900L);

        assertTrue(ops.postGl(5L, 3L));

        assertTrue(t.isGlPosted());
        assertEquals(900L, t.getGlJournalEntryId());
        verify(transactionRepository).save(t);
    }

    @Test
    void postGl_skipsWhenAlreadyPosted() {
        when(transactionRepository.findByIdAndTenantId(5L, 3L)).thenReturn(Optional.of(tx(true, null)));

        assertFalse(ops.postGl(5L, 3L));

        verify(glIntegrationService, never()).postPOSTransaction(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void postGl_skipsWhenTransactionGone() {
        when(transactionRepository.findByIdAndTenantId(5L, 3L)).thenReturn(Optional.empty());

        assertFalse(ops.postGl(5L, 3L));
        verifyNoInteractions(glIntegrationService);
    }

    @Test
    void postGl_propagatesGlFailureWithoutFlagging() {
        POSTransaction t = tx(false, null);
        when(transactionRepository.findByIdAndTenantId(5L, 3L)).thenReturn(Optional.of(t));
        when(glIntegrationService.postPOSTransaction(t)).thenThrow(new RuntimeException("GL down"));

        assertThrows(RuntimeException.class, () -> ops.postGl(5L, 3L));
        assertFalse(t.isGlPosted(), "flag must stay false so the sweep retries");
    }

    // ---- createArInvoice ----

    @Test
    void createArInvoice_createsAndLinksInvoice() {
        POSTransaction t = tx(false, null);
        t.getPayments().add(credit(new BigDecimal("5000"), POSPaymentStatus.APPROVED));
        when(transactionRepository.findByIdAndTenantId(5L, 3L)).thenReturn(Optional.of(t));
        when(arInvoiceService.createFromPOSTransaction(t)).thenReturn(
                ARInvoiceDto.builder().id(77L).invoiceNumber("INV-77").build());

        assertTrue(ops.createArInvoice(5L, 3L));

        assertEquals(77L, t.getArInvoiceId());
        verify(transactionRepository).save(t);
    }

    @Test
    void createArInvoice_skipsWhenAlreadyInvoicedOrNoCredit() {
        when(transactionRepository.findByIdAndTenantId(5L, 3L)).thenReturn(Optional.of(tx(false, 99L)));
        assertFalse(ops.createArInvoice(5L, 3L), "already invoiced");

        POSTransaction noCredit = tx(false, null);
        noCredit.getPayments().add(credit(new BigDecimal("5000"), POSPaymentStatus.VOIDED));
        when(transactionRepository.findByIdAndTenantId(5L, 3L)).thenReturn(Optional.of(noCredit));
        assertFalse(ops.createArInvoice(5L, 3L), "no approved credit payment");

        verifyNoInteractions(arInvoiceService);
    }
}
