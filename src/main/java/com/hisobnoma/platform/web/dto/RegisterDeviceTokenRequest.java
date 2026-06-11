package com.hisobnoma.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDeviceTokenRequest {

    @NotBlank(message = "Token is required")
    @Size(max = 500, message = "Token cannot exceed 500 characters")
    private String token;

    @NotBlank(message = "Platform is required")
    @Size(max = 20, message = "Platform cannot exceed 20 characters")
    private String platform;
}
