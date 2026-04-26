package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateFiscalYearRequest;
import com.hisobnoma.platform.finance.dto.FiscalYearDto;
import com.hisobnoma.platform.finance.entity.FiscalYear;
import com.hisobnoma.platform.finance.entity.PeriodStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class FiscalYearMapperTest {

    @Autowired
    private FiscalYearMapper fiscalYearMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        FiscalYear fiscalYear = FiscalYear.builder()
                .id(1L)
                .tenantId(1L)
                .year(2024)
                .name("Fiscal Year 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .status(PeriodStatus.OPEN)
                .current(true)
                .closed(false)
                .retainedEarningsAccountId(100L)
                .notes("FY2024")
                .periods(new ArrayList<>())
                .build();

        // When
        FiscalYearDto dto = fiscalYearMapper.toDto(fiscalYear);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(2024, dto.getYear());
        assertEquals("Fiscal Year 2024", dto.getName());
        assertEquals(LocalDate.of(2024, 1, 1), dto.getStartDate());
        assertEquals(LocalDate.of(2024, 12, 31), dto.getEndDate());
        assertEquals(PeriodStatus.OPEN, dto.getStatus());
        assertTrue(dto.isCurrent());
        assertFalse(dto.isClosed());
        assertEquals(100L, dto.getRetainedEarningsAccountId());
        assertEquals("FY2024", dto.getNotes());
        // toDto includes periods
        assertNotNull(dto.getPeriods());
    }

    @Test
    void toDtoWithoutPeriods_excludesPeriods() {
        // Given
        FiscalYear fiscalYear = FiscalYear.builder()
                .id(1L)
                .year(2024)
                .name("Fiscal Year 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .status(PeriodStatus.OPEN)
                .periods(new ArrayList<>())
                .build();

        // When
        FiscalYearDto dto = fiscalYearMapper.toDtoWithoutPeriods(fiscalYear);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(2024, dto.getYear());
        assertNull(dto.getPeriods());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2025)
                .name("Fiscal Year 2025")
                .startDate(LocalDate.of(2025, 1, 1))
                .endDate(LocalDate.of(2025, 12, 31))
                .current(false)
                .retainedEarningsAccountId(200L)
                .notes("FY2025")
                .build();

        // When
        FiscalYear entity = fiscalYearMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(2025, entity.getYear());
        assertEquals("Fiscal Year 2025", entity.getName());
        assertEquals(LocalDate.of(2025, 1, 1), entity.getStartDate());
        assertEquals(LocalDate.of(2025, 12, 31), entity.getEndDate());
        assertFalse(entity.isCurrent());
        assertEquals(200L, entity.getRetainedEarningsAccountId());
        assertEquals("FY2025", entity.getNotes());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getClosedAt());
        assertNull(entity.getClosedBy());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        FiscalYear existing = FiscalYear.builder()
                .id(1L)
                .tenantId(1L)
                .year(2024)
                .name("Old Name")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .status(PeriodStatus.OPEN)
                .current(true)
                .periods(new ArrayList<>())
                .build();

        CreateFiscalYearRequest request = CreateFiscalYearRequest.builder()
                .year(2024)
                .name("Updated FY 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .notes("Updated notes")
                .build();

        // When
        fiscalYearMapper.updateEntity(request, existing);

        // Then
        assertEquals("Updated FY 2024", existing.getName());
        assertEquals("Updated notes", existing.getNotes());
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
        assertEquals(PeriodStatus.OPEN, existing.getStatus()); // ignored in update
    }

    @Test
    void toDtoList_mapsAllFiscalYears() {
        // Given
        FiscalYear fy1 = FiscalYear.builder()
                .id(1L).year(2023).name("FY 2023")
                .startDate(LocalDate.of(2023, 1, 1))
                .endDate(LocalDate.of(2023, 12, 31))
                .status(PeriodStatus.CLOSED)
                .periods(new ArrayList<>()).build();

        FiscalYear fy2 = FiscalYear.builder()
                .id(2L).year(2024).name("FY 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .status(PeriodStatus.OPEN)
                .periods(new ArrayList<>()).build();

        // When
        List<FiscalYearDto> dtos = fiscalYearMapper.toDtoList(List.of(fy1, fy2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        // toDtoList uses toDtoWithoutPeriods
        assertNull(dtos.get(0).getPeriods());
    }
}
