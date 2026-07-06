package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.VisitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VisitCheckInRequest {

    @NotNull(message = "Agent is required")
    private Long agentId;

    @NotNull(message = "Customer is required")
    private Long customerId;

    private Long routeId;

    private Long routeStopId;

    private VisitType visitType;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
