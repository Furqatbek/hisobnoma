package com.hisobnoma.platform.hr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositionDto {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Long departmentId;
    private String departmentName;
    private BigDecimal baseSalary;
    private boolean active;
}
