package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.DistributionPaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Field order placement from the agent app. There is no {@code agentId} — the agent
 * is always the authenticated token holder — and no {@code sourceLocationId}: the sale
 * draws down the agent's current van loadout by default. Pricing is server-side.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentCreateOrderRequest {

    @NotNull(message = "Customer is required")
    private Long customerId;

    /** Optional link to the visit the agent is currently on (must be their own). */
    private Long visitId;

    private Long routeId;
    private LocalDate expectedDeliveryDate;
    private DistributionPaymentMethod paymentMethod;
    private Integer paymentTermsDays;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private String deliveryAddress;
    private BigDecimal deliveryLat;
    private BigDecimal deliveryLng;
    private String notes;

    /** Immediately CONFIRM the order (reserve van stock). Default false — captured as DRAFT. */
    private boolean confirmNow;

    @NotEmpty(message = "At least one line is required")
    @Valid
    private List<DistributionOrderLineRequest> lines;

    /** Maps to the internal create request; agentId + sourceLocation are filled by the service. */
    public CreateDistributionOrderRequest toCreateRequest() {
        return CreateDistributionOrderRequest.builder()
                .customerId(customerId)
                .visitId(visitId)
                .routeId(routeId)
                .expectedDeliveryDate(expectedDeliveryDate)
                .paymentMethod(paymentMethod)
                .paymentTermsDays(paymentTermsDays)
                .discountAmount(discountAmount)
                .deliveryFee(deliveryFee)
                .deliveryAddress(deliveryAddress)
                .deliveryLat(deliveryLat)
                .deliveryLng(deliveryLng)
                .notes(notes)
                .lines(lines)
                .build();
    }
}
