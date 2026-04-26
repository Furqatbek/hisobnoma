package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.FiscalPeriodDto;
import com.hisobnoma.platform.finance.entity.FiscalPeriod;
import com.hisobnoma.platform.finance.entity.FiscalYear;
import com.hisobnoma.platform.finance.entity.PeriodStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FiscalPeriodMapperTest {

    private FiscalPeriodMapper fiscalPeriodMapper;

    @BeforeEach
    void setUp() {
        fiscalPeriodMapper = Mappers.getMapper(FiscalPeriodMapper.class);
    }

    @Test
    void toDto_allFields_mappedCorrectly() {
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

        FiscalPeriod entity = FiscalPeriod.builder()
                .id(1L)
                .fiscalYear(fiscalYear)
                .periodNumber(1)
                .name("January")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN)
                .adjustmentPeriod(false)
                .build();
        entity.setTenantId(1L);

        // When
        FiscalPeriodDto dto = fiscalPeriodMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(1L, dto.getFiscalYearId());
        assertEquals(2024, dto.getFiscalYear());
        assertEquals(1, dto.getPeriodNumber());
        assertEquals("January", dto.getName());
        assertEquals(LocalDate.of(2024, 1, 1), dto.getStartDate());
        assertEquals(LocalDate.of(2024, 1, 31), dto.getEndDate());
        assertEquals(PeriodStatus.OPEN, dto.getStatus());
        assertFalse(dto.isAdjustmentPeriod());
        assertEquals("January 2024", dto.getDisplayName());
    }

    @Test
    void toDtoList_mapsList() {
        // Given
        FiscalYear fiscalYear = FiscalYear.builder()
                .id(1L).year(2024).name("FY 2024")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .periods(new ArrayList<>())
                .build();

        FiscalPeriod p1 = FiscalPeriod.builder()
                .id(1L).fiscalYear(fiscalYear).periodNumber(1).name("January")
                .startDate(LocalDate.of(2024, 1, 1)).endDate(LocalDate.of(2024, 1, 31))
                .status(PeriodStatus.OPEN).build();
        FiscalPeriod p2 = FiscalPeriod.builder()
                .id(2L).fiscalYear(fiscalYear).periodNumber(2).name("February")
                .startDate(LocalDate.of(2024, 2, 1)).endDate(LocalDate.of(2024, 2, 29))
                .status(PeriodStatus.OPEN).build();

        // When
        List<FiscalPeriodDto> dtos = fiscalPeriodMapper.toDtoList(List.of(p1, p2));

        // Then
        assertEquals(2, dtos.size());
        assertEquals("January", dtos.get(0).getName());
        assertEquals("February", dtos.get(1).getName());
    }
}
