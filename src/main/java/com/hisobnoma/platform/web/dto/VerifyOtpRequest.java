package com.hisobnoma.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyOtpRequest {

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9 ()\\-]{7,20}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Code is required")
    @Pattern(regexp = "^[0-9]{4,8}$", message = "Invalid code")
    private String code;

    /**
     * Optional display name, stored on first login.
     */
    private String name;
}
