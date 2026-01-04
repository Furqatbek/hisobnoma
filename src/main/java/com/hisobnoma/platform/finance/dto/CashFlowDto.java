package com.hisobnoma.platform.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowDto {
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal openingBalance;
    private BigDecimal totalInflows;
    private BigDecimal totalOutflows;
    private BigDecimal netCashFlow;
    private BigDecimal closingBalance;
    private List<CashFlowLineDto> inflowDetails;
    private List<CashFlowLineDto> outflowDetails;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashFlowLineDto {
        private String category;
        private String description;
        private BigDecimal amount;
        private Integer transactionCount;
    }
}
