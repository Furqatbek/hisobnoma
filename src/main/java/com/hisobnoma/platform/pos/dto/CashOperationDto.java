package com.hisobnoma.platform.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CashOperationDto {
    private Long id;
    private Long shiftId;
    private String operationType;
    private BigDecimal amount;
    private String reason;
    private Long cashierId;
    private String cashierName;
    private Instant createdAt;
}
