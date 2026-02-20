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
public class SalaryAdvanceDto {
    private Long id;
    private Long employeeId;
    private String employeeName;
    private String employeeCode;
    private BigDecimal amount;
    private LocalDate advanceDate;
    private Integer periodYear;
    private Integer periodMonth;
    private String status;
    private Long salaryRecordId;
    private Long glJournalEntryId;
    private String notes;
}
