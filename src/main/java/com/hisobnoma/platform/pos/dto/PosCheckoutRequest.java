package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.entity.POSPaymentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * A whole sale in one request: lines, optional sale-level discount, optional
 * coupon, and the payments. Used by both the quote (totals only, nothing
 * saved) and the atomic checkout.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosCheckoutRequest {

    @NotNull(message = "Terminal ID is required")
    private Long terminalId;

    private Long customerId;
    private Long deliveryRegionId;
    private Long deliveryVillageId;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<CreateTransactionRequest.LineItem> items;

    /** Sale-level discount: either a percent (0-100) or a fixed amount. */
    @DecimalMin(value = "0", message = "Discount percent must be non-negative")
    @DecimalMax(value = "100", message = "Discount percent must not exceed 100")
    private BigDecimal discountPercent;

    @DecimalMin(value = "0", message = "Discount amount must be non-negative")
    private BigDecimal discountAmount;

    @Size(max = 200, message = "Reason must not exceed 200 characters")
    private String discountReason;

    /** Coupon whose discount is included above; its redemption is recorded with the sale. */
    @Size(max = 50)
    private String couponCode;

    /** Required for checkout, ignored by quote. */
    @Valid
    private List<Payment> payments;

    /**
     * The total the cashier was shown. When set, checkout is rejected with
     * {@code TOTAL_CHANGED} if the server's total differs, so the customer is
     * never charged an amount other than the one quoted.
     */
    private BigDecimal expectedTotal;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payment {
        @NotNull(message = "Payment type is required")
        private POSPaymentType paymentType;

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        private BigDecimal amount;

        /** Cash handed over (for change). */
        private BigDecimal tenderedAmount;
    }
}
