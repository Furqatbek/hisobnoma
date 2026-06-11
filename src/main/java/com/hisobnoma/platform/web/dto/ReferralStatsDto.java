package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralStatsDto {
    private String code;
    private boolean enabled;
    private long invitedCount;
    private BigDecimal pointsEarned;
}
