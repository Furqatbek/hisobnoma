package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.sms.dto.SmsBulkSendRequest;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.entity.WebCampaign;
import com.hisobnoma.platform.web.entity.WebCampaignStatus;
import com.hisobnoma.platform.web.repository.WebCampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebCampaignDispatcherTest {

    @Mock private SmsService smsService;
    @Mock private WebCampaignRepository campaignRepository;
    @Mock private WebPushService pushService;

    @InjectMocks
    private WebCampaignDispatcher dispatcher;

    private WebCampaign campaign;

    @BeforeEach
    void setUp() {
        campaign = WebCampaign.builder().name("C").status(WebCampaignStatus.SENDING).tenantId(1L).build();
        campaign.setId(10L);
        lenient().when(campaignRepository.findById(10L)).thenReturn(Optional.of(campaign));
        lenient().when(campaignRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private List<SmsBulkSendRequest.Recipient> recipients(int n) {
        return java.util.stream.IntStream.range(0, n).mapToObj(i -> {
            SmsBulkSendRequest.Recipient r = new SmsBulkSendRequest.Recipient();
            r.setPhone("99890000000" + i);
            return r;
        }).toList();
    }

    @Test
    void dispatch_allSentFinalizesAsSent() {
        when(smsService.sendBulk(eq(5L), anyList(), isNull()))
                .thenReturn(Map.of("total", 3, "sent", 3, "failed", 0));

        dispatcher.dispatch(10L, 1L, 5L, recipients(3), List.of(), null, null);

        assertEquals(WebCampaignStatus.SENT, campaign.getStatus());
        assertEquals(3, campaign.getSentCount());
        assertEquals(0, campaign.getFailedCount());
        assertNotNull(campaign.getSentAt());
    }

    @Test
    void dispatch_partialFailureStillSentWithFailedCount() {
        when(smsService.sendBulk(eq(5L), anyList(), isNull()))
                .thenReturn(Map.of("total", 3, "sent", 2, "failed", 1));

        dispatcher.dispatch(10L, 1L, 5L, recipients(3), List.of(), null, null);

        assertEquals(WebCampaignStatus.SENT, campaign.getStatus());
        assertEquals(2, campaign.getSentCount());
        assertEquals(1, campaign.getFailedCount());
    }

    @Test
    void dispatch_allFailedFinalizesAsFailed() {
        when(smsService.sendBulk(eq(5L), anyList(), isNull()))
                .thenReturn(Map.of("total", 2, "sent", 0, "failed", 2));

        dispatcher.dispatch(10L, 1L, 5L, recipients(2), List.of(), null, null);

        assertEquals(WebCampaignStatus.FAILED, campaign.getStatus());
        assertEquals(0, campaign.getSentCount());
        assertEquals(2, campaign.getFailedCount());
    }

    @Test
    void dispatch_exceptionFinalizesAsFailedWithReason() {
        when(smsService.sendBulk(eq(5L), anyList(), isNull()))
                .thenThrow(new RuntimeException("balance gone"));

        dispatcher.dispatch(10L, 1L, 5L, recipients(4), List.of(), null, null);

        assertEquals(WebCampaignStatus.FAILED, campaign.getStatus());
        assertEquals(0, campaign.getSentCount());
        assertEquals(4, campaign.getFailedCount());
        assertEquals("balance gone", campaign.getFailureReason());
    }

    @Test
    void dispatch_pushOnlySucceeds() {
        dispatcher.dispatch(10L, 1L, 5L, List.of(), List.of(1L, 2L, 3L), "Hello", null);

        assertEquals(WebCampaignStatus.SENT, campaign.getStatus());
        assertEquals(3, campaign.getSentCount());
        verify(pushService, times(3)).sendToCustomer(eq(1L), anyLong(),
                eq("Акция"), eq("Hello"), anyMap());
    }

    @Test
    void dispatch_pushAndSmsCombined() {
        when(smsService.sendBulk(eq(5L), anyList(), isNull()))
                .thenReturn(Map.of("total", 2, "sent", 2, "failed", 0));

        dispatcher.dispatch(10L, 1L, 5L, recipients(2), List.of(1L), "Hello", null);

        assertEquals(WebCampaignStatus.SENT, campaign.getStatus());
        assertEquals(3, campaign.getSentCount());
    }
}
