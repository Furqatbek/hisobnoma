package com.hisobnoma.platform.finance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrencyDto {
    private Long id;
    private String code;
    private String name;
    private String symbol;
    private Integer decimalPlaces;
    private boolean baseCurrency;
    private boolean active;
    private String formatPattern;
    private String symbolPosition;
    private String thousandsSeparator;
    private String decimalSeparator;
    private Integer sortOrder;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
