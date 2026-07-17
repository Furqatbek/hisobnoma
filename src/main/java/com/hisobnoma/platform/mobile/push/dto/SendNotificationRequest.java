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
public class SendNotificationRequest {

    /** "tenant" (all app users of the tenant) or "user" (one user). Defaults to tenant. */
    private String audience;

    /** Required when audience = "user". */
    private Long userId;

    @NotBlank(message = "title is required")
    @Size(max = 200)
    private String title;

    @NotBlank(message = "body is required")
    @Size(max = 1000)
    private String body;

    /** Notification type driving in-app routing (e.g. "system", "new_order", "payment_due"). */
    @Size(max = 40)
    private String type;

    /** Target entity id for deep-linking (nullable). */
    private Long id;

    /** Optional explicit route override (e.g. "/alerts"). */
    @Size(max = 200)
    private String route;

    /** Optional badge count. */
    private Integer badge;
}
