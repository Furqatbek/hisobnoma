package com.hisobnoma.platform.web.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A marketing SMS campaign: a customer segment + an SMS template, optionally
 * with a batch of personal coupon codes from a promotion. Sendable only while
 * DRAFT so it can never be blasted twice.
 */
@Entity
@Table(name = "web_campaigns", indexes = {
        @Index(name = "idx_web_campaigns_tenant", columnList = "tenant_id, created_at")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WebCampaign extends TenantAwareEntity {

    @Column(nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "segment_type", nullable = false, length = 40)
    private WebSegmentType segmentType;

    /**
     * N days (day-based segments) or amount (MIN_TOTAL_SPENT); null otherwise.
     */
    @Column(name = "segment_param", precision = 18, scale = 4)
    private BigDecimal segmentParam;

    @Column(name = "sms_template_id", nullable = false)
    private Long smsTemplateId;

    /**
     * Promotion whose coupons are generated per recipient (one personal
     * single-use code each, injected as the {coupon} template variable).
     */
    @Column(name = "promotion_id")
    private Long promotionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private CampaignChannel channel = CampaignChannel.SMS;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WebCampaignStatus status = WebCampaignStatus.DRAFT;

    @Column(name = "recipient_count", nullable = false)
    @Builder.Default
    private Integer recipientCount = 0;

    @Column(name = "sent_count", nullable = false)
    @Builder.Default
    private Integer sentCount = 0;

    @Column(name = "failed_count", nullable = false)
    @Builder.Default
    private Integer failedCount = 0;

    @Column(name = "cost_estimate", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal costEstimate = BigDecimal.ZERO;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "sent_at")
    private Instant sentAt;
}
