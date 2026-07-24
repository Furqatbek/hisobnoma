package com.hisobnoma.platform.mobile.dto;

import com.hisobnoma.platform.finance.entity.ARPaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Mobile admin: accept and register a payment from a debtor (customer who owes).
 * The amount settles the customer's unpaid invoices oldest-due-first automatically;
 * pass {@code invoiceId} to target one specific invoice instead.
 */
@Data
public class MobileDebtorPaymentRequest {

    @NotNull(message = "customerId is required")
    private Long customerId;

    @NotNull(message = "amount is required")
    @Positive(message = "amount must be positive")
    private BigDecimal amount;

    /** Defaults to CASH. */
    private ARPaymentMethod paymentMethod;

    /** Optional: apply the whole payment to this one invoice instead of oldest-first. */
    private Long invoiceId;

    private String referenceNumber;
    private String notes;
}
