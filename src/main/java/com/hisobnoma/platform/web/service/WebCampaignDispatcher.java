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

/**
 * Runs the actual SMS sending off the request thread. Sits in its own bean so
 * the {@code @Async} proxy applies (a self-invoked async method would run
 * synchronously). Security/tenant context is NOT propagated to async threads,
 * so the tenant is set explicitly for template resolution.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebCampaignDispatcher {

    private final SmsService smsService;
    private final WebCampaignRepository campaignRepository;

    @Async
    public void dispatch(Long campaignId, Long tenantId, Long templateId,
                         List<SmsBulkSendRequest.Recipient> recipients, String from) {
        TenantContext.setCurrentTenant(tenantId);
        try {
            Map<String, Object> result = smsService.sendBulk(templateId, recipients, from);
            int sent = asInt(result.get("sent"));
            int failed = asInt(result.get("failed"));
            // Any success finalizes as SENT (with the failed count recorded);
            // only a wholesale failure is FAILED.
            finalize(campaignId, sent, failed,
                    sent > 0 ? WebCampaignStatus.SENT : WebCampaignStatus.FAILED, null);
        } catch (Exception e) {
            log.warn("Campaign {} dispatch failed: {}", campaignId, e.getMessage());
            finalize(campaignId, 0, recipients.size(), WebCampaignStatus.FAILED, truncate(e.getMessage()));
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
