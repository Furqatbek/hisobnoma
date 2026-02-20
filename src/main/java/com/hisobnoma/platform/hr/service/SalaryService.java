package com.hisobnoma.platform.hr.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.hr.dto.CreateSalaryRecordRequest;
import com.hisobnoma.platform.hr.dto.SalaryRecordDto;
import com.hisobnoma.platform.hr.entity.Employee;
import com.hisobnoma.platform.hr.entity.SalaryAdvance;
import com.hisobnoma.platform.hr.entity.SalaryRecord;
import com.hisobnoma.platform.hr.repository.EmployeeRepository;
import com.hisobnoma.platform.hr.repository.SalaryAdvanceRepository;
import com.hisobnoma.platform.hr.repository.SalaryRecordRepository;
import com.hisobnoma.platform.finance.service.GLIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SalaryService {

    private final SalaryRecordRepository salaryRecordRepository;
    private final EmployeeRepository employeeRepository;
    private final SalaryAdvanceRepository advanceRepository;
    private final SecurityContextHelper securityContextHelper;
    private final GLIntegrationService glIntegrationService;

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

        // Calculate advance deduction
        BigDecimal advanceTotal = advanceRepository.sumGivenByEmployeeAndPeriod(
                tenantId, record.getEmployee().getId(), record.getPeriodYear(), record.getPeriodMonth());
        BigDecimal netAmount = record.getNetAmount();
        BigDecimal cashToPay = netAmount.subtract(advanceTotal).max(BigDecimal.ZERO);

        record.setAdvanceAmount(advanceTotal);
        record.setPayAmount(cashToPay);

        // Post salary payment to General Ledger (with advance split)
        try {
            Long journalEntryId = glIntegrationService.postSalaryPayment(
                    record.getId(),
                    record.getEmployee().getFullName(),
                    netAmount,
                    advanceTotal,
                    cashToPay,
                    record.getPeriodYear(),
                    record.getPeriodMonth(),
                    record.getPaidDate()
            );
            record.setGlJournalEntryId(journalEntryId);
            log.info("Salary payment posted to GL: salaryRecordId={}, journalEntryId={}, advance={}, cash={}",
                    id, journalEntryId, advanceTotal, cashToPay);
        } catch (Exception e) {
            log.error("Failed to post salary payment to GL for salaryRecordId={}: {}", id, e.getMessage());
            throw new BusinessException("Salary payment failed: could not record in finance system - " + e.getMessage());
        }

        // Mark all GIVEN advances for this employee/period as DEDUCTED
        List<SalaryAdvance> givenAdvances = advanceRepository
                .findByTenantIdAndEmployeeIdAndPeriodYearAndPeriodMonthAndStatus(
                        tenantId, record.getEmployee().getId(),
                        record.getPeriodYear(), record.getPeriodMonth(),
                        SalaryAdvance.AdvanceStatus.GIVEN);
        for (SalaryAdvance advance : givenAdvances) {
            advance.setStatus(SalaryAdvance.AdvanceStatus.DEDUCTED);
            advance.setSalaryRecordId(record.getId());
        }
        advanceRepository.saveAll(givenAdvances);

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
        BigDecimal advanceAmount = s.getAdvanceAmount();
        BigDecimal payAmount = s.getPayAmount();

        // For pending records, dynamically calculate advance deduction so the user
        // can see how much will be subtracted before they confirm payment
        if (s.getStatus() == SalaryRecord.SalaryStatus.PENDING) {
            advanceAmount = advanceRepository.sumGivenByEmployeeAndPeriod(
                    s.getTenantId(), s.getEmployee().getId(), s.getPeriodYear(), s.getPeriodMonth());
            payAmount = s.getNetAmount().subtract(advanceAmount).max(BigDecimal.ZERO);
        }

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
                .advanceAmount(advanceAmount)
                .payAmount(payAmount)
                .status(s.getStatus().name())
                .paidDate(s.getPaidDate())
                .glJournalEntryId(s.getGlJournalEntryId())
                .notes(s.getNotes())
                .build();
    }
}
