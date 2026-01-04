package com.hisobnoma.platform.pos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCouponRequest {

    @NotBlank(message = "Coupon code is required")
    @Size(max = 50, message = "Code cannot exceed 50 characters")
    private String code;

    @NotNull(message = "Promotion ID is required")
    private Long promotionId;

    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxUses;
    private Integer maxUsesPerCustomer;
    private Long customerId;
    private Long restrictedToCustomerId;
    private java.math.BigDecimal minimumOrderAmount;
    private String customerEmail;
    private String notes;
}
