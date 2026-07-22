package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.pos.event.PosSaleCompletedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PosGlPostingListenerTest {

    @Mock private PosRetryOperations retryOperations;
    @InjectMocks private PosGlPostingListener listener;

    @Test
    void delegatesToRetryOperationsWithIdAndTenant() {
        listener.onSaleCompleted(new PosSaleCompletedEvent(5L, 3L));

        verify(retryOperations).postGl(5L, 3L);
    }

    @Test
    void glFailureIsSwallowedSoTheSaleIsUnaffected() {
        when(retryOperations.postGl(5L, 3L)).thenThrow(new RuntimeException("GL down"));

        // Must not propagate — the sale is already committed; the retry scheduler will handle it.
        assertDoesNotThrow(() -> listener.onSaleCompleted(new PosSaleCompletedEvent(5L, 3L)));
    }
}
