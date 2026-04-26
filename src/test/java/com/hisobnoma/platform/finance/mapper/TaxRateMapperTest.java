package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateTaxRateRequest;
import com.hisobnoma.platform.finance.dto.TaxRateDto;
import com.hisobnoma.platform.finance.entity.TaxCode;
import com.hisobnoma.platform.finance.entity.TaxRate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class TaxRateMapperTest {

    @Autowired
    private TaxRateMapper taxRateMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        TaxCode taxCode = TaxCode.builder()
                .id(10L)
                .code("VAT-12")
                .name("VAT 12%")
                .build();

        TaxRate taxRate = TaxRate.builder()
                .id(1L)
                .tenantId(1L)
                .taxCode(taxCode)
                .name("Standard VAT Rate")
                .rate(new BigDecimal("12.0000"))
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .effectiveTo(LocalDate.of(2024, 12, 31))
                .minAmount(new BigDecimal("1000.0000"))
                .maxAmount(new BigDecimal("10000000.0000"))
                .active(true)
                .notes("Standard rate for 2024")
                .build();

        // When
        TaxRateDto dto = taxRateMapper.toDto(taxRate);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getTaxCodeId());
        assertEquals("VAT-12", dto.getTaxCodeCode());
        assertEquals("VAT 12%", dto.getTaxCodeName());
        assertEquals("Standard VAT Rate", dto.getName());
        assertEquals(new BigDecimal("12.0000"), dto.getRate());
        assertEquals(LocalDate.of(2024, 1, 1), dto.getEffectiveFrom());
        assertEquals(LocalDate.of(2024, 12, 31), dto.getEffectiveTo());
        assertEquals(new BigDecimal("1000.0000"), dto.getMinAmount());
        assertEquals(new BigDecimal("10000000.0000"), dto.getMaxAmount());
        assertTrue(dto.isActive());
        assertEquals("Standard rate for 2024", dto.getNotes());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreateTaxRateRequest request = CreateTaxRateRequest.builder()
                .taxCodeId(10L)
                .name("New VAT Rate")
                .rate(new BigDecimal("15.0000"))
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .effectiveTo(LocalDate.of(2025, 12, 31))
                .minAmount(new BigDecimal("500.0000"))
                .maxAmount(new BigDecimal("5000000.0000"))
                .notes("New rate for 2025")
                .build();

        // When
        TaxRate entity = taxRateMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("New VAT Rate", entity.getName());
        assertEquals(new BigDecimal("15.0000"), entity.getRate());
        assertEquals(LocalDate.of(2025, 1, 1), entity.getEffectiveFrom());
        assertEquals(LocalDate.of(2025, 12, 31), entity.getEffectiveTo());
        assertEquals(new BigDecimal("500.0000"), entity.getMinAmount());
        assertEquals(new BigDecimal("5000000.0000"), entity.getMaxAmount());
        assertEquals("New rate for 2025", entity.getNotes());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getTaxCode());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        TaxCode taxCode = TaxCode.builder().id(10L).code("VAT-12").name("VAT 12%").build();

        TaxRate existing = TaxRate.builder()
                .id(1L)
                .tenantId(1L)
                .taxCode(taxCode)
                .name("Old Rate")
                .rate(new BigDecimal("12.0000"))
                .effectiveFrom(LocalDate.of(2024, 1, 1))
                .active(true)
                .build();

        CreateTaxRateRequest request = CreateTaxRateRequest.builder()
                .taxCodeId(10L)
                .name("Updated Rate")
                .rate(new BigDecimal("15.0000"))
                .effectiveFrom(LocalDate.of(2025, 1, 1))
                .notes("Rate updated")
                .build();

        // When
        taxRateMapper.updateEntity(request, existing);

        // Then
        assertEquals("Updated Rate", existing.getName());
        assertEquals(new BigDecimal("15.0000"), existing.getRate());
        assertEquals(LocalDate.of(2025, 1, 1), existing.getEffectiveFrom());
        assertEquals("Rate updated", existing.getNotes());
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
        assertNotNull(existing.getTaxCode()); // ignored in update
    }

    @Test
    void toDtoList_mapsAllRates() {
        // Given
        TaxCode tc = TaxCode.builder().id(10L).code("VAT").name("VAT").build();

        TaxRate r1 = TaxRate.builder()
                .id(1L).taxCode(tc).name("Rate 1").rate(new BigDecimal("12.0000"))
                .effectiveFrom(LocalDate.of(2024, 1, 1)).active(true).build();
        TaxRate r2 = TaxRate.builder()
                .id(2L).taxCode(tc).name("Rate 2").rate(new BigDecimal("15.0000"))
                .effectiveFrom(LocalDate.of(2025, 1, 1)).active(true).build();

        // When
        List<TaxRateDto> dtos = taxRateMapper.toDtoList(List.of(r1, r2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
