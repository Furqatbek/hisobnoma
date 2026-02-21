package com.hisobnoma.platform.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PinLoginRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "PIN is required")
    private String pin;
}
