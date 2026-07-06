package com.hisobnoma.platform.distribution.dto;

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
public class CreateVanLoadoutRequest {

    @NotNull(message = "Agent is required")
    private Long agentId;

    @NotNull(message = "Vehicle location is required")
    private Long vehicleLocationId;

    @NotNull(message = "Source warehouse is required")
    private Long sourceLocationId;

    private LocalDate loadoutDate;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @NotEmpty(message = "At least one line is required")
    @Valid
    private List<LineRequest> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineRequest {
        @NotNull(message = "Product is required")
        private Long productId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.0001", message = "Quantity must be positive")
        private BigDecimal quantityLoaded;
    }
}
