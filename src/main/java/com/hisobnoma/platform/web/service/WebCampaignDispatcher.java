package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.common.tenant.TenantContext;
import com.hisobnoma.platform.sms.dto.SmsBulkSendRequest;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.entity.WebCampaign;
import com.hisobnoma.platform.web.entity.WebCampaignStatus;
import com.hisobnoma.platform.web.repository.WebCampaignRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebCampaignDispatcher {

    private final SmsService smsService;
    private final WebCampaignRepository campaignRepository;
    private final WebPushService pushService;

    @Async
    public void dispatch(Long campaignId, Long tenantId, Long templateId,
                         List<SmsBulkSendRequest.Recipient> smsRecipients,
                         List<Long> pushCustomerIds, String messageTemplate,
                         String from) {
        TenantContext.setCurrentTenant(tenantId);
        try {
            int totalSent = 0;
            int totalFailed = 0;

            // Push phase
            if (pushCustomerIds != null && !pushCustomerIds.isEmpty()) {
                String pushBody = messageTemplate != null ? messageTemplate : "";
                for (Long customerId : pushCustomerIds) {
                    try {
                        pushService.sendToCustomer(tenantId, customerId,
                                "Акция", pushBody,
                                Map.of("type", "CAMPAIGN", "campaignId", campaignId.toString()));
                        totalSent++;
                    } catch (Exception e) {
                        log.warn("Campaign {} push to customer {} failed: {}",
                                campaignId, customerId, e.getMessage());
                        totalFailed++;
                    }
                }
            }

            // SMS phase
            String smsError = null;
            if (smsRecipients != null && !smsRecipients.isEmpty()) {
                try {
                    Map<String, Object> result = smsService.sendBulk(templateId, smsRecipients, from);
                    totalSent += asInt(result.get("sent"));
                    totalFailed += asInt(result.get("failed"));
                } catch (Exception e) {
                    log.warn("Campaign {} SMS dispatch failed: {}", campaignId, e.getMessage());
                    totalFailed += smsRecipients.size();
                    smsError = e.getMessage();
                }
            }

            finalize(campaignId, totalSent, totalFailed,
                    totalSent > 0 ? WebCampaignStatus.SENT : WebCampaignStatus.FAILED,
                    totalSent == 0 ? truncate(smsError) : null);
        } catch (Exception e) {
            log.warn("Campaign {} dispatch failed: {}", campaignId, e.getMessage());
            int total = (smsRecipients != null ? smsRecipients.size() : 0)
                    + (pushCustomerIds != null ? pushCustomerIds.size() : 0);
            finalize(campaignId, 0, total, WebCampaignStatus.FAILED, truncate(e.getMessage()));
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional
    protected void finalize(Long campaignId, int sent, int failed,
                            WebCampaignStatus status, String failureReason) {
        WebCampaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign == null) {
            log.warn("Campaign {} vanished before finalization", campaignId);
            return;
        }
        campaign.setSentCount(sent);
        campaign.setFailedCount(failed);
        campaign.setStatus(status);
        campaign.setFailureReason(failureReason);
        campaign.setSentAt(Instant.now());
        campaignRepository.save(campaign);
        log.info("Campaign {} finalized: status={}, sent={}, failed={}", campaignId, status, sent, failed);
    }

    private static int asInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
