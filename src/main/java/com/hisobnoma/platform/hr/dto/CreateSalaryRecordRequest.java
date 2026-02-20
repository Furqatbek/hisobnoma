package com.hisobnoma.platform.hr.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateSalaryRecordRequest {
    @NotNull
    private Long employeeId;
    @NotNull
    private Integer periodYear;
    @NotNull
    private Integer periodMonth;
    @NotNull
    private BigDecimal baseAmount;
    private BigDecimal bonusAmount;
    private BigDecimal deductionAmount;
    private String notes;
}
