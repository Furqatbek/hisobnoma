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
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockIssueRequest {

    @NotNull(message = "Location ID is required")
    private Long locationId;

    private MovementReferenceType referenceType;
    private Long referenceId;
    private String referenceNumber;

    @Size(max = 200, message = "Reason must not exceed 200 characters")
    private String reason;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<IssueItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueItem {
        @NotNull(message = "Product ID is required")
        private Long productId;

        private Long productVariantId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be positive")
        private BigDecimal quantity;

        // Batch tracking - which batch to issue from
        private String batchNumber;

        // Serial tracking - which serials to issue
        private List<String> serialNumbers;

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        private String notes;
    }
}
