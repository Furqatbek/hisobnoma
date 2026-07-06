package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.TargetPeriodType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionAgentTargetDto {
    private Long id;
    private Long agentId;
    private TargetPeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private BigDecimal targetRevenue;
    private Integer targetOrders;
    private Integer targetVisits;
    private Integer targetNewCustomers;
    private BigDecimal targetCollection;
    private String notes;
}
