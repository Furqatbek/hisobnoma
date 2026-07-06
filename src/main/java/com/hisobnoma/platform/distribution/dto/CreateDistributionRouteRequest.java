package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.RouteStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
public class CreateDistributionRouteRequest {

    @NotBlank(message = "Code is required")
    @Size(max = 50, message = "Code must not exceed 50 characters")
    private String code;

    @NotBlank(message = "Name is required")
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
