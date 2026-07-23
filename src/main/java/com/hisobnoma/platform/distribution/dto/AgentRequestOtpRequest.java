package com.hisobnoma.platform.distribution.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Agent asks for an SMS login code to be sent to their phone. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentRequestOtpRequest {

    @NotBlank(message = "phone is required")
    private String phone;
}
