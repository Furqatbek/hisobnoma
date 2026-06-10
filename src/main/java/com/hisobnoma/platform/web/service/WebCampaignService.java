package com.hisobnoma.platform.web.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.pos.dto.CouponDto;
import com.hisobnoma.platform.pos.dto.CreateCouponRequest;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.PromotionChannel;
import com.hisobnoma.platform.pos.repository.PromotionRepository;
import com.hisobnoma.platform.pos.service.CouponService;
import com.hisobnoma.platform.sms.dto.SmsBulkSendRequest;
import com.hisobnoma.platform.sms.entity.SmsTemplate;
import com.hisobnoma.platform.sms.repository.SmsTemplateRepository;
import com.hisobnoma.platform.sms.service.SmsService;
import com.hisobnoma.platform.web.dto.CampaignPreviewDto;
import com.hisobnoma.platform.web.dto.CreateCampaignRequest;
import com.hisobnoma.platform.web.dto.WebCampaignDto;
import com.hisobnoma.platform.web.entity.WebCampaign;
import com.hisobnoma.platform.web.entity.WebCampaignStatus;
import com.hisobnoma.platform.web.entity.WebCustomer;
import com.hisobnoma.platform.web.entity.WebSegmentType;
import com.hisobnoma.platform.web.repository.WebCampaignRepository;
import com.hisobnoma.platform.web.repository.WebCustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Staff-driven SMS marketing: build a draft against a customer segment, preview
 * its cost, then send once. Coupon codes (when a promotion is attached) are
 * generated one-per-recipient and injected as the {coupon} template variable.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebCampaignService {

    private final WebCampaignRepository campaignRepository;
    private final WebCustomerRepository webCustomerRepository;
    private final PromotionRepository promotionRepository;
    private final SmsService smsService;
    private final SmsTemplateRepository templateRepository;
    private final CouponService couponService;
    private final WebCampaignDispatcher dispatcher;
    private final SecurityContextHelper securityContextHelper;

    // ==================== CRUD ====================

    @Transactional(readOnly = true)
    public Page<WebCampaignDto> getCampaigns(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return campaignRepository.findAllByTenant(tenantId, pageable).map(this::toDto);
    }

    @Transactional(readOnly = true)
    public WebCampaignDto getCampaign(Long id) {
        return toDto(getEntity(id));
    }

    @Transactional
    public WebCampaignDto create(CreateCampaignRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        validateSegment(request.getSegmentType(), request.getSegmentParam());
        validateTemplate(request.getSmsTemplateId());
        validatePromotion(request.getPromotionId());

        WebCampaign campaign = WebCampaign.builder()
                .tenantId(tenantId)
                .name(request.getName().trim())
                .segmentType(request.getSegmentType())
                .segmentParam(request.getSegmentParam())
                .smsTemplateId(request.getSmsTemplateId())
                .promotionId(request.getPromotionId())
                .status(WebCampaignStatus.DRAFT)
                .build();
        return toDto(campaignRepository.save(campaign));
    }

    @Transactional
    public WebCampaignDto update(Long id, CreateCampaignRequest request) {
        WebCampaign campaign = getEntity(id);
        requireDraft(campaign);
        validateSegment(request.getSegmentType(), request.getSegmentParam());
        validateTemplate(request.getSmsTemplateId());
        validatePromotion(request.getPromotionId());

        campaign.setName(request.getName().trim());
        campaign.setSegmentType(request.getSegmentType());
        campaign.setSegmentParam(request.getSegmentParam());
        campaign.setSmsTemplateId(request.getSmsTemplateId());
        campaign.setPromotionId(request.getPromotionId());
        return toDto(campaignRepository.save(campaign));
    }

    @Transactional
    public void delete(Long id) {
        WebCampaign campaign = getEntity(id);
        requireDraft(campaign);
        campaignRepository.delete(campaign);
    }

    // ==================== Preview ====================

    @Transactional(readOnly = true)
    public CampaignPreviewDto preview(Long id) {
        WebCampaign campaign = getEntity(id);
        int count = resolveSegment(campaign).size();
        BigDecimal cost = BigDecimal.valueOf(smsService.smsCost()).multiply(BigDecimal.valueOf(count));
        double balance = smsService.getBalanceAmount();
        boolean known = balance >= 0;
        return CampaignPreviewDto.builder()
                .recipientCount(count)
                .estimatedCost(cost)
                .smsBalance(known ? BigDecimal.valueOf(balance) : null)
                .sufficientBalance(!known || balance >= cost.doubleValue())
                .build();
    }

    // ==================== Send ====================

    @Transactional
    public WebCampaignDto send(Long id) {
        WebCampaign campaign = getEntity(id);
        requireDraft(campaign);

        List<WebCustomer> recipients = resolveSegment(campaign);
        if (recipients.isEmpty()) {
            throw new BusinessException("Segment has no recipients");
        }

        // Guard: a {coupon} template needs a promotion to fill it
        SmsTemplate template = getTemplate(campaign.getTenantId(), campaign.getSmsTemplateId());
        boolean needsCoupon = template.getTemplate() != null && template.getTemplate().contains("{coupon}");
        if (needsCoupon && campaign.getPromotionId() == null) {
            throw new ValidationException("Template uses {coupon} but no promotion is attached");
        }

        int count = recipients.size();
        BigDecimal cost = BigDecimal.valueOf(smsService.smsCost()).multiply(BigDecimal.valueOf(count));

        // Block before going async / generating coupons so staff gets a clean error
        double balance = smsService.getBalanceAmount();
        if (balance >= 0 && balance < cost.doubleValue()) {
            throw new BusinessException(String.format(
                    "Insufficient SMS balance: have %.0f UZS, need %.0f UZS", balance, cost.doubleValue()));
        }

        // One personal single-use coupon per recipient, when a promotion is attached
        List<String> couponCodes = campaign.getPromotionId() != null
                ? generatePersonalCoupons(campaign.getPromotionId(), count)
                : List.of();

        List<SmsBulkSendRequest.Recipient> payloads = buildRecipients(recipients, couponCodes);

        campaign.setRecipientCount(count);
        campaign.setCostEstimate(cost);
        campaign.setStatus(WebCampaignStatus.SENDING);
        campaign.setFailureReason(null);
        campaignRepository.save(campaign);

        dispatcher.dispatch(campaign.getId(), campaign.getTenantId(),
                campaign.getSmsTemplateId(), payloads, null);

        return toDto(campaign);
    }

    // ==================== Segment resolution ====================

    List<WebCustomer> resolveSegment(WebCampaign campaign) {
        Long tenantId = campaign.getTenantId();
        WebSegmentType type = campaign.getSegmentType();
        return switch (type) {
            case ALL_CUSTOMERS -> webCustomerRepository.segmentAll(tenantId);
            case NEVER_ORDERED -> webCustomerRepository.segmentNeverOrdered(tenantId);
            case ORDERED_LAST_N_DAYS ->
                    webCustomerRepository.segmentOrderedSince(tenantId, cutoff(campaign.getSegmentParam()));
            case NO_ORDER_IN_N_DAYS ->
                    webCustomerRepository.segmentNoOrderSince(tenantId, cutoff(campaign.getSegmentParam()));
            case MIN_TOTAL_SPENT ->
                    webCustomerRepository.segmentMinSpent(tenantId, campaign.getSegmentParam());
        };
    }

    private Instant cutoff(BigDecimal days) {
        return Instant.now().minus(days.longValue(), ChronoUnit.DAYS);
    }

    // ==================== Helpers ====================

    private List<String> generatePersonalCoupons(Long promotionId, int count) {
        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("CAMPAIGN") // ignored — generateCoupons mints random codes
                .promotionId(promotionId)
                .maxUses(1)
                .maxUsesPerCustomer(1)
                .build();
        List<CouponDto> coupons = couponService.generateCoupons(promotionId, count, request);
        List<String> codes = new ArrayList<>(coupons.size());
        for (CouponDto coupon : coupons) {
            codes.add(coupon.getCode());
        }
        return codes;
    }

    private List<SmsBulkSendRequest.Recipient> buildRecipients(List<WebCustomer> customers, List<String> couponCodes) {
        List<SmsBulkSendRequest.Recipient> payloads = new ArrayList<>(customers.size());
        for (int i = 0; i < customers.size(); i++) {
            WebCustomer customer = customers.get(i);
            SmsBulkSendRequest.Recipient recipient = new SmsBulkSendRequest.Recipient();
            recipient.setPhone(customer.getPhone());
            java.util.Map<String, String> vars = new java.util.HashMap<>();
            vars.put("name", customer.getName() != null && !customer.getName().isBlank()
                    ? customer.getName() : "мижоз");
            if (i < couponCodes.size()) {
                vars.put("coupon", couponCodes.get(i));
            }
            recipient.setVariables(vars);
            payloads.add(recipient);
        }
        return payloads;
    }

    private void validateSegment(WebSegmentType type, BigDecimal param) {
        if (type.requiresParam() && (param == null || param.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new ValidationException("Segment " + type + " requires a positive parameter");
        }
    }

    private void validateTemplate(Long templateId) {
        // Throws if the template doesn't exist for this tenant
        getTemplate(securityContextHelper.getCurrentTenantId(), templateId);
    }

    private SmsTemplate getTemplate(Long tenantId, Long templateId) {
        return templateRepository.findByTenantIdAndId(tenantId, templateId)
                .orElseThrow(() -> new NotFoundException("SMS template not found: " + templateId));
    }

    private void validatePromotion(Long promotionId) {
        if (promotionId == null) {
            return;
        }
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Promotion promotion = promotionRepository.findByIdAndTenantId(promotionId, tenantId)
                .orElseThrow(() -> new NotFoundException("Promotion not found: " + promotionId));
        // Codes must be redeemable online, so the promotion can't be POS-only
        if (promotion.getChannel() == PromotionChannel.POS) {
            throw new ValidationException("Attached promotion must target the online shop (WEB or ALL channel)");
        }
    }

    private void requireDraft(WebCampaign campaign) {
        if (campaign.getStatus() != WebCampaignStatus.DRAFT) {
            throw new BusinessException("Only draft campaigns can be changed or sent");
        }
    }

    private WebCampaign getEntity(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return campaignRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Campaign not found: " + id));
    }

    private WebCampaignDto toDto(WebCampaign campaign) {
        String templateName = templateRepository
                .findByTenantIdAndId(campaign.getTenantId(), campaign.getSmsTemplateId())
                .map(SmsTemplate::getName)
                .orElse(null);
        String promotionCode = null;
        if (campaign.getPromotionId() != null) {
            promotionCode = promotionRepository.findById(campaign.getPromotionId())
                    .map(Promotion::getCode).orElse(null);
        }
        return WebCampaignDto.builder()
                .id(campaign.getId())
                .name(campaign.getName())
                .segmentType(campaign.getSegmentType())
                .segmentParam(campaign.getSegmentParam())
                .smsTemplateId(campaign.getSmsTemplateId())
                .smsTemplateName(templateName)
                .promotionId(campaign.getPromotionId())
                .promotionCode(promotionCode)
                .status(campaign.getStatus())
                .recipientCount(campaign.getRecipientCount())
                .sentCount(campaign.getSentCount())
                .failedCount(campaign.getFailedCount())
                .costEstimate(campaign.getCostEstimate())
                .failureReason(campaign.getFailureReason())
                .sentAt(campaign.getSentAt())
                .createdAt(campaign.getCreatedAt())
                .build();
    }
}
