package com.hisobnoma.platform.pos.dto;

import com.hisobnoma.platform.pos.entity.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
