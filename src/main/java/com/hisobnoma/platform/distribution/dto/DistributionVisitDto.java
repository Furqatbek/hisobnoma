package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.VisitOutcome;
import com.hisobnoma.platform.distribution.entity.VisitType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionVisitDto {
    private Long id;
    private Long agentId;
    private Long customerId;
    private String customerName;
    private Long routeId;
    private Long routeStopId;
    private Instant checkInAt;
    private Instant checkOutAt;
    private BigDecimal checkInLat;
    private BigDecimal checkInLng;
    private BigDecimal checkOutLat;
    private BigDecimal checkOutLng;
    private VisitType visitType;
    private VisitOutcome outcome;
    private Long distributionOrderId;
    private String notes;
}
