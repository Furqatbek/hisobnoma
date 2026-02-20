package com.hisobnoma.platform.reports.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalaryReportDTO {
    private ReportMetadata metadata;
    private Summary summary;
    private List<EmployeeSalary> employees;
    private List<DepartmentSummary> byDepartment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportMetadata {
        private String reportName;
        private Instant generatedAt;
        private Integer periodYear;
        private Integer periodMonth;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        private BigDecimal totalBase;
        private BigDecimal totalBonus;
        private BigDecimal totalDeductions;
        private BigDecimal totalNet;
        private int totalEmployees;
        private int paidCount;
        private int pendingCount;
        private int cancelledCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmployeeSalary {
        private Long employeeId;
        private String employeeCode;
        private String employeeName;
        private String departmentName;
        private String positionName;
        private BigDecimal baseAmount;
        private BigDecimal bonusAmount;
        private BigDecimal deductionAmount;
        private BigDecimal netAmount;
        private String status;
        private LocalDate paidDate;
        private Long glJournalEntryId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentSummary {
        private String departmentName;
        private int employeeCount;
        private BigDecimal totalBase;
        private BigDecimal totalNet;
        private BigDecimal percentOfTotal;
    }
}
