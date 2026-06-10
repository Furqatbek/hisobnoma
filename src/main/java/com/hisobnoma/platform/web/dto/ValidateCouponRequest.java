package com.hisobnoma.platform.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Coupon validation payload: the code plus the cart it would apply to
 * (the discount depends on the cart total).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidateCouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(max = 50, message = "Coupon code cannot exceed 50 characters")
    private String code;

    @NotEmpty(message = "At least one line is required")
    @Size(max = 50, message = "Cart cannot contain more than 50 lines")
    @Valid
    private List<CheckoutRequest.CheckoutLine> lines;
}
