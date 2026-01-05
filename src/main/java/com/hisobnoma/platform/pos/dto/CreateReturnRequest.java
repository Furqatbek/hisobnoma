package com.hisobnoma.platform.pos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for creating a POS return transaction.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReturnRequest {

    /**
     * Original transaction ID to return against (optional - can be standalone return)
     */
    private Long originalTransactionId;

    /**
     * Original transaction number to return against (alternative to ID)
     */
    private String originalTransactionNumber;

    /**
     * Reason for the return
     */
    @NotNull(message = "Return reason is required")
    @Size(min = 3, max = 500, message = "Return reason must be between 3 and 500 characters")
    private String returnReason;

    /**
     * Customer ID (optional)
     */
    private Long customerId;

    /**
     * Items to return
     */
    @NotEmpty(message = "At least one return item is required")
    @Valid
    private List<ReturnLineItem> items;

    /**
     * Refund method
     */
    @NotNull(message = "Refund method is required")
    private RefundMethod refundMethod;

    /**
     * Notes for the return
     */
    @Size(max = 1000)
    private String notes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnLineItem {

        /**
         * Original line ID from the original transaction (optional)
         */
        private Long originalLineId;

        /**
         * Product ID
         */
        @NotNull(message = "Product ID is required")
        private Long productId;

        /**
         * Variant ID (optional)
         */
        private Long variantId;

        /**
         * Quantity to return
         */
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        private BigDecimal quantity;

        /**
         * Unit price for the return (if different from original)
         */
        private BigDecimal unitPrice;

        /**
         * Reason for returning this specific item
         */
        @Size(max = 500)
        private String reason;
    }

    public enum RefundMethod {
        CASH,
        CARD,
        STORE_CREDIT,
        ORIGINAL_PAYMENT_METHOD
    }
}
