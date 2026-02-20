package com.hisobnoma.platform.hr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateSalaryAdvanceRequest {
    @NotNull
    private Long employeeId;
    @NotNull
    @Positive
    private BigDecimal amount;
    private LocalDate advanceDate;
    @NotNull
    private Integer periodYear;
    @NotNull
    private Integer periodMonth;
    private String notes;
}
