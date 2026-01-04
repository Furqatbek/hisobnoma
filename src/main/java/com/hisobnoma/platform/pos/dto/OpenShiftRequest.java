package com.hisobnoma.platform.pos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class OpenShiftRequest {

    @NotNull(message = "Terminal ID is required")
    private Long terminalId;

    @NotNull(message = "Opening cash is required")
    @DecimalMin(value = "0", message = "Opening cash must be non-negative")
    private BigDecimal openingCash;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
