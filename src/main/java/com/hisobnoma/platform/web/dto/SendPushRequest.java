package com.hisobnoma.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Staff request to manually push a message to customer-app user(s). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendPushRequest {

    @NotBlank(message = "title is required")
    @Size(max = 100)
    private String title;

    @NotBlank(message = "body is required")
    @Size(max = 500)
    private String body;
}
