package com.hisobnoma.platform.pos.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoldTransactionRequest {

    @Size(max = 200, message = "Reason must not exceed 200 characters")
    private String reason;
}
