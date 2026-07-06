package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.DistributionPaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Edits a DRAFT order. Non-null scalar fields are applied; when {@code lines} is
 * non-null it fully replaces the order's lines (re-priced server-side).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDistributionOrderRequest {

    private Long agentId;

    private Long sourceLocationId;

    private LocalDate expectedDeliveryDate;

    private DistributionPaymentMethod paymentMethod;

    private Integer paymentTermsDays;

    @DecimalMin(value = "0", message = "Discount must be non-negative")
    private BigDecimal discountAmount;

    @DecimalMin(value = "0", message = "Tax must be non-negative")
    private BigDecimal taxAmount;

    @DecimalMin(value = "0", message = "Delivery fee must be non-negative")
    private BigDecimal deliveryFee;

    @Size(max = 500, message = "Delivery address must not exceed 500 characters")
    private String deliveryAddress;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @Size(max = 1000, message = "Internal notes must not exceed 1000 characters")
    private String internalNotes;

    @Valid
    private List<DistributionOrderLineRequest> lines;
}
