package com.hisobnoma.platform.distribution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionRouteStopDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private Integer sortOrder;
    private LocalTime visitWindowStart;
    private LocalTime visitWindowEnd;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String notes;
}
