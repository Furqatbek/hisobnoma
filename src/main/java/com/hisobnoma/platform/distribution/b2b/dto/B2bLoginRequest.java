package com.hisobnoma.platform.distribution.b2b.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class B2bLoginRequest {

    @NotBlank(message = "Customer code is required")
    private String code;

    @NotBlank(message = "Phone is required")
    private String phone;
}
