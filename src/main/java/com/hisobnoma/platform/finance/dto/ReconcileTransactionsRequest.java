package com.hisobnoma.platform.finance.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileTransactionsRequest {

    @NotNull(message = "Reconciliation ID is required")
    private Long reconciliationId;

    @NotNull(message = "Transaction IDs are required")
    private List<Long> transactionIds;

    private boolean clear;
}
