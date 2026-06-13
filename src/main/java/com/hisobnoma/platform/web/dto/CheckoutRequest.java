package com.hisobnoma.platform.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Anonymous checkout payload from the mobile app.
 * Prices are NEVER taken from the client — the server snapshots the
 * current catalog price for every line.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotBlank(message = "Customer name is required")
    @Size(max = 200, message = "Customer name cannot exceed 200 characters")
    private String customerName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^\\+?[0-9 ()\\-]{7,20}$", message = "Invalid phone number")
    private String phone;

    private Long regionId;

    private Long villageId;

    @Size(max = 500, message = "Note cannot exceed 500 characters")
    private String note;

    /**
     * Free-text delivery address (street/house/landmark). Required by the app
     * UI; kept optional here so older clients don't break.
     */
    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    /**
     * "CASH" (default) or "CARD". Anything unrecognized is treated as CASH.
     */
    @Pattern(regexp = "CASH|CARD", message = "Payment method must be CASH or CARD")
    private String paymentMethod;

    @Size(max = 50, message = "Coupon code cannot exceed 50 characters")
    private String couponCode;

    private BigDecimal pointsToSpend;

    @NotEmpty(message = "At least one order line is required")
    @Size(max = 50, message = "Order cannot contain more than 50 lines")
    @Valid
    private List<CheckoutLine> lines;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutLine {

        @NotNull(message = "Catalog item ID is required")
        private Long catalogItemId;

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.001", message = "Quantity must be at least 0.001")
        @DecimalMax(value = "10000", message = "Quantity cannot exceed 10000")
        private BigDecimal quantity;
    }
}
