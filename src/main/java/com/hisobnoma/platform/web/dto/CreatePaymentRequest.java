package com.hisobnoma.platform.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to start an online card payment for an existing order.
 * Phone is in the body (not the URL) and must match the order — same guard as
 * the order-status lookup.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentRequest {

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9 ()\\-]{7,20}$", message = "Invalid phone number")
    private String phone;

    @NotBlank(message = "Provider is required")
    @Pattern(regexp = "PAYME|CLICK|UZUM", message = "Provider must be PAYME, CLICK or UZUM")
    private String provider;

    /** Optional HTTPS deep-link to return the customer to after payment. */
    @Size(max = 500)
    private String returnUrl;
}
