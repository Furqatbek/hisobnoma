package com.hisobnoma.platform.hr.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.hr.dto.CreateSalaryRecordRequest;
import com.hisobnoma.platform.hr.dto.SalaryRecordDto;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.entity.SalaryRecord;
import com.hisobnoma.platform.hr.repository.EmployeeRepository;
import com.hisobnoma.platform.hr.repository.SalaryRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SalaryService {

    private final SalaryRecordRepository salaryRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final SecurityContextHelper securityContextHelper;

    @Transactional(readOnly = true)
    public PageResponse<SalaryRecordDto> getAll(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<SalaryRecordDto> page = salaryRecordRepository.findByTenantId(tenantId, pageable).map(this::toDto);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<SalaryRecordDto> getByPeriod(Integer year, Integer month, Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<SalaryRecordDto> page = salaryRecordRepository
                .findByTenantIdAndPeriodYearAndPeriodMonth(tenantId, year, month, pageable)
                .map(this::toDto);
        return PageResponse.from(page);
    }

    @Transactional(readOnly = true)
    public List<SalaryRecordDto> getByEmployee(Long employeeId) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        employeeRepository.findByIdAndTenantId(employeeId, tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", employeeId));
        return salaryRecordRepository
                .findByTenantIdAndEmployeeIdOrderByPeriodYearDescPeriodMonthDesc(tenantId, employeeId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SalaryRecordDto getById(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return toDto(salaryRecordRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("SalaryRecord", id)));
    }

    @Transactional
    public SalaryRecordDto create(CreateSalaryRecordRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        Employee employee = employeeRepository.findByIdAndTenantId(request.getEmployeeId(), tenantId)
                .orElseThrow(() -> new NotFoundException("Employee", request.getEmployeeId()));

        if (salaryRecordRepository.existsByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonth(
                tenantId, request.getEmployeeId(), request.getPeriodYear(), request.getPeriodMonth())) {
            throw new BusinessException("Salary record already exists for this employee and period");
        }

        BigDecimal bonus = request.getBonusAmount() != null ? request.getBonusAmount() : BigDecimal.ZERO;
        BigDecimal deduction = request.getDeductionAmount() != null ? request.getDeductionAmount() : BigDecimal.ZERO;
        BigDecimal net = request.getBaseAmount().add(bonus).subtract(deduction);

        SalaryRecord record = SalaryRecord.builder()
                .tenantId(tenantId)
                .employee(employee)
                .periodYear(request.getPeriodYear())
                .periodMonth(request.getPeriodMonth())
                .baseAmount(request.getBaseAmount())
                .bonusAmount(bonus)
                .deductionAmount(deduction)
                .netAmount(net)
                .status(SalaryRecord.SalaryStatus.PENDING)
                .notes(request.getNotes())
                .build();

        return toDto(salaryRecordRepository.save(record));
    }

    @Transactional
    public SalaryRecordDto markPaid(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        SalaryRecord record = salaryRecordRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("SalaryRecord", id));

        if (record.getStatus() != SalaryRecord.SalaryStatus.PENDING) {
            throw new BusinessException("Only pending salary records can be marked as paid");
        }

        record.setStatus(SalaryRecord.SalaryStatus.PAID);
        record.setPaidDate(LocalDate.now());

        return toDto(salaryRecordRepository.save(record));
    }

    @Transactional
    public SalaryRecordDto cancel(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        SalaryRecord record = salaryRecordRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("SalaryRecord", id));

        if (record.getStatus() == SalaryRecord.SalaryStatus.PAID) {
            throw new BusinessException("Paid salary records cannot be cancelled");
        }

        record.setStatus(SalaryRecord.SalaryStatus.CANCELLED);
        return toDto(salaryRecordRepository.save(record));
    }

    private SalaryRecordDto toDto(SalaryRecord s) {
        return SalaryRecordDto.builder()
                .id(s.getId())
                .employeeId(s.getEmployee().getId())
                .employeeName(s.getEmployee().getFullName())
                .employeeCode(s.getEmployee().getEmployeeCode())
                .periodYear(s.getPeriodYear())
                .periodMonth(s.getPeriodMonth())
                .baseAmount(s.getBaseAmount())
                .bonusAmount(s.getBonusAmount())
                .deductionAmount(s.getDeductionAmount())
                .netAmount(s.getNetAmount())
                .status(s.getStatus().name())
                .paidDate(s.getPaidDate())
                .notes(s.getNotes())
                .build();
    }
}
