package com.hisobnoma.platform.delivery.mapper;

import com.hisobnoma.platform.delivery.dto.DeliveryRegionDTO;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DeliveryRegionMapperTest {

    @Autowired
    private DeliveryRegionMapper deliveryRegionMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        DeliveryVillage v1 = DeliveryVillage.builder().id(1L).name("Village 1").build();
        DeliveryVillage v2 = DeliveryVillage.builder().id(2L).name("Village 2").build();
        List<DeliveryVillage> villages = new ArrayList<>();
        villages.add(v1);
        villages.add(v2);

        DeliveryRegion entity = DeliveryRegion.builder()
                .id(1L)
                .tenantId(1L)
                .name("Tashkent Region")
                .code("TASH")
                .description("Capital region")
                .active(true)
                .sortOrder(1)
                .villages(villages)
                .build();

        // When
        DeliveryRegionDTO dto = deliveryRegionMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("Tashkent Region", dto.getName());
        assertEquals("TASH", dto.getCode());
        assertEquals("Capital region", dto.getDescription());
        assertTrue(dto.isActive());
        assertEquals(1, dto.getSortOrder());
        assertEquals(2, dto.getVillageCount());
    }

    @Test
    void toDto_withNullVillages_returnsZeroCount() {
        // Given
        DeliveryRegion entity = DeliveryRegion.builder()
                .id(1L)
                .name("Empty Region")
                .code("EMPTY")
                .active(true)
                .sortOrder(0)
                .villages(null)
                .build();

        // When
        DeliveryRegionDTO dto = deliveryRegionMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(0, dto.getVillageCount());
    }

    @Test
    void toEntity_fromDto_mapsBasicFields() {
        // Given
        DeliveryRegionDTO dto = DeliveryRegionDTO.builder()
                .name("New Region")
                .code("NEW")
                .description("A new delivery region")
                .active(true)
                .sortOrder(5)
                .build();

        // When
        DeliveryRegion entity = deliveryRegionMapper.toEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals("New Region", entity.getName());
        assertEquals("NEW", entity.getCode());
        assertEquals("A new delivery region", entity.getDescription());
        assertTrue(entity.isActive());
        assertEquals(5, entity.getSortOrder());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getVillages());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        DeliveryRegion existing = DeliveryRegion.builder()
                .id(1L)
                .tenantId(1L)
                .name("Old Name")
                .code("OLD")
                .active(true)
                .sortOrder(1)
                .villages(new ArrayList<>())
                .build();

        DeliveryRegionDTO dto = DeliveryRegionDTO.builder()
                .name("Updated Name")
                .code("UPD")
                .description("Updated description")
                .active(false)
                .sortOrder(10)
                .build();

        // When
        deliveryRegionMapper.updateEntity(dto, existing);

        // Then
        assertEquals("Updated Name", existing.getName());
        assertEquals("UPD", existing.getCode());
        assertEquals("Updated description", existing.getDescription());
        assertFalse(existing.isActive());
        assertEquals(10, existing.getSortOrder());
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
    }

    @Test
    void toDtoList_mapsAllRegions() {
        // Given
        DeliveryRegion r1 = DeliveryRegion.builder()
                .id(1L).name("Region 1").code("R1").active(true)
                .villages(new ArrayList<>()).build();
        DeliveryRegion r2 = DeliveryRegion.builder()
                .id(2L).name("Region 2").code("R2").active(true)
                .villages(new ArrayList<>()).build();

        // When
        List<DeliveryRegionDTO> dtos = deliveryRegionMapper.toDtoList(List.of(r1, r2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Region 1", dtos.get(0).getName());
        assertEquals("Region 2", dtos.get(1).getName());
    }
}
