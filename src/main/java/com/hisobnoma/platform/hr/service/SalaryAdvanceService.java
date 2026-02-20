package com.hisobnoma.platform.hr.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.service.GLIntegrationService;
import com.hisobnoma.platform.hr.dto.CreateSalaryAdvanceRequest;
import com.hisobnoma.platform.hr.dto.SalaryAdvanceDto;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.entity.SalaryAdvance;
import com.hisobnoma.platform.hr.repository.EmployeeRepository;
import com.hisobnoma.platform.hr.repository.SalaryAdvanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryAdvanceService {

    private final SalaryAdvanceRepository advanceRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityContextHelper securityContextHelper;
    private final GLIntegrationService glIntegrationService;

    @Transactional(readOnly = true)
    public List<SalaryAdvanceDto> getByPeriod(Integer year, Integer month) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return advanceRepository.findByTenantIdAndPeriodYearAndPeriodMonthOrderByAdvanceDateDesc(tenantId, year, month)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SalaryAdvanceDto> getByEmployee(Long employeeId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return advanceRepository.findByTenantIdAndEmployeeIdOrderByAdvanceDateDesc(tenantId, employeeId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BigDecimal getUndeductedTotal(Long employeeId, Integer year, Integer month) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return advanceRepository.sumGivenByEmployeeAndPeriod(tenantId, employeeId, year, month);
    }

    @Transactional
    public SalaryAdvanceDto create(CreateSalaryAdvanceRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Employee employee = employeeRepository.findByIdAndTenantId(request.getEmployeeId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", request.getEmployeeId()));

        LocalDate advanceDate = request.getAdvanceDate() != null ? request.getAdvanceDate() : LocalDate.now();

        SalaryAdvance advance = SalaryAdvance.builder()
                .tenantId(tenantId)
                .employee(employee)
                .amount(request.getAmount())
                .advanceDate(advanceDate)
                .periodYear(request.getPeriodYear())
                .periodMonth(request.getPeriodMonth())
                .status(SalaryAdvance.AdvanceStatus.GIVEN)
                .notes(request.getNotes())
                .build();

        advance = advanceRepository.save(advance);

        // Post advance to GL: DR Salary Advance (asset), CR Cash
        try {
            Long journalEntryId = glIntegrationService.postSalaryAdvance(
                    advance.getId(),
                    employee.getFullName(),
                    request.getAmount(),
                    request.getPeriodYear(),
                    request.getPeriodMonth(),
                    advanceDate
            );
            advance.setGlJournalEntryId(journalEntryId);
            advance = advanceRepository.save(advance);
            log.info("Salary advance posted to GL: advanceId={}, journalEntryId={}", advance.getId(), journalEntryId);
        } catch (Exception e) {
            log.error("Failed to post salary advance to GL: {}", e.getMessage());
            throw new BusinessException("Avans berish muvaffaqiyatsiz: moliyaviy tizimda qayd etilmadi - " + e.getMessage());
        }

        return toDto(advance);
    }

    @Transactional
    public SalaryAdvanceDto cancel(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        SalaryAdvance advance = advanceRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("SalaryAdvance", id));

        if (advance.getStatus() != SalaryAdvance.AdvanceStatus.GIVEN) {
            throw new BusinessException("Faqat berilgan avanslarni bekor qilish mumkin");
        }

        advance.setStatus(SalaryAdvance.AdvanceStatus.CANCELLED);
        return toDto(advanceRepository.save(advance));
    }

    private SalaryAdvanceDto toDto(SalaryAdvance a) {
        return SalaryAdvanceDto.builder()
                .id(a.getId())
                .employeeId(a.getEmployee().getId())
                .employeeName(a.getEmployee().getFullName())
                .employeeCode(a.getEmployee().getEmployeeCode())
                .amount(a.getAmount())
                .advanceDate(a.getAdvanceDate())
                .periodYear(a.getPeriodYear())
                .periodMonth(a.getPeriodMonth())
                .status(a.getStatus().name())
                .salaryRecordId(a.getSalaryRecordId())
                .glJournalEntryId(a.getGlJournalEntryId())
                .notes(a.getNotes())
                .build();
    }
}
