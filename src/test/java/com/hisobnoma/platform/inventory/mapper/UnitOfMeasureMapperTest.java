package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateUomRequest;
import com.hisobnoma.platform.inventory.dto.UnitOfMeasureDto;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class UnitOfMeasureMapperTest {

    @Autowired
    private UnitOfMeasureMapper unitOfMeasureMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        UnitOfMeasure baseUom = UnitOfMeasure.builder()
                .id(1L)
                .code("KG")
                .name("Kilogram")
                .symbol("kg")
                .active(true)
                .isBaseUnit(true)
                .build();

        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(2L)
                .tenantId(1L)
                .code("GR")
                .name("Gram")
                .symbol("g")
                .description("1/1000 of a kilogram")
                .baseUom(baseUom)
                .conversionFactor(new BigDecimal("0.001000"))
                .active(true)
                .isBaseUnit(false)
                .build();

        // When
        UnitOfMeasureDto dto = unitOfMeasureMapper.toDto(uom);

        // Then
        assertNotNull(dto);
        assertEquals(2L, dto.getId());
        assertEquals("GR", dto.getCode());
        assertEquals("Gram", dto.getName());
        assertEquals("g", dto.getSymbol());
        assertEquals("1/1000 of a kilogram", dto.getDescription());
        assertEquals(1L, dto.getBaseUomId());
        assertEquals("KG", dto.getBaseUomCode());
        assertEquals("Kilogram", dto.getBaseUomName());
        assertEquals(new BigDecimal("0.001000"), dto.getConversionFactor());
        assertTrue(dto.isActive());
        assertFalse(dto.isBaseUnit());
    }

    @Test
    void toDto_withNullBaseUom() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .symbol("pcs")
                .baseUom(null)
                .isBaseUnit(true)
                .active(true)
                .build();

        // When
        UnitOfMeasureDto dto = unitOfMeasureMapper.toDto(uom);

        // Then
        assertNotNull(dto);
        assertNull(dto.getBaseUomId());
        assertNull(dto.getBaseUomCode());
        assertNull(dto.getBaseUomName());
        assertTrue(dto.isBaseUnit());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        UnitOfMeasure uom1 = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").active(true).isBaseUnit(true).build();
        UnitOfMeasure uom2 = UnitOfMeasure.builder()
                .id(2L).code("KG").name("Kilogram").active(true).isBaseUnit(true).build();

        // When
        List<UnitOfMeasureDto> dtos = unitOfMeasureMapper.toDtoList(List.of(uom1, uom2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Pieces", dtos.get(0).getName());
        assertEquals("Kilogram", dtos.get(1).getName());
    }

    @Test
    void toEntity_mapsFromCreateRequest() {
        // Given
        CreateUomRequest request = CreateUomRequest.builder()
                .code("LTR")
                .name("Liter")
                .symbol("L")
                .description("Volume measure")
                .conversionFactor(BigDecimal.ONE)
                .active(true)
                .isBaseUnit(true)
                .build();

        // When
        UnitOfMeasure entity = unitOfMeasureMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("LTR", entity.getCode());
        assertEquals("Liter", entity.getName());
        assertEquals("L", entity.getSymbol());
        assertEquals("Volume measure", entity.getDescription());
        assertEquals(0, BigDecimal.ONE.compareTo(entity.getConversionFactor()));
        assertTrue(entity.isActive());
        assertTrue(entity.isBaseUnit());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getBaseUom());
    }
}
