package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Cost preview shown before a campaign is sent. The send button stays disabled
 * until the staff has seen the recipient count and confirmed the cost.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignPreviewDto {

    private int recipientCount;
    private BigDecimal estimatedCost;

    /**
     * Current SMS balance in UZS, or null when it can't be retrieved.
     */
    private BigDecimal smsBalance;

    /**
     * False when the balance is known and below the estimated cost.
     */
    private boolean sufficientBalance;
}
