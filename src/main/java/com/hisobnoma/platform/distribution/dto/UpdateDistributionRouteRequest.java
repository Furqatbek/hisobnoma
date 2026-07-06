package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.RouteStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.List;

/**
 * Partial update. Non-null scalar fields are applied; a non-null {@code stops}
 * list fully replaces the route's stops.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDistributionRouteRequest {

    @Size(max = 200, message = "Name must not exceed 200 characters")
    private String name;

    private Long agentId;

    private Long territoryRegionId;

    private DayOfWeek dayOfWeek;

    private Integer estimatedDurationMinutes;

    private BigDecimal distanceKm;

    private RouteStatus status;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @Valid
    private List<DistributionRouteStopRequest> stops;
}
