package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.hr.entity.SalaryRecord;
import com.hisobnoma.platform.hr.repository.SalaryAdvanceRepository;
import com.hisobnoma.platform.hr.repository.SalaryRecordRepository;
import com.hisobnoma.platform.reports.dto.SalaryReportDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SalaryReportService {

    private final SecurityContextHelper securityContextHelper;
    private final SalaryRecordRepository salaryRecordRepository;
    private final SalaryAdvanceRepository advanceRepository;

    public SalaryReportDTO generateSalaryReport(Integer year, Integer month) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        log.info("Generating salary report for tenant {} - {}/{}", tenantId, year, month);

        List<SalaryRecord> records = salaryRecordRepository.findByPeriodWithEmployee(tenantId, year, month);

        // Build employee details
        BigDecimal totalBase = BigDecimal.ZERO;
        BigDecimal totalBonus = BigDecimal.ZERO;
        BigDecimal totalDeductions = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalAdvances = BigDecimal.ZERO;
        BigDecimal totalCashPaid = BigDecimal.ZERO;
        int paidCount = 0;
        int pendingCount = 0;
        int cancelledCount = 0;

        List<SalaryReportDTO.EmployeeSalary> employees = new ArrayList<>();
        Map<String, List<SalaryRecord>> byDepartment = new LinkedHashMap<>();

        for (SalaryRecord record : records) {
            BigDecimal base = record.getBaseAmount() != null ? record.getBaseAmount() : BigDecimal.ZERO;
            BigDecimal bonus = record.getBonusAmount() != null ? record.getBonusAmount() : BigDecimal.ZERO;
            BigDecimal deduction = record.getDeductionAmount() != null ? record.getDeductionAmount() : BigDecimal.ZERO;
            BigDecimal net = record.getNetAmount() != null ? record.getNetAmount() : BigDecimal.ZERO;

            totalBase = totalBase.add(base);
            totalBonus = totalBonus.add(bonus);
            totalDeductions = totalDeductions.add(deduction);
            totalNet = totalNet.add(net);

            BigDecimal advance;
            BigDecimal cashPay;

            // For pending records, dynamically calculate advance deduction
            if (record.getStatus() == SalaryRecord.SalaryStatus.PENDING) {
                advance = advanceRepository.sumGivenByEmployeeAndPeriod(
                        tenantId, record.getEmployee().getId(), record.getPeriodYear(), record.getPeriodMonth());
                cashPay = net.subtract(advance).max(BigDecimal.ZERO);
            } else {
                advance = record.getAdvanceAmount() != null ? record.getAdvanceAmount() : BigDecimal.ZERO;
                cashPay = record.getPayAmount() != null ? record.getPayAmount() : BigDecimal.ZERO;
            }
            totalAdvances = totalAdvances.add(advance);
            totalCashPaid = totalCashPaid.add(cashPay);

            switch (record.getStatus()) {
                case PAID -> paidCount++;
                case PENDING -> pendingCount++;
                case CANCELLED -> cancelledCount++;
            }

            String deptName = record.getEmployee().getDepartment() != null
                    ? record.getEmployee().getDepartment().getName()
                    : "Belgilanmagan";
            String posName = record.getEmployee().getPosition() != null
                    ? record.getEmployee().getPosition().getName()
                    : null;

            employees.add(SalaryReportDTO.EmployeeSalary.builder()
                    .employeeId(record.getEmployee().getId())
                    .employeeCode(record.getEmployee().getEmployeeCode())
                    .employeeName(record.getEmployee().getFullName())
                    .departmentName(deptName)
                    .positionName(posName)
                    .baseAmount(base)
                    .bonusAmount(bonus)
                    .deductionAmount(deduction)
                    .netAmount(net)
                    .advanceAmount(advance)
                    .payAmount(cashPay)
                    .status(record.getStatus().name())
                    .paidDate(record.getPaidDate())
                    .glJournalEntryId(record.getGlJournalEntryId())
                    .build());

            byDepartment.computeIfAbsent(deptName, k -> new ArrayList<>()).add(record);
        }

        // Build department breakdown
        List<SalaryReportDTO.DepartmentSummary> departments = new ArrayList<>();
        for (Map.Entry<String, List<SalaryRecord>> entry : byDepartment.entrySet()) {
            BigDecimal deptBase = BigDecimal.ZERO;
            BigDecimal deptNet = BigDecimal.ZERO;
            for (SalaryRecord r : entry.getValue()) {
                deptBase = deptBase.add(r.getBaseAmount() != null ? r.getBaseAmount() : BigDecimal.ZERO);
                deptNet = deptNet.add(r.getNetAmount() != null ? r.getNetAmount() : BigDecimal.ZERO);
            }

            BigDecimal pct = totalNet.compareTo(BigDecimal.ZERO) > 0
                    ? deptNet.multiply(BigDecimal.valueOf(100)).divide(totalNet, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            departments.add(SalaryReportDTO.DepartmentSummary.builder()
                    .departmentName(entry.getKey())
                    .employeeCount(entry.getValue().size())
                    .totalBase(deptBase)
                    .totalNet(deptNet)
                    .percentOfTotal(pct)
                    .build());
        }

        departments.sort((a, b) -> b.getTotalNet().compareTo(a.getTotalNet()));

        return SalaryReportDTO.builder()
                .metadata(SalaryReportDTO.ReportMetadata.builder()
                        .reportName("Ish haqi hisoboti")
                        .generatedAt(Instant.now())
                        .periodYear(year)
                        .periodMonth(month)
                        .build())
                .summary(SalaryReportDTO.Summary.builder()
                        .totalBase(totalBase)
                        .totalBonus(totalBonus)
                        .totalDeductions(totalDeductions)
                        .totalNet(totalNet)
                        .totalAdvances(totalAdvances)
                        .totalCashPaid(totalCashPaid)
                        .totalEmployees(employees.size())
                        .paidCount(paidCount)
                        .pendingCount(pendingCount)
                        .cancelledCount(cancelledCount)
                        .build())
                .employees(employees)
                .byDepartment(departments)
                .build();
    }
}
