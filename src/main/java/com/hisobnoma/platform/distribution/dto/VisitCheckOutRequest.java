package com.hisobnoma.platform.distribution.dto;

import com.hisobnoma.platform.distribution.entity.VisitOutcome;
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
public class VisitCheckOutRequest {

    @NotNull(message = "Outcome is required")
    private VisitOutcome outcome;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private Long distributionOrderId;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
