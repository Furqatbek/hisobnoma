package com.hisobnoma.platform.distribution.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * End-of-day reconciliation: per-line returned/damaged counts (sold is derived) plus
 * the cash the agent actually handed in.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileVanLoadoutRequest {

    @DecimalMin(value = "0", message = "Cash must be non-negative")
    private BigDecimal actualCash;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @Valid
    private List<LineReconcile> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineReconcile {
        @NotNull(message = "Line id is required")
        private Long lineId;

        @DecimalMin(value = "0", message = "Returned must be non-negative")
        @Builder.Default
        private BigDecimal quantityReturned = BigDecimal.ZERO;

        @DecimalMin(value = "0", message = "Damaged must be non-negative")
        @Builder.Default
        private BigDecimal quantityDamaged = BigDecimal.ZERO;
    }
}
