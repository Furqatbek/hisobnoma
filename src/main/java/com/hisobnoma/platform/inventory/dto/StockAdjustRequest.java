package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockAdjustRequest {

    @NotNull(message = "Location ID is required")
    private Long locationId;

    @NotBlank(message = "Reason is required")
    @Size(max = 200, message = "Reason must not exceed 200 characters")
    private String reason;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<AdjustItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdjustItem {
        @NotNull(message = "Product ID is required")
        private Long productId;

        private Long productVariantId;

        /**
         * Adjustment quantity (positive to add, negative to subtract)
         */
        @NotNull(message = "Adjustment quantity is required")
        private BigDecimal adjustmentQuantity;

        /**
         * Or set new quantity directly (will calculate adjustment)
         */
        private BigDecimal newQuantity;

        // Batch tracking
        private String batchNumber;

        // Serial tracking - which serials to adjust
        private List<String> serialNumbers;

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        private String notes;
    }
}
