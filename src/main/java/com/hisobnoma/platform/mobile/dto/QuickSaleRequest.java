package com.hisobnoma.platform.mobile.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for quick sale from mobile.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuickSaleRequest {

    @NotNull(message = "Terminal ID is required")
    private Long terminalId;

    @NotEmpty(message = "At least one item is required")
    private List<QuickSaleItem> items;

    @NotNull(message = "Payment type is required")
    private String paymentType;

    private BigDecimal tenderedAmount;
    private Long customerId;
    private String customerName;
    private String notes;

    /**
     * Optional client-generated idempotency key (a UUID). Send the same value when safe-retrying a
     * sale whose response was lost: the server returns the original transaction instead of creating
     * a duplicate. Omit it to keep the legacy (non-idempotent) behaviour.
     */
    @jakarta.validation.constraints.Size(max = 100, message = "clientRequestId must not exceed 100 characters")
    private String clientRequestId;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuickSaleItem {
        @NotNull(message = "Product ID is required")
        private Long productId;
        private Long variantId;
        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal discountAmount;
    }
}
