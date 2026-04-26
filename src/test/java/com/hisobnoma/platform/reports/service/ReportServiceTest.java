package com.hisobnoma.platform.reports.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.auth.security.UserPrincipal;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportDefinitionRepository definitionRepository;

    @Mock
    private ReportScheduleRepository scheduleRepository;

    @Mock
    private ReportExecutionRepository executionRepository;

    @Mock
    private ReportDefinitionMapper definitionMapper;

    @Mock
    private ReportScheduleMapper scheduleMapper;

    @Mock
    private ReportExecutionMapper executionMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ReportService reportService;

    private ReportDefinition reportDefinition;
    private ReportDefinitionDTO reportDefinitionDTO;
    private ReportSchedule reportSchedule;
    private ReportScheduleDTO reportScheduleDTO;
    private ReportExecution reportExecution;
    private ReportExecutionDTO reportExecutionDTO;
    private UserPrincipal userPrincipal;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        userPrincipal = new UserPrincipal(10L, "admin", "pass", TENANT_ID, true, true, List.of());

        reportDefinition = ReportDefinition.builder()
                .id(1L)
                .code("SALES_SUMMARY")
                .name("Sales Summary Report")
                .category(ReportDefinition.ReportCategory.SALES)
                .systemReport(false)
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        reportDefinitionDTO = ReportDefinitionDTO.builder()
                .id(1L)
                .code("SALES_SUMMARY")
                .name("Sales Summary Report")
                .category(ReportDefinition.ReportCategory.SALES)
                .systemReport(false)
                .active(true)
                .build();

        reportSchedule = ReportSchedule.builder()
                .id(1L)
                .name("Daily Sales")
                .frequency(ReportSchedule.ScheduleFrequency.DAILY)
                .runTime(LocalTime.of(6, 0))
                .exportFormat(ReportDefinition.ExportFormat.EXCEL)
                .active(true)
                .tenantId(TENANT_ID)
                .reportDefinition(reportDefinition)
                .build();

        reportScheduleDTO = ReportScheduleDTO.builder()
                .id(1L)
                .name("Daily Sales")
                .frequency(ReportSchedule.ScheduleFrequency.DAILY)
                .runTime(LocalTime.of(6, 0))
                .exportFormat(ReportDefinition.ExportFormat.EXCEL)
                .active(true)
                .build();

        reportExecution = ReportExecution.builder()
                .id(1L)
                .reportDefinition(reportDefinition)
                .status(ReportExecution.ExecutionStatus.COMPLETED)
                .source(ReportExecution.ExecutionSource.MANUAL)
                .tenantId(TENANT_ID)
                .build();

        reportExecutionDTO = ReportExecutionDTO.builder()
                .id(1L)
                .reportDefinitionId(1L)
                .status(ReportExecution.ExecutionStatus.COMPLETED)
                .source(ReportExecution.ExecutionSource.MANUAL)
                .build();
    }

    // ====== getReportDefinitions ======

    @Test
    void getReportDefinitions_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReportDefinition> page = new PageImpl<>(List.of(reportDefinition), pageable, 1);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(definitionMapper.toDto(reportDefinition)).thenReturn(reportDefinitionDTO);

        // When
        var result = reportService.getReportDefinitions(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("SALES_SUMMARY", result.getContent().get(0).getCode());
        verify(definitionRepository).findAllByTenantId(TENANT_ID, pageable);
    }

    // ====== getReportDefinition ======

    @Test
    void getReportDefinition_found_returnsDto() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reportDefinition));
        when(definitionMapper.toDto(reportDefinition)).thenReturn(reportDefinitionDTO);

        // When
        ReportDefinitionDTO result = reportService.getReportDefinition(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("SALES_SUMMARY", result.getCode());
    }

    @Test
    void getReportDefinition_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> reportService.getReportDefinition(999L));
    }

    // ====== getReportDefinitionByCode ======

    @Test
    void getReportDefinitionByCode_found_returnsDto() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findByCodeAndTenantId("SALES_SUMMARY", TENANT_ID)).thenReturn(Optional.of(reportDefinition));
        when(definitionMapper.toDto(reportDefinition)).thenReturn(reportDefinitionDTO);

        // When
        ReportDefinitionDTO result = reportService.getReportDefinitionByCode("SALES_SUMMARY");

        // Then
        assertNotNull(result);
        assertEquals("SALES_SUMMARY", result.getCode());
    }

    @Test
    void getReportDefinitionByCode_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findByCodeAndTenantId("UNKNOWN", TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> reportService.getReportDefinitionByCode("UNKNOWN"));
    }

    // ====== getAvailableReports ======

    @Test
    void getAvailableReports_returnsList() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(reportDefinition));
        when(definitionMapper.toDtoList(List.of(reportDefinition))).thenReturn(List.of(reportDefinitionDTO));

        // When
        List<ReportDefinitionDTO> result = reportService.getAvailableReports();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    // ====== getReportsByCategory (paginated) ======

    @Test
    void getReportsByCategory_paged_returnsFilteredReports() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReportDefinition> page = new PageImpl<>(List.of(reportDefinition), pageable, 1);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findByCategoryAndTenantIdPaged(ReportDefinition.ReportCategory.SALES, TENANT_ID, pageable))
                .thenReturn(page);
        when(definitionMapper.toDto(reportDefinition)).thenReturn(reportDefinitionDTO);

        // When
        var result = reportService.getReportsByCategory(ReportDefinition.ReportCategory.SALES, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(ReportDefinition.ReportCategory.SALES, result.getContent().get(0).getCategory());
    }

    // ====== getReportsByCategory (list) ======

    @Test
    void getReportsByCategory_returnsList() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findByCategoryAndTenantId(ReportDefinition.ReportCategory.SALES, TENANT_ID))
                .thenReturn(List.of(reportDefinition));
        when(definitionMapper.toDtoList(List.of(reportDefinition))).thenReturn(List.of(reportDefinitionDTO));

        // When
        List<ReportDefinitionDTO> result = reportService.getReportsByCategory(ReportDefinition.ReportCategory.SALES);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ====== createReport ======

    @Test
    void createReport_success() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.existsByCodeAndTenantId("SALES_SUMMARY", TENANT_ID)).thenReturn(false);
        when(definitionMapper.toEntity(reportDefinitionDTO)).thenReturn(reportDefinition);
        when(definitionRepository.save(any(ReportDefinition.class))).thenReturn(reportDefinition);
        when(definitionMapper.toDto(reportDefinition)).thenReturn(reportDefinitionDTO);

        // When
        ReportDefinitionDTO result = reportService.createReport(reportDefinitionDTO);

        // Then
        assertNotNull(result);
        assertEquals("SALES_SUMMARY", result.getCode());
        verify(definitionRepository).save(any(ReportDefinition.class));
    }

    @Test
    void createReport_duplicateCode_throwsValidationException() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.existsByCodeAndTenantId("SALES_SUMMARY", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(ValidationException.class, () -> reportService.createReport(reportDefinitionDTO));
        verify(definitionRepository, never()).save(any());
    }

    // ====== updateReport ======

    @Test
    void updateReport_success() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findByCodeAndTenantId("SALES_SUMMARY", TENANT_ID)).thenReturn(Optional.of(reportDefinition));
        when(definitionRepository.save(any(ReportDefinition.class))).thenReturn(reportDefinition);
        when(definitionMapper.toDto(any(ReportDefinition.class))).thenReturn(reportDefinitionDTO);

        // When
        ReportDefinitionDTO result = reportService.updateReport("SALES_SUMMARY", reportDefinitionDTO);

        // Then
        assertNotNull(result);
        verify(definitionMapper).updateEntity(eq(reportDefinitionDTO), any(ReportDefinition.class));
        verify(definitionRepository).save(any(ReportDefinition.class));
    }

    @Test
    void updateReport_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findByCodeAndTenantId("UNKNOWN", TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> reportService.updateReport("UNKNOWN", reportDefinitionDTO));
    }

    @Test
    void updateReport_systemReport_throwsValidationException() {
        // Given
        reportDefinition.setSystemReport(true);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findByCodeAndTenantId("SALES_SUMMARY", TENANT_ID)).thenReturn(Optional.of(reportDefinition));

        // When/Then
        assertThrows(ValidationException.class, () -> reportService.updateReport("SALES_SUMMARY", reportDefinitionDTO));
        verify(definitionRepository, never()).save(any());
    }

    // ====== getSchedules ======

    @Test
    void getSchedules_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReportSchedule> page = new PageImpl<>(List.of(reportSchedule), pageable, 1);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(scheduleRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(scheduleMapper.toDto(reportSchedule)).thenReturn(reportScheduleDTO);

        // When
        var result = reportService.getSchedules(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ====== getSchedule ======

    @Test
    void getSchedule_found_returnsDto() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(scheduleRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reportSchedule));
        when(scheduleMapper.toDto(reportSchedule)).thenReturn(reportScheduleDTO);

        // When
        ReportScheduleDTO result = reportService.getSchedule(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getSchedule_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(scheduleRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> reportService.getSchedule(999L));
    }

    // ====== createSchedule (with reportCode) ======

    @Test
    void createSchedule_withReportCode_success() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(definitionRepository.findByCodeAndTenantId("SALES_SUMMARY", TENANT_ID)).thenReturn(Optional.of(reportDefinition));
        when(scheduleMapper.toEntity(reportScheduleDTO)).thenReturn(reportSchedule);
        when(scheduleRepository.save(any(ReportSchedule.class))).thenReturn(reportSchedule);
        when(scheduleMapper.toDto(reportSchedule)).thenReturn(reportScheduleDTO);

        // When
        ReportScheduleDTO result = reportService.createSchedule("SALES_SUMMARY", reportScheduleDTO);

        // Then
        assertNotNull(result);
        verify(scheduleRepository).save(any(ReportSchedule.class));
    }

    @Test
    void createSchedule_reportCodeNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(definitionRepository.findByCodeAndTenantId("UNKNOWN", TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> reportService.createSchedule("UNKNOWN", reportScheduleDTO));
    }

    // ====== createSchedule (with DTO) ======

    @Test
    void createSchedule_withReportDefinitionId_success() {
        // Given
        reportScheduleDTO.setReportDefinitionId(1L);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);
        when(definitionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reportDefinition));
        when(scheduleMapper.toEntity(reportScheduleDTO)).thenReturn(reportSchedule);
        when(scheduleRepository.save(any(ReportSchedule.class))).thenReturn(reportSchedule);
        when(scheduleMapper.toDto(reportSchedule)).thenReturn(reportScheduleDTO);

        // When
        ReportScheduleDTO result = reportService.createSchedule(reportScheduleDTO);

        // Then
        assertNotNull(result);
        verify(scheduleRepository).save(any(ReportSchedule.class));
    }

    @Test
    void createSchedule_noReportIdOrCode_throwsValidationException() {
        // Given
        reportScheduleDTO.setReportDefinitionId(null);
        reportScheduleDTO.setReportCode(null);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(securityContextHelper.getRequiredCurrentUser()).thenReturn(userPrincipal);

        // When/Then
        assertThrows(ValidationException.class, () -> reportService.createSchedule(reportScheduleDTO));
    }

    // ====== updateSchedule ======

    @Test
    void updateSchedule_success() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(scheduleRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reportSchedule));
        when(scheduleRepository.save(any(ReportSchedule.class))).thenReturn(reportSchedule);
        when(scheduleMapper.toDto(any(ReportSchedule.class))).thenReturn(reportScheduleDTO);

        // When
        ReportScheduleDTO result = reportService.updateSchedule(1L, reportScheduleDTO);

        // Then
        assertNotNull(result);
        verify(scheduleMapper).updateEntity(eq(reportScheduleDTO), any(ReportSchedule.class));
        verify(scheduleRepository).save(any(ReportSchedule.class));
    }

    @Test
    void updateSchedule_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(scheduleRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> reportService.updateSchedule(999L, reportScheduleDTO));
    }

    // ====== deleteSchedule ======

    @Test
    void deleteSchedule_success() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(scheduleRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reportSchedule));

        // When
        reportService.deleteSchedule(1L);

        // Then
        verify(scheduleRepository).delete(reportSchedule);
    }

    @Test
    void deleteSchedule_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(scheduleRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> reportService.deleteSchedule(999L));
    }

    // ====== toggleSchedule ======

    @Test
    void toggleSchedule_enable_success() {
        // Given
        reportSchedule.setActive(false);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(scheduleRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reportSchedule));
        when(scheduleRepository.save(any(ReportSchedule.class))).thenReturn(reportSchedule);
        when(scheduleMapper.toDto(any(ReportSchedule.class))).thenReturn(reportScheduleDTO);

        // When
        ReportScheduleDTO result = reportService.toggleSchedule(1L, true);

        // Then
        assertNotNull(result);
        verify(scheduleRepository).save(any(ReportSchedule.class));
    }

    @Test
    void toggleSchedule_disable_success() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(scheduleRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reportSchedule));
        when(scheduleRepository.save(any(ReportSchedule.class))).thenReturn(reportSchedule);
        when(scheduleMapper.toDto(any(ReportSchedule.class))).thenReturn(reportScheduleDTO);

        // When
        ReportScheduleDTO result = reportService.toggleSchedule(1L, false);

        // Then
        assertNotNull(result);
        verify(scheduleRepository).save(any(ReportSchedule.class));
    }

    @Test
    void toggleSchedule_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(scheduleRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> reportService.toggleSchedule(999L, true));
    }

    // ====== getExecutions ======

    @Test
    void getExecutions_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReportExecution> page = new PageImpl<>(List.of(reportExecution), pageable, 1);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(executionRepository.findByTenantIdOrderByStartedAtDesc(TENANT_ID, pageable)).thenReturn(page);
        when(executionMapper.toDto(reportExecution)).thenReturn(reportExecutionDTO);

        // When
        var result = reportService.getExecutions(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ====== getExecution ======

    @Test
    void getExecution_found_returnsDto() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(executionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(reportExecution));
        when(executionMapper.toDto(reportExecution)).thenReturn(reportExecutionDTO);

        // When
        ReportExecutionDTO result = reportService.getExecution(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
    }

    @Test
    void getExecution_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(executionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> reportService.getExecution(999L));
    }

    // ====== getExecutionsByReport ======

    @Test
    void getExecutionsByReport_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReportExecution> page = new PageImpl<>(List.of(reportExecution), pageable, 1);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(executionRepository.findByReportDefinitionIdAndTenantId(1L, TENANT_ID, pageable)).thenReturn(page);
        when(executionMapper.toDto(reportExecution)).thenReturn(reportExecutionDTO);

        // When
        var result = reportService.getExecutionsByReport(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ====== getReportExecutionHistory ======

    @Test
    void getReportExecutionHistory_success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<ReportExecution> page = new PageImpl<>(List.of(reportExecution), pageable, 1);
        reportDefinition.setId(1L);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findByCodeAndTenantId("SALES_SUMMARY", TENANT_ID)).thenReturn(Optional.of(reportDefinition));
        when(executionRepository.findByReportDefinitionIdAndTenantId(1L, TENANT_ID, pageable)).thenReturn(page);
        when(executionMapper.toDto(reportExecution)).thenReturn(reportExecutionDTO);

        // When
        var result = reportService.getReportExecutionHistory("SALES_SUMMARY", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getReportExecutionHistory_reportNotFound_throwsNotFoundException() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        when(securityContextHelper.getRequiredTenantId()).thenReturn(TENANT_ID);
        when(definitionRepository.findByCodeAndTenantId("UNKNOWN", TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> reportService.getReportExecutionHistory("UNKNOWN", pageable));
    }
}
