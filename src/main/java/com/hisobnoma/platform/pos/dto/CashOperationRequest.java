package com.hisobnoma.platform.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request for cash in/out operations during a shift
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashOperationRequest {

    public enum OperationType {
        CASH_IN,
        CASH_OUT
    }

    @NotNull(message = "Operation type is required")
    private OperationType operationType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
