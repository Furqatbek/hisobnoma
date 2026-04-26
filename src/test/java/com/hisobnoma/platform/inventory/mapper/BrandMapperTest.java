package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.BrandDto;
import com.hisobnoma.platform.inventory.dto.CreateBrandRequest;
import com.hisobnoma.platform.inventory.entity.Brand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class BrandMapperTest {

    @Autowired
    private BrandMapper brandMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Brand brand = Brand.builder()
                .id(1L)
                .tenantId(1L)
                .code("BR-001")
                .name("Samsung")
                .description("Samsung Electronics")
                .logoUrl("https://example.com/samsung.png")
                .website("https://samsung.com")
                .sortOrder(1)
                .active(true)
                .build();

        // When
        BrandDto dto = brandMapper.toDto(brand);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("BR-001", dto.getCode());
        assertEquals("Samsung", dto.getName());
        assertEquals("Samsung Electronics", dto.getDescription());
        assertEquals("https://example.com/samsung.png", dto.getLogoUrl());
        assertEquals("https://samsung.com", dto.getWebsite());
        assertEquals(1, dto.getSortOrder());
        assertTrue(dto.isActive());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        Brand brand1 = Brand.builder()
                .id(1L)
                .code("BR-001")
                .name("Samsung")
                .active(true)
                .build();

        Brand brand2 = Brand.builder()
                .id(2L)
                .code("BR-002")
                .name("Apple")
                .active(false)
                .build();

        // When
        List<BrandDto> dtos = brandMapper.toDtoList(List.of(brand1, brand2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Samsung", dtos.get(0).getName());
        assertEquals("Apple", dtos.get(1).getName());
    }

    @Test
    void toEntity_mapsFromCreateRequest() {
        // Given
        CreateBrandRequest request = CreateBrandRequest.builder()
                .code("BR-003")
                .name("LG")
                .description("LG Electronics")
                .logoUrl("https://example.com/lg.png")
                .website("https://lg.com")
                .sortOrder(3)
                .active(true)
                .build();

        // When
        Brand entity = brandMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("BR-003", entity.getCode());
        assertEquals("LG", entity.getName());
        assertEquals("LG Electronics", entity.getDescription());
        assertEquals("https://example.com/lg.png", entity.getLogoUrl());
        assertEquals("https://lg.com", entity.getWebsite());
        assertEquals(3, entity.getSortOrder());
        assertTrue(entity.isActive());
        // Ignored fields should not be set
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
    }
}
