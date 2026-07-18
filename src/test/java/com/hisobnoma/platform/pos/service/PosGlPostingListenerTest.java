package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.finance.service.GLIntegrationService;
import com.hisobnoma.platform.pos.entity.POSPayment;
import com.hisobnoma.platform.pos.entity.POSTransaction;
import com.hisobnoma.platform.pos.entity.POSTransactionLine;
import com.hisobnoma.platform.pos.event.PosSaleCompletedEvent;
import com.hisobnoma.platform.pos.repository.POSTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PosGlPostingListenerTest {

    @Mock private POSTransactionRepository transactionRepository;
    @Mock private GLIntegrationService glIntegrationService;
    @InjectMocks private PosGlPostingListener listener;

    private POSTransaction tx(boolean glPosted) {
        POSTransaction t = POSTransaction.builder()
                .transactionNumber("TXN-1").glPosted(glPosted)
                .lines(new ArrayList<POSTransactionLine>())
                .payments(new ArrayList<POSPayment>())
                .build();
        t.setId(5L);
        return t;
    }

    @Test
    void postsGlAndFlagsTransaction() {
        POSTransaction t = tx(false);
        when(transactionRepository.findByIdAndTenantId(5L, 3L)).thenReturn(Optional.of(t));
        when(glIntegrationService.postPOSTransaction(t)).thenReturn(900L);

        listener.onSaleCompleted(new PosSaleCompletedEvent(5L, 3L));

        assertTrue(t.isGlPosted());
        assertEquals(900L, t.getGlJournalEntryId());
        verify(transactionRepository).save(t);
    }

    @Test
    void skipsWhenAlreadyPosted() {
        when(transactionRepository.findByIdAndTenantId(5L, 3L)).thenReturn(Optional.of(tx(true)));

        listener.onSaleCompleted(new PosSaleCompletedEvent(5L, 3L));

        verify(glIntegrationService, never()).postPOSTransaction(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void glFailureIsSwallowedSoTheSaleIsUnaffected() {
        POSTransaction t = tx(false);
        when(transactionRepository.findByIdAndTenantId(5L, 3L)).thenReturn(Optional.of(t));
        when(glIntegrationService.postPOSTransaction(t)).thenThrow(new RuntimeException("GL down"));

        // Must not propagate — the sale is already committed; the retry scheduler will handle it.
        assertDoesNotThrow(() -> listener.onSaleCompleted(new PosSaleCompletedEvent(5L, 3L)));
        assertFalse(t.isGlPosted());
    }
}
