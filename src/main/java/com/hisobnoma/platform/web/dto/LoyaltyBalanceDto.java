package com.hisobnoma.platform.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyBalanceDto {

    private BigDecimal balance;
    private boolean enabled;
    private BigDecimal minRedeem;
    private int maxRedeemPercent;
    private List<LoyaltyEntryDto> entries;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoyaltyEntryDto {
        private Long id;
        private String type;
        private BigDecimal amount;
        private String orderNumber;
        private String note;
        private Instant expiresAt;
        private Instant createdAt;
    }
}
