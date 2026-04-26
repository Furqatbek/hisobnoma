package com.hisobnoma.platform.reports.mapper;

import com.hisobnoma.platform.reports.dto.ReportScheduleDTO;
import com.hisobnoma.platform.reports.entity.ReportDefinition;
import com.hisobnoma.platform.reports.entity.ReportSchedule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReportScheduleMapperTest {

    @Autowired
    private ReportScheduleMapper reportScheduleMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        ReportDefinition reportDefinition = ReportDefinition.builder()
                .id(10L)
                .code("RPT-001")
                .name("Sales Report")
                .build();

        Instant lastRunAt = Instant.now().minusSeconds(86400);
        Instant nextRunAt = Instant.now().plusSeconds(86400);

        ReportSchedule entity = ReportSchedule.builder()
                .id(1L)
                .tenantId(1L)
                .reportDefinition(reportDefinition)
                .name("Daily Sales Schedule")
                .frequency(ReportSchedule.ScheduleFrequency.DAILY)
                .cronExpression("0 0 6 * * ?")
                .runTime(LocalTime.of(6, 0))
                .dayOfWeek(1)
                .dayOfMonth(15)
                .parameters("{\"period\":\"daily\"}")
                .exportFormat(ReportDefinition.ExportFormat.EXCEL)
                .deliveryMethod(ReportSchedule.DeliveryMethod.EMAIL)
                .emailRecipients("admin@example.com,manager@example.com")
                .lastRunAt(lastRunAt)
                .nextRunAt(nextRunAt)
                .lastRunStatus("COMPLETED")
                .active(true)
                .build();

        // When
        ReportScheduleDTO dto = reportScheduleMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getReportDefinitionId());
        assertEquals("RPT-001", dto.getReportCode());
        assertEquals("Sales Report", dto.getReportName());
        assertEquals("Daily Sales Schedule", dto.getName());
        assertEquals(ReportSchedule.ScheduleFrequency.DAILY, dto.getFrequency());
        assertEquals("0 0 6 * * ?", dto.getCronExpression());
        assertEquals(LocalTime.of(6, 0), dto.getRunTime());
        assertEquals(1, dto.getDayOfWeek());
        assertEquals(15, dto.getDayOfMonth());
        assertEquals("{\"period\":\"daily\"}", dto.getParameters());
        assertEquals(ReportDefinition.ExportFormat.EXCEL, dto.getExportFormat());
        assertEquals(ReportSchedule.DeliveryMethod.EMAIL, dto.getDeliveryMethod());
        assertEquals("admin@example.com,manager@example.com", dto.getEmailRecipients());
        assertEquals(lastRunAt, dto.getLastRunAt());
        assertEquals(nextRunAt, dto.getNextRunAt());
        assertEquals("COMPLETED", dto.getLastRunStatus());
        assertTrue(dto.isActive());
    }

    @Test
    void toEntity_fromDto_mapsBasicFields() {
        // Given
        ReportScheduleDTO dto = ReportScheduleDTO.builder()
                .name("Weekly Report")
                .frequency(ReportSchedule.ScheduleFrequency.WEEKLY)
                .cronExpression("0 0 8 ? * MON")
                .runTime(LocalTime.of(8, 0))
                .dayOfWeek(1)
                .parameters("{\"period\":\"weekly\"}")
                .exportFormat(ReportDefinition.ExportFormat.PDF)
                .deliveryMethod(ReportSchedule.DeliveryMethod.STORE_ONLY)
                .emailRecipients("team@example.com")
                .active(true)
                .build();

        // When
        ReportSchedule entity = reportScheduleMapper.toEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals("Weekly Report", entity.getName());
        assertEquals(ReportSchedule.ScheduleFrequency.WEEKLY, entity.getFrequency());
        assertEquals("0 0 8 ? * MON", entity.getCronExpression());
        assertEquals(LocalTime.of(8, 0), entity.getRunTime());
        assertEquals(1, entity.getDayOfWeek());
        assertEquals("{\"period\":\"weekly\"}", entity.getParameters());
        assertEquals(ReportDefinition.ExportFormat.PDF, entity.getExportFormat());
        assertEquals(ReportSchedule.DeliveryMethod.STORE_ONLY, entity.getDeliveryMethod());
        assertEquals("team@example.com", entity.getEmailRecipients());
        assertTrue(entity.isActive());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getReportDefinition());
        assertNull(entity.getLastRunAt());
        assertNull(entity.getNextRunAt());
        assertNull(entity.getLastRunStatus());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        ReportSchedule existing = ReportSchedule.builder()
                .id(1L)
                .tenantId(1L)
                .name("Old Schedule")
                .frequency(ReportSchedule.ScheduleFrequency.DAILY)
                .exportFormat(ReportDefinition.ExportFormat.PDF)
                .deliveryMethod(ReportSchedule.DeliveryMethod.STORE_ONLY)
                .active(true)
                .build();

        ReportScheduleDTO dto = ReportScheduleDTO.builder()
                .name("Updated Schedule")
                .frequency(ReportSchedule.ScheduleFrequency.WEEKLY)
                .exportFormat(ReportDefinition.ExportFormat.EXCEL)
                .deliveryMethod(ReportSchedule.DeliveryMethod.EMAIL)
                .emailRecipients("new@example.com")
                .active(false)
                .build();

        // When
        reportScheduleMapper.updateEntity(dto, existing);

        // Then
        assertEquals("Updated Schedule", existing.getName());
        assertEquals(ReportSchedule.ScheduleFrequency.WEEKLY, existing.getFrequency());
        assertEquals(ReportDefinition.ExportFormat.EXCEL, existing.getExportFormat());
        assertEquals(ReportSchedule.DeliveryMethod.EMAIL, existing.getDeliveryMethod());
        assertEquals("new@example.com", existing.getEmailRecipients());
        assertFalse(existing.isActive());
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
    }

    @Test
    void toDtoList_mapsAllSchedules() {
        // Given
        ReportDefinition rd = ReportDefinition.builder()
                .id(10L).code("RPT-001").name("Report").build();

        ReportSchedule s1 = ReportSchedule.builder()
                .id(1L).reportDefinition(rd).name("Schedule 1")
                .frequency(ReportSchedule.ScheduleFrequency.DAILY)
                .exportFormat(ReportDefinition.ExportFormat.PDF)
                .deliveryMethod(ReportSchedule.DeliveryMethod.STORE_ONLY).build();
        ReportSchedule s2 = ReportSchedule.builder()
                .id(2L).reportDefinition(rd).name("Schedule 2")
                .frequency(ReportSchedule.ScheduleFrequency.WEEKLY)
                .exportFormat(ReportDefinition.ExportFormat.EXCEL)
                .deliveryMethod(ReportSchedule.DeliveryMethod.EMAIL).build();

        // When
        List<ReportScheduleDTO> dtos = reportScheduleMapper.toDtoList(List.of(s1, s2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
