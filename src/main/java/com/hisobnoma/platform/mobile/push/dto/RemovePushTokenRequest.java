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
public class RemovePushTokenRequest {

    @NotBlank(message = "token is required")
    @Size(max = 512)
    private String token;
}
