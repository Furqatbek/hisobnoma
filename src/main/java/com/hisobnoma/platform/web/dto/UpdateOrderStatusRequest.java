package com.hisobnoma.platform.web.dto;

import com.hisobnoma.platform.web.entity.WebOrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderStatusRequest {

    @NotNull(message = "Status is required")
    private WebOrderStatus status;

    /**
     * Required when cancelling.
     */
    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;
}
