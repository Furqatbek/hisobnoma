package com.hisobnoma.platform.distribution.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionRouteStopRequest {

    @NotNull(message = "Customer is required")
    private Long customerId;

    @Builder.Default
    private Integer sortOrder = 0;

    private LocalTime visitWindowStart;
    private LocalTime visitWindowEnd;

    private BigDecimal latitude;
    private BigDecimal longitude;

    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
