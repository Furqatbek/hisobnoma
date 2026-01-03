package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
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
public class StockTransferRequest {

    @NotNull(message = "Source location ID is required")
    private Long fromLocationId;

    @NotNull(message = "Destination location ID is required")
    private Long toLocationId;

    private String referenceNumber;

    @Size(max = 200, message = "Reason must not exceed 200 characters")
    private String reason;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<TransferItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferItem {
        @NotNull(message = "Product ID is required")
        private Long productId;

        private Long productVariantId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be positive")
        private BigDecimal quantity;

        // Batch tracking - which batch to transfer
        private String batchNumber;

        // Serial tracking - which serials to transfer
        private List<String> serialNumbers;

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        private String notes;
    }
}
