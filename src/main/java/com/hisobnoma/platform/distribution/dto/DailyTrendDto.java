package com.hisobnoma.platform.distribution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One day of distribution activity for the KPI trend chart. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyTrendDto {
    private LocalDate date;
    private BigDecimal revenue;
    private long orders;
    private long visits;
    private BigDecimal collected;
}
