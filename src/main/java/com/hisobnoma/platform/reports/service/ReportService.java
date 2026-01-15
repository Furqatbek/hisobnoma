package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.reports.dto.ReportDefinitionDTO;
import com.hisobnoma.platform.reports.dto.ReportExecutionDTO;
import com.hisobnoma.platform.reports.dto.ReportScheduleDTO;
import com.hisobnoma.platform.reports.entity.ReportDefinition;
import com.hisobnoma.platform.reports.entity.ReportExecution;
import com.hisobnoma.platform.reports.entity.ReportSchedule;
import com.hisobnoma.platform.reports.mapper.ReportDefinitionMapper;
import com.hisobnoma.platform.reports.mapper.ReportExecutionMapper;
import com.hisobnoma.platform.reports.mapper.ReportScheduleMapper;
import com.hisobnoma.platform.reports.repository.ReportDefinitionRepository;
import com.hisobnoma.platform.reports.repository.ReportExecutionRepository;
import com.hisobnoma.platform.reports.repository.ReportScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

/**
 * Service for managing report definitions, schedules, and executions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final ReportDefinitionRepository definitionRepository;
    private final ReportScheduleRepository scheduleRepository;
    private final ReportExecutionRepository executionRepository;
    private final ReportDefinitionMapper definitionMapper;
    private final ReportScheduleMapper scheduleMapper;
    private final ReportExecutionMapper executionMapper;
    private final SecurityContextHelper securityContextHelper;

    // =====================================================
    // Report Definition Operations
    // =====================================================

    /**
     * Get all available reports for the current tenant (paginated).
     */
    public Page<ReportDefinitionDTO> getReportDefinitions(Pageable pageable) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return definitionRepository.findAllByTenantId(tenantId, pageable)
                .map(definitionMapper::toDto);
    }

    /**
     * Get report definition by ID.
     */
    public ReportDefinitionDTO getReportDefinition(Long id) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        ReportDefinition report = definitionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Report definition not found: " + id));
        return definitionMapper.toDto(report);
    }

    /**
     * Get report definition by code.
     */
    public ReportDefinitionDTO getReportDefinitionByCode(String code) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        ReportDefinition report = definitionRepository.findByCodeAndTenantId(code, tenantId)
                .orElseThrow(() -> new NotFoundException("Report not found: " + code));
        return definitionMapper.toDto(report);
    }

    /**
     * Get all available reports for the current tenant.
     */
    public List<ReportDefinitionDTO> getAvailableReports() {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return definitionMapper.toDtoList(definitionRepository.findAllActiveByTenantId(tenantId));
    }

    /**
     * Get reports by category (paginated).
     */
    public Page<ReportDefinitionDTO> getReportsByCategory(ReportDefinition.ReportCategory category, Pageable pageable) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return definitionRepository.findByCategoryAndTenantIdPaged(category, tenantId, pageable)
                .map(definitionMapper::toDto);
    }

    /**
     * Get reports by category.
     */
    public List<ReportDefinitionDTO> getReportsByCategory(ReportDefinition.ReportCategory category) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return definitionMapper.toDtoList(definitionRepository.findByCategoryAndTenantId(category, tenantId));
    }

    /**
     * Get report by code.
     */
    public ReportDefinitionDTO getReportByCode(String code) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        ReportDefinition report = definitionRepository.findByCodeAndTenantId(code, tenantId)
                .orElseThrow(() -> new NotFoundException("Report not found: " + code));
        return definitionMapper.toDto(report);
    }

    /**
     * Create a custom report definition.
     */
    @Transactional
    public ReportDefinitionDTO createReport(ReportDefinitionDTO dto) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        if (definitionRepository.existsByCodeAndTenantId(dto.getCode(), tenantId)) {
            throw new ValidationException("Report with code " + dto.getCode() + " already exists");
        }

        ReportDefinition report = definitionMapper.toEntity(dto);
        report.setTenantId(tenantId);
        report.setSystemReport(false);

        report = definitionRepository.save(report);
        log.info("Created report definition: {} for tenant: {}", report.getCode(), tenantId);
        return definitionMapper.toDto(report);
    }

    /**
     * Update a custom report definition.
     */
    @Transactional
    public ReportDefinitionDTO updateReport(String code, ReportDefinitionDTO dto) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        ReportDefinition report = definitionRepository.findByCodeAndTenantId(code, tenantId)
                .orElseThrow(() -> new NotFoundException("Report not found: " + code));

        if (report.isSystemReport()) {
            throw new ValidationException("Cannot modify system reports");
        }

        definitionMapper.updateEntity(dto, report);
        report = definitionRepository.save(report);
        return definitionMapper.toDto(report);
    }

    // =====================================================
    // Report Schedule Operations
    // =====================================================

    /**
     * Get all schedules for the current tenant.
     */
    public Page<ReportScheduleDTO> getSchedules(Pageable pageable) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return scheduleRepository.findByTenantId(tenantId, pageable)
                .map(scheduleMapper::toDto);
    }

    /**
     * Get schedule by ID.
     */
    public ReportScheduleDTO getSchedule(Long id) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        ReportSchedule schedule = scheduleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Schedule not found: " + id));
        return scheduleMapper.toDto(schedule);
    }

    /**
     * Create a report schedule.
     */
    @Transactional
    public ReportScheduleDTO createSchedule(String reportCode, ReportScheduleDTO dto) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();

        ReportDefinition report = definitionRepository.findByCodeAndTenantId(reportCode, tenantId)
                .orElseThrow(() -> new NotFoundException("Report not found: " + reportCode));

        ReportSchedule schedule = scheduleMapper.toEntity(dto);
        schedule.setTenantId(tenantId);
        schedule.setReportDefinition(report);
        schedule.setCreatedByUserId(userId);
        schedule.setNextRunAt(calculateNextRunTime(schedule));

        schedule = scheduleRepository.save(schedule);
        log.info("Created schedule {} for report {} in tenant {}", schedule.getId(), reportCode, tenantId);
        return scheduleMapper.toDto(schedule);
    }

    /**
     * Create a report schedule (using reportDefinitionId from DTO).
     */
    @Transactional
    public ReportScheduleDTO createSchedule(ReportScheduleDTO dto) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        Long userId = securityContextHelper.getRequiredCurrentUser().getId();

        if (dto.getReportDefinitionId() == null && dto.getReportCode() == null) {
            throw new ValidationException("Either reportDefinitionId or reportCode is required");
        }

        ReportDefinition report;
        if (dto.getReportDefinitionId() != null) {
            report = definitionRepository.findByIdAndTenantId(dto.getReportDefinitionId(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Report not found: " + dto.getReportDefinitionId()));
        } else {
            report = definitionRepository.findByCodeAndTenantId(dto.getReportCode(), tenantId)
                    .orElseThrow(() -> new NotFoundException("Report not found: " + dto.getReportCode()));
        }

        ReportSchedule schedule = scheduleMapper.toEntity(dto);
        schedule.setTenantId(tenantId);
        schedule.setReportDefinition(report);
        schedule.setCreatedByUserId(userId);
        schedule.setNextRunAt(calculateNextRunTime(schedule));

        schedule = scheduleRepository.save(schedule);
        log.info("Created schedule {} for report {} in tenant {}", schedule.getId(), report.getCode(), tenantId);
        return scheduleMapper.toDto(schedule);
    }

    /**
     * Update a report schedule.
     */
    @Transactional
    public ReportScheduleDTO updateSchedule(Long id, ReportScheduleDTO dto) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        ReportSchedule schedule = scheduleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Schedule not found: " + id));

        scheduleMapper.updateEntity(dto, schedule);
        schedule.setNextRunAt(calculateNextRunTime(schedule));

        schedule = scheduleRepository.save(schedule);
        return scheduleMapper.toDto(schedule);
    }

    /**
     * Delete a schedule.
     */
    @Transactional
    public void deleteSchedule(Long id) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        ReportSchedule schedule = scheduleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Schedule not found: " + id));

        scheduleRepository.delete(schedule);
        log.info("Deleted schedule {} in tenant {}", id, tenantId);
    }

    /**
     * Enable or disable a schedule.
     */
    @Transactional
    public ReportScheduleDTO toggleSchedule(Long id, boolean active) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        ReportSchedule schedule = scheduleRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Schedule not found: " + id));

        schedule.setActive(active);
        if (active) {
            schedule.setNextRunAt(calculateNextRunTime(schedule));
        }

        schedule = scheduleRepository.save(schedule);
        log.info("{} schedule {} in tenant {}", active ? "Enabled" : "Disabled", id, tenantId);
        return scheduleMapper.toDto(schedule);
    }

    // =====================================================
    // Report Execution Operations
    // =====================================================

    /**
     * Get all executions (paginated).
     */
    public Page<ReportExecutionDTO> getExecutions(Pageable pageable) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return executionRepository.findByTenantIdOrderByStartedAtDesc(tenantId, pageable)
                .map(executionMapper::toDto);
    }

    /**
     * Get execution history.
     */
    public Page<ReportExecutionDTO> getExecutionHistory(Pageable pageable) {
        return getExecutions(pageable);
    }

    /**
     * Get execution by ID.
     */
    public ReportExecutionDTO getExecution(Long id) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        ReportExecution execution = executionRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("Execution not found: " + id));
        return executionMapper.toDto(execution);
    }

    /**
     * Get executions by report ID.
     */
    public Page<ReportExecutionDTO> getExecutionsByReport(Long reportId, Pageable pageable) {
        Long tenantId = securityContextHelper.getRequiredTenantId();
        return executionRepository.findByReportDefinitionIdAndTenantId(reportId, tenantId, pageable)
                .map(executionMapper::toDto);
    }

    /**
     * Get execution history for a specific report.
     */
    public Page<ReportExecutionDTO> getReportExecutionHistory(String reportCode, Pageable pageable) {
        Long tenantId = securityContextHelper.getRequiredTenantId();

        ReportDefinition report = definitionRepository.findByCodeAndTenantId(reportCode, tenantId)
                .orElseThrow(() -> new NotFoundException("Report not found: " + reportCode));

        return executionRepository.findByReportDefinitionIdAndTenantId(report.getId(), tenantId, pageable)
                .map(executionMapper::toDto);
    }

    // =====================================================
    // Helper Methods
    // =====================================================

    private Instant calculateNextRunTime(ReportSchedule schedule) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        LocalTime runTime = schedule.getRunTime() != null ? schedule.getRunTime() : LocalTime.of(6, 0);

        ZonedDateTime nextRun;

        switch (schedule.getFrequency()) {
            case DAILY:
                nextRun = now.with(runTime);
                if (nextRun.isBefore(now) || nextRun.equals(now)) {
                    nextRun = nextRun.plusDays(1);
                }
                break;

            case WEEKLY:
                int dayOfWeek = schedule.getDayOfWeek() != null ? schedule.getDayOfWeek() : 1;
                nextRun = now.with(TemporalAdjusters.nextOrSame(java.time.DayOfWeek.of(dayOfWeek))).with(runTime);
                if (nextRun.isBefore(now) || nextRun.equals(now)) {
                    nextRun = nextRun.plusWeeks(1);
                }
                break;

            case MONTHLY:
                int dayOfMonth = schedule.getDayOfMonth() != null ? schedule.getDayOfMonth() : 1;
                nextRun = now.withDayOfMonth(Math.min(dayOfMonth, now.toLocalDate().lengthOfMonth())).with(runTime);
                if (nextRun.isBefore(now) || nextRun.equals(now)) {
                    nextRun = nextRun.plusMonths(1).withDayOfMonth(Math.min(dayOfMonth, nextRun.plusMonths(1).toLocalDate().lengthOfMonth()));
                }
                break;

            case QUARTERLY:
                nextRun = now.with(TemporalAdjusters.firstDayOfNextMonth()).with(runTime);
                while (nextRun.getMonthValue() % 3 != 1) {
                    nextRun = nextRun.plusMonths(1);
                }
                break;

            case YEARLY:
                nextRun = now.withMonth(1).withDayOfMonth(1).with(runTime);
                if (nextRun.isBefore(now) || nextRun.equals(now)) {
                    nextRun = nextRun.plusYears(1);
                }
                break;

            default:
                nextRun = now.plusDays(1).with(runTime);
        }

        return nextRun.toInstant();
    }
}
