package com.hisobnoma.platform.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** The server's authoritative totals for a cart, computed without saving a sale. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosQuoteDto {
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
}
