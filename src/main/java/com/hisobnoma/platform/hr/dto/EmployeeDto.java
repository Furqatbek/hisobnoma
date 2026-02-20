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
public class EmployeeDto {
    private Long id;
    private String employeeCode;
    private String firstName;
    private String lastName;
    private String middleName;
    private String fullName;
    private String phone;
    private String email;
    private LocalDate dateOfBirth;
    private String gender;
    private String address;
    private LocalDate hireDate;
    private LocalDate terminationDate;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private Long userId;
    private String username;
    private BigDecimal currentSalary;
    private String status;
    private String notes;
    private boolean active;
}
