package com.hisobnoma.platform.finance.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.dto.CreateFiscalYearRequest;
import com.hisobnoma.platform.finance.dto.FiscalPeriodDto;
import com.hisobnoma.platform.finance.dto.FiscalYearDto;
import com.hisobnoma.platform.finance.entity.FiscalPeriod;
import com.hisobnoma.platform.finance.entity.FiscalYear;
import com.hisobnoma.platform.finance.entity.PeriodStatus;
import com.hisobnoma.platform.finance.mapper.FiscalPeriodMapper;
import com.hisobnoma.platform.finance.mapper.FiscalYearMapper;
import com.hisobnoma.platform.finance.repository.FiscalPeriodRepository;
import com.hisobnoma.platform.finance.repository.FiscalYearRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FiscalPeriodService {

    private final FiscalYearRepository fiscalYearRepository;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final FiscalYearMapper fiscalYearMapper;
    private final FiscalPeriodMapper fiscalPeriodMapper;
    private final SecurityContextHelper securityContextHelper;

    // ============== Fiscal Year Methods ==============

    @Transactional(readOnly = true)
    public PageResponse<FiscalYearDto> getFiscalYears(Pageable pageable) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Page<FiscalYear> page = fiscalYearRepository.findAllByTenantId(tenantId, pageable);
        return PageResponse.of(page.map(fiscalYearMapper::toDtoWithoutPeriods));
    }

    @Transactional(readOnly = true)
    public List<FiscalYearDto> getAllFiscalYears() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<FiscalYear> years = fiscalYearRepository.findAllByTenantId(tenantId);
        return fiscalYearMapper.toDtoList(years);
    }

    @Transactional(readOnly = true)
    public FiscalYearDto getFiscalYear(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        FiscalYear fiscalYear = fiscalYearRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Fiscal year not found with id: " + id));
        return fiscalYearMapper.toDto(fiscalYear);
    }

    @Transactional(readOnly = true)
    public FiscalYearDto getFiscalYearWithPeriods(Long id) {
        FiscalYear fiscalYear = fiscalYearRepository.findByIdWithPeriods(id)
                .orElseThrow(() -> new NotFoundException("Fiscal year not found with id: " + id));
        return fiscalYearMapper.toDto(fiscalYear);
    }

    @Transactional(readOnly = true)
    public FiscalYearDto getCurrentFiscalYear() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        FiscalYear fiscalYear = fiscalYearRepository.findCurrentByTenantId(tenantId)
                .orElseThrow(() -> new NotFoundException("No current fiscal year found"));
        return fiscalYearMapper.toDto(fiscalYear);
    }

    @Transactional(readOnly = true)
    public FiscalYearDto getFiscalYearByDate(LocalDate date) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        FiscalYear fiscalYear = fiscalYearRepository.findByDateAndTenantId(date, tenantId)
                .orElseThrow(() -> new NotFoundException("No fiscal year found for date: " + date));
        return fiscalYearMapper.toDto(fiscalYear);
    }

    @Transactional
    public FiscalYearDto createFiscalYear(CreateFiscalYearRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Check for duplicate year
        if (fiscalYearRepository.existsByYearAndTenantId(request.getYear(), tenantId)) {
            throw new BusinessException("Fiscal year " + request.getYear() + " already exists");
        }

        // Validate dates
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException("End date must be after start date");
        }

        FiscalYear fiscalYear = fiscalYearMapper.toEntity(request);
        fiscalYear.setTenantId(tenantId);
        fiscalYear.setStatus(PeriodStatus.OPEN);

        // If this is set as current, unset any existing current year
        if (request.isCurrent()) {
            fiscalYearRepository.findCurrentByTenantId(tenantId)
                    .ifPresent(current -> {
                        current.setCurrent(false);
                        fiscalYearRepository.save(current);
                    });
        }

        fiscalYear = fiscalYearRepository.save(fiscalYear);

        // Generate periods if requested
        if (request.isGeneratePeriods()) {
            generatePeriodsForYear(fiscalYear);
        }

        return fiscalYearMapper.toDto(fiscalYear);
    }

    @Transactional
    public FiscalYearDto updateFiscalYear(Long id, CreateFiscalYearRequest request) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        FiscalYear fiscalYear = fiscalYearRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Fiscal year not found with id: " + id));

        if (fiscalYear.isClosed()) {
            throw new BusinessException("Cannot modify a closed fiscal year");
        }

        fiscalYearMapper.updateEntity(request, fiscalYear);
        fiscalYear = fiscalYearRepository.save(fiscalYear);
        return fiscalYearMapper.toDto(fiscalYear);
    }

    @Transactional
    public FiscalYearDto setCurrentFiscalYear(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();

        // Unset any existing current year
        fiscalYearRepository.findCurrentByTenantId(tenantId)
                .ifPresent(current -> {
                    current.setCurrent(false);
                    fiscalYearRepository.save(current);
                });

        // Set the new current year
        FiscalYear fiscalYear = fiscalYearRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Fiscal year not found with id: " + id));

        fiscalYear.setCurrent(true);
        fiscalYear = fiscalYearRepository.save(fiscalYear);
        return fiscalYearMapper.toDto(fiscalYear);
    }

    private void generatePeriodsForYear(FiscalYear fiscalYear) {
        LocalDate periodStart = fiscalYear.getStartDate();
        int periodNumber = 1;

        while (!periodStart.isAfter(fiscalYear.getEndDate())) {
            LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
            if (periodEnd.isAfter(fiscalYear.getEndDate())) {
                periodEnd = fiscalYear.getEndDate();
            }

            String monthName = periodStart.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH);

            FiscalPeriod period = FiscalPeriod.builder()
                    .fiscalYear(fiscalYear)
                    .periodNumber(periodNumber)
                    .name(monthName)
                    .startDate(periodStart)
                    .endDate(periodEnd)
                    .status(PeriodStatus.OPEN)
                    .tenantId(fiscalYear.getTenantId())
                    .build();

            fiscalYear.addPeriod(period);
            periodStart = periodStart.plusMonths(1);
            periodNumber++;
        }

        fiscalYearRepository.save(fiscalYear);
    }

    // ============== Fiscal Period Methods ==============

    @Transactional(readOnly = true)
    public List<FiscalPeriodDto> getPeriodsByFiscalYear(Long fiscalYearId) {
        List<FiscalPeriod> periods = fiscalPeriodRepository.findByFiscalYearId(fiscalYearId);
        return fiscalPeriodMapper.toDtoList(periods);
    }

    @Transactional(readOnly = true)
    public FiscalPeriodDto getPeriod(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        FiscalPeriod period = fiscalPeriodRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Fiscal period not found with id: " + id));
        return fiscalPeriodMapper.toDto(period);
    }

    @Transactional(readOnly = true)
    public FiscalPeriodDto getPeriodByDate(LocalDate date) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        FiscalPeriod period = fiscalPeriodRepository.findByDateAndTenantId(date, tenantId)
                .orElseThrow(() -> new NotFoundException("No fiscal period found for date: " + date));
        return fiscalPeriodMapper.toDto(period);
    }

    @Transactional(readOnly = true)
    public List<FiscalPeriodDto> getOpenPeriods() {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        List<FiscalPeriod> periods = fiscalPeriodRepository.findOpenPeriodsByTenantId(tenantId);
        return fiscalPeriodMapper.toDtoList(periods);
    }

    @Transactional
    public FiscalPeriodDto closePeriod(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        FiscalPeriod period = fiscalPeriodRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Fiscal period not found with id: " + id));

        if (period.getStatus() != PeriodStatus.OPEN) {
            throw new BusinessException("Period is not open");
        }

        period.setStatus(PeriodStatus.CLOSED);
        period.setClosedAt(LocalDateTime.now());
        period.setClosedBy(userId);

        period = fiscalPeriodRepository.save(period);
        return fiscalPeriodMapper.toDto(period);
    }

    @Transactional
    public FiscalPeriodDto reopenPeriod(Long id, String reason) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        FiscalPeriod period = fiscalPeriodRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Fiscal period not found with id: " + id));

        if (period.getStatus() == PeriodStatus.LOCKED) {
            throw new BusinessException("Period is locked and cannot be reopened");
        }

        if (period.getStatus() != PeriodStatus.CLOSED) {
            throw new BusinessException("Period is not closed");
        }

        // Check if fiscal year is closed
        if (period.getFiscalYear().isClosed()) {
            throw new BusinessException("Cannot reopen period in a closed fiscal year");
        }

        period.setStatus(PeriodStatus.OPEN);
        period.setReopenedAt(LocalDateTime.now());
        period.setReopenedBy(userId);
        period.setReopenReason(reason);

        period = fiscalPeriodRepository.save(period);
        return fiscalPeriodMapper.toDto(period);
    }

    @Transactional
    public FiscalYearDto closeFiscalYear(Long id) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        Long userId = securityContextHelper.getCurrentUserId();

        FiscalYear fiscalYear = fiscalYearRepository.findByIdWithPeriods(id)
                .orElseThrow(() -> new NotFoundException("Fiscal year not found with id: " + id));

        if (fiscalYear.isClosed()) {
            throw new BusinessException("Fiscal year is already closed");
        }

        // Check if all periods are closed
        if (!fiscalYear.areAllPeriodsClosed()) {
            throw new BusinessException("All periods must be closed before closing the fiscal year");
        }

        fiscalYear.setStatus(PeriodStatus.CLOSED);
        fiscalYear.setClosed(true);
        fiscalYear.setClosedAt(LocalDateTime.now());
        fiscalYear.setClosedBy(userId);

        // Lock all periods
        for (FiscalPeriod period : fiscalYear.getPeriods()) {
            period.setStatus(PeriodStatus.LOCKED);
        }

        fiscalYear = fiscalYearRepository.save(fiscalYear);
        return fiscalYearMapper.toDto(fiscalYear);
    }

    /**
     * Gets the fiscal period entity for a given date.
     */
    @Transactional(readOnly = true)
    public FiscalPeriod getPeriodEntityByDate(LocalDate date) {
        Long tenantId = securityContextHelper.getCurrentTenantId();
        return fiscalPeriodRepository.findByDateAndTenantId(date, tenantId)
                .orElseThrow(() -> new NotFoundException("No fiscal period found for date: " + date));
    }

    /**
     * Validates that posting is allowed for a given date.
     */
    @Transactional(readOnly = true)
    public void validatePostingAllowed(LocalDate date) {
        FiscalPeriod period = getPeriodEntityByDate(date);
        if (!period.allowsPosting()) {
            throw new BusinessException("Posting is not allowed for period: " + period.getDisplayName() + ". Period status: " + period.getStatus());
        }
    }
}
