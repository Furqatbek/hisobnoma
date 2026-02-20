package com.hisobnoma.platform.hr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateEmployeeRequest {
    @NotBlank
    private String employeeCode;
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    private String middleName;
    private String phone;
    private String email;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    @NotNull
    private LocalDate hireDate;
    private Long departmentId;
    private Long positionId;
    private Long userId;
    private BigDecimal currentSalary;
    private String notes;
}
