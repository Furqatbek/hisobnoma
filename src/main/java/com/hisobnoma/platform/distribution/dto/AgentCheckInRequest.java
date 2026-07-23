package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.VisitType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Agent-app check-in. Unlike the staff {@link VisitCheckInRequest} there is no
 * {@code agentId} — the agent is always the authenticated token holder.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentCheckInRequest {

    @NotNull(message = "Customer is required")
    private Long customerId;

    private Long routeId;
    private Long routeStopId;
    private VisitType visitType;
    private BigDecimal latitude;
    private BigDecimal longitude;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    /** Maps to the internal staff request; agentId is filled from the token by the service. */
    public VisitCheckInRequest toVisitCheckInRequest() {
        return VisitCheckInRequest.builder()
                .customerId(customerId)
                .routeId(routeId)
                .routeStopId(routeStopId)
                .visitType(visitType)
                .latitude(latitude)
                .longitude(longitude)
                .notes(notes)
                .build();
    }
}
