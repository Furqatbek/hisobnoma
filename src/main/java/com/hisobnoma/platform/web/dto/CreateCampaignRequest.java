package com.hisobnoma.platform.web.dto;

import com.hisobnoma.platform.web.entity.CampaignChannel;
import com.hisobnoma.platform.web.entity.WebSegmentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Create/update a draft campaign.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCampaignRequest {

    @NotBlank(message = "Campaign name is required")
    @Size(max = 200, message = "Name cannot exceed 200 characters")
    private String name;

    @NotNull(message = "Segment type is required")
    private WebSegmentType segmentType;

    /**
     * Days (ORDERED_LAST_N_DAYS / NO_ORDER_IN_N_DAYS) or amount (MIN_TOTAL_SPENT).
     */
    private BigDecimal segmentParam;

    @NotNull(message = "SMS template is required")
    private Long smsTemplateId;

    /**
     * Optional promotion — one personal coupon is generated per recipient.
     */
    private Long promotionId;

    @Builder.Default
    private CampaignChannel channel = CampaignChannel.SMS;
}
