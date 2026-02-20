package com.hisobnoma.platform.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryRecordDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private Integer periodYear;
    private Integer periodMonth;
    private BigDecimal baseAmount;
    private BigDecimal bonusAmount;
    private BigDecimal deductionAmount;
    private BigDecimal netAmount;
    private String status;
    private LocalDate paidDate;
    private Long glJournalEntryId;
    private String notes;
}
