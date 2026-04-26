package com.hisobnoma.platform.reports.mapper;

import com.hisobnoma.platform.reports.dto.ReportExecutionDTO;
import com.hisobnoma.platform.reports.entity.ReportDefinition;
import com.hisobnoma.platform.reports.entity.ReportExecution;
import com.hisobnoma.platform.reports.entity.ReportSchedule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReportExecutionMapperTest {

    @Autowired
    private ReportExecutionMapper reportExecutionMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        ReportDefinition reportDefinition = ReportDefinition.builder()
                .id(10L)
                .code("RPT-001")
                .name("Sales Report")
                .build();

        ReportSchedule reportSchedule = ReportSchedule.builder()
                .id(20L)
                .build();

        Instant startedAt = Instant.now().minusSeconds(60);
        Instant completedAt = Instant.now();

        ReportExecution entity = ReportExecution.builder()
                .id(1L)
                .tenantId(1L)
                .reportDefinition(reportDefinition)
                .reportSchedule(reportSchedule)
                .executedByUserId(5L)
                .source(ReportExecution.ExecutionSource.MANUAL)
                .status(ReportExecution.ExecutionStatus.COMPLETED)
                .parameters("{\"date\":\"2024-01-01\"}")
                .exportFormat(ReportDefinition.ExportFormat.PDF)
                .startedAt(startedAt)
                .completedAt(completedAt)
                .durationMs(60000L)
                .rowCount(150)
                .filePath("/reports/output/sales-2024-01-01.pdf")
                .fileSize(25600L)
                .errorMessage(null)
                .build();

        // When
        ReportExecutionDTO dto = reportExecutionMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getReportDefinitionId());
        assertEquals("RPT-001", dto.getReportCode());
        assertEquals("Sales Report", dto.getReportName());
        assertEquals(20L, dto.getReportScheduleId());
        assertEquals(5L, dto.getExecutedByUserId());
        assertEquals(ReportExecution.ExecutionSource.MANUAL, dto.getSource());
        assertEquals(ReportExecution.ExecutionStatus.COMPLETED, dto.getStatus());
        assertEquals("{\"date\":\"2024-01-01\"}", dto.getParameters());
        assertEquals(ReportDefinition.ExportFormat.PDF, dto.getExportFormat());
        assertEquals(startedAt, dto.getStartedAt());
        assertEquals(completedAt, dto.getCompletedAt());
        assertEquals(60000L, dto.getDurationMs());
        assertEquals(150, dto.getRowCount());
        assertEquals("/reports/output/sales-2024-01-01.pdf", dto.getFilePath());
        assertEquals(25600L, dto.getFileSize());
        assertNull(dto.getErrorMessage());
    }

    @Test
    void toDto_withNullSchedule_mapsScheduleIdToNull() {
        // Given
        ReportDefinition reportDefinition = ReportDefinition.builder()
                .id(10L).code("RPT-001").name("Sales Report").build();

        ReportExecution entity = ReportExecution.builder()
                .id(1L)
                .reportDefinition(reportDefinition)
                .reportSchedule(null)
                .executedByUserId(5L)
                .source(ReportExecution.ExecutionSource.MANUAL)
                .status(ReportExecution.ExecutionStatus.COMPLETED)
                .build();

        // When
        ReportExecutionDTO dto = reportExecutionMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertNull(dto.getReportScheduleId());
    }

    @Test
    void toDto_withFailedExecution_mapsErrorMessage() {
        // Given
        ReportDefinition reportDefinition = ReportDefinition.builder()
                .id(10L).code("RPT-001").name("Sales Report").build();

        ReportExecution entity = ReportExecution.builder()
                .id(1L)
                .reportDefinition(reportDefinition)
                .executedByUserId(5L)
                .source(ReportExecution.ExecutionSource.SCHEDULED)
                .status(ReportExecution.ExecutionStatus.FAILED)
                .errorMessage("Database connection timeout")
                .build();

        // When
        ReportExecutionDTO dto = reportExecutionMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(ReportExecution.ExecutionStatus.FAILED, dto.getStatus());
        assertEquals("Database connection timeout", dto.getErrorMessage());
    }

    @Test
    void toDtoList_mapsAllExecutions() {
        // Given
        ReportDefinition rd = ReportDefinition.builder()
                .id(10L).code("RPT-001").name("Report").build();

        ReportExecution e1 = ReportExecution.builder()
                .id(1L).reportDefinition(rd)
                .source(ReportExecution.ExecutionSource.MANUAL)
                .status(ReportExecution.ExecutionStatus.COMPLETED).build();
        ReportExecution e2 = ReportExecution.builder()
                .id(2L).reportDefinition(rd)
                .source(ReportExecution.ExecutionSource.SCHEDULED)
                .status(ReportExecution.ExecutionStatus.COMPLETED).build();

        // When
        List<ReportExecutionDTO> dtos = reportExecutionMapper.toDtoList(List.of(e1, e2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
