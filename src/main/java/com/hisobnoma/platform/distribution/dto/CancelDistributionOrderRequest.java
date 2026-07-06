package com.hisobnoma.platform.distribution.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelDistributionOrderRequest {

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
