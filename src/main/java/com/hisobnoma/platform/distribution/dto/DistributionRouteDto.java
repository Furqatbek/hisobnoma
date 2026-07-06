package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.RouteStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionRouteDto {
    private Long id;
    private String code;
    private String name;
    private Long agentId;
    private Long territoryRegionId;
    private DayOfWeek dayOfWeek;
    private Integer estimatedDurationMinutes;
    private BigDecimal distanceKm;
    private RouteStatus status;
    private String notes;
    private List<DistributionRouteStopDto> stops;
}
