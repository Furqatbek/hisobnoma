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
public class B2bAuthResponse {
    private String token;
    private Long customerId;
    private String code;
    private String name;
    private String currency;
    private BigDecimal availableCredit;
}
