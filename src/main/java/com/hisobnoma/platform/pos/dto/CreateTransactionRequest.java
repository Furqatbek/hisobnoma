package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransactionRequest {

    @NotNull(message = "Terminal ID is required")
    private Long terminalId;

    private Long customerId;

    @Size(max = 200, message = "Customer name must not exceed 200 characters")
    private String customerName;

    @Size(max = 50, message = "Customer phone must not exceed 50 characters")
    private String customerPhone;

    @Builder.Default
    private TransactionType transactionType = TransactionType.SALE;

    /**
     * For returns - reference to original transaction
     */
    private Long originalTransactionId;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    /**
     * Optional line items to add during transaction creation.
     */
    private List<LineItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LineItem {
        @NotNull(message = "Product ID is required")
        private Long productId;
        private Long variantId;
        @NotNull(message = "Quantity is required")
        private BigDecimal quantity;
        private BigDecimal unitPrice;
    }
}
