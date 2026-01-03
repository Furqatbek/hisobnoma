package com.hisobnoma.platform.inventory.dto;

import com.hisobnoma.platform.inventory.entity.MovementReferenceType;
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
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockReceiveRequest {

    @NotNull(message = "Location ID is required")
    private Long locationId;

    private MovementReferenceType referenceType;
    private Long referenceId;
    private String referenceNumber;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<ReceiveItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiveItem {
        @NotNull(message = "Product ID is required")
        private Long productId;

        private Long productVariantId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be positive")
        private BigDecimal quantity;

        @DecimalMin(value = "0", message = "Unit cost must be non-negative")
        private BigDecimal unitCost;

        // Batch tracking fields
        private String batchNumber;
        private LocalDate manufactureDate;
        private LocalDate expiryDate;
        private String supplierBatchNumber;

        // Serial tracking fields
        private List<String> serialNumbers;

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        private String notes;
    }
}
