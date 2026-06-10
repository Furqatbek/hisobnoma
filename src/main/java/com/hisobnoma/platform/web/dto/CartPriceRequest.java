package com.hisobnoma.platform.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Cart-price preview payload: the same lines shape as checkout, no
 * customer data. The optional phone (from the auth token, never trusted
 * from the body) personalises customer-specific promotion conditions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartPriceRequest {

    @NotEmpty(message = "At least one line is required")
    @Size(max = 50, message = "Cart cannot contain more than 50 lines")
    @Valid
    private List<CheckoutRequest.CheckoutLine> lines;
}
