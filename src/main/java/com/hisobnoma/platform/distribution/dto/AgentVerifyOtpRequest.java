package com.hisobnoma.platform.distribution.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Agent submits the SMS code to obtain a bearer token. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentVerifyOtpRequest {

    @NotBlank(message = "phone is required")
    private String phone;

    @NotBlank(message = "code is required")
    private String code;
}
