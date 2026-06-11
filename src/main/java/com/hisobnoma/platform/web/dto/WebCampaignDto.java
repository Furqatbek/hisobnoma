package com.hisobnoma.platform.web.dto;

import com.hisobnoma.platform.web.entity.CampaignChannel;
import com.hisobnoma.platform.web.entity.WebCampaignStatus;
import com.hisobnoma.platform.web.entity.WebSegmentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Staff-facing view of a campaign.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebCampaignDto {

    private Long id;
    private String name;
    private CampaignChannel channel;
    private WebSegmentType segmentType;
    private BigDecimal segmentParam;
    private Long smsTemplateId;
    private String smsTemplateName;
    private Long promotionId;
    private String promotionCode;
    private WebCampaignStatus status;
    private Integer recipientCount;
    private Integer sentCount;
    private Integer failedCount;
    private BigDecimal costEstimate;
    private String failureReason;
    private Instant sentAt;
    private Instant createdAt;
}
