package com.hisobnoma.platform.distribution.b2b.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class B2bProfileDto {
    private Long customerId;
    private String code;
    private String name;
    private String phone;
    private String currency;
    private BigDecimal creditLimit;
    private BigDecimal currentBalance;
    private BigDecimal availableCredit;
    private Integer paymentTermsDays;
    private boolean creditHold;
}
