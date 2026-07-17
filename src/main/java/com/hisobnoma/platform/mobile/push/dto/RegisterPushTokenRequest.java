package com.hisobnoma.platform.mobile.push.dto;

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
public class RegisterPushTokenRequest {

    @NotBlank(message = "token is required")
    @Size(max = 512, message = "token must not exceed 512 characters")
    private String token;

    /** "ios" (later "android"). Defaults to "ios". */
    @Size(max = 20)
    private String platform;

    /** "sandbox" or "production". Defaults to production. */
    @Size(max = 20)
    private String environment;

    @Size(max = 40)
    private String appVersion;
}
