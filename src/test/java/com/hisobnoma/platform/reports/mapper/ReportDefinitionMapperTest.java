package com.hisobnoma.platform.reports.mapper;

import com.hisobnoma.platform.reports.dto.ReportDefinitionDTO;
import com.hisobnoma.platform.reports.entity.ReportDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ReportDefinitionMapperTest {

    @Autowired
    private ReportDefinitionMapper reportDefinitionMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        List<ReportDefinition.ExportFormat> formats = List.of(
                ReportDefinition.ExportFormat.PDF,
                ReportDefinition.ExportFormat.EXCEL
        );

        ReportDefinition entity = ReportDefinition.builder()
                .id(1L)
                .tenantId(1L)
                .code("RPT-001")
                .name("Sales Report")
                .description("Daily sales summary")
                .category(ReportDefinition.ReportCategory.SALES)
                .queryClass("com.hisobnoma.reports.SalesQuery")
                .templatePath("/templates/sales.jrxml")
                .supportedFormats(formats)
                .parameterSchema("{\"type\":\"object\"}")
                .defaultParameters("{\"period\":\"daily\"}")
                .systemReport(false)
                .active(true)
                .sortOrder(1)
                .schedules(new ArrayList<>())
                .build();

        // When
        ReportDefinitionDTO dto = reportDefinitionMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("RPT-001", dto.getCode());
        assertEquals("Sales Report", dto.getName());
        assertEquals("Daily sales summary", dto.getDescription());
        assertEquals(ReportDefinition.ReportCategory.SALES, dto.getCategory());
        assertNotNull(dto.getSupportedFormats());
        assertEquals(2, dto.getSupportedFormats().size());
        assertEquals("{\"type\":\"object\"}", dto.getParameterSchema());
        assertEquals("{\"period\":\"daily\"}", dto.getDefaultParameters());
        assertFalse(dto.isSystemReport());
        assertTrue(dto.isActive());
        assertEquals(1, dto.getSortOrder());
    }

    @Test
    void toEntity_fromDto_mapsBasicFields() {
        // Given
        ReportDefinitionDTO dto = ReportDefinitionDTO.builder()
                .code("RPT-002")
                .name("Inventory Report")
                .description("Current stock levels")
                .category(ReportDefinition.ReportCategory.INVENTORY)
                .parameterSchema("{\"type\":\"object\"}")
                .defaultParameters("{}")
                .systemReport(true)
                .active(true)
                .sortOrder(2)
                .build();

        // When
        ReportDefinition entity = reportDefinitionMapper.toEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals("RPT-002", entity.getCode());
        assertEquals("Inventory Report", entity.getName());
        assertEquals("Current stock levels", entity.getDescription());
        assertEquals(ReportDefinition.ReportCategory.INVENTORY, entity.getCategory());
        assertEquals("{\"type\":\"object\"}", entity.getParameterSchema());
        assertEquals("{}", entity.getDefaultParameters());
        assertTrue(entity.isSystemReport());
        assertTrue(entity.isActive());
        assertEquals(2, entity.getSortOrder());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getQueryClass());
        assertNull(entity.getTemplatePath());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        ReportDefinition existing = ReportDefinition.builder()
                .id(1L)
                .tenantId(1L)
                .code("RPT-001")
                .name("Old Name")
                .category(ReportDefinition.ReportCategory.SALES)
                .active(true)
                .schedules(new ArrayList<>())
                .build();

        ReportDefinitionDTO dto = ReportDefinitionDTO.builder()
                .code("RPT-001")
                .name("Updated Name")
                .description("Updated description")
                .category(ReportDefinition.ReportCategory.SALES)
                .build();

        // When
        reportDefinitionMapper.updateEntity(dto, existing);

        // Then
        assertEquals("Updated Name", existing.getName());
        assertEquals("Updated description", existing.getDescription());
        // code is ignored in updateEntity
        assertEquals("RPT-001", existing.getCode());
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
    }

    @Test
    void toDtoList_mapsAllDefinitions() {
        // Given
        ReportDefinition r1 = ReportDefinition.builder()
                .id(1L).code("RPT-001").name("Report 1")
                .category(ReportDefinition.ReportCategory.SALES)
                .schedules(new ArrayList<>()).build();
        ReportDefinition r2 = ReportDefinition.builder()
                .id(2L).code("RPT-002").name("Report 2")
                .category(ReportDefinition.ReportCategory.INVENTORY)
                .schedules(new ArrayList<>()).build();

        // When
        List<ReportDefinitionDTO> dtos = reportDefinitionMapper.toDtoList(List.of(r1, r2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
