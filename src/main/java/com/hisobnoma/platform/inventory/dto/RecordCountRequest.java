package com.hisobnoma.platform.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
public class RecordCountRequest {

    @NotNull(message = "Counted quantity is required")
    @PositiveOrZero(message = "Counted quantity must be zero or positive")
    private BigDecimal countedQuantity;

    @Size(max = 100, message = "Batch number must not exceed 100 characters")
    private String batchNumber;

    @Size(max = 100, message = "Serial number must not exceed 100 characters")
    private String serialNumber;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
