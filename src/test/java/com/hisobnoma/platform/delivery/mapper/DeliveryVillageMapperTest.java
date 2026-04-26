package com.hisobnoma.platform.delivery.mapper;

import com.hisobnoma.platform.delivery.dto.DeliveryVillageDTO;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class DeliveryVillageMapperTest {

    @Autowired
    private DeliveryVillageMapper deliveryVillageMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        DeliveryRegion region = DeliveryRegion.builder()
                .id(10L)
                .name("Tashkent Region")
                .build();

        DeliveryVillage entity = DeliveryVillage.builder()
                .id(1L)
                .tenantId(1L)
                .region(region)
                .name("Chilanzar")
                .code("CHIL")
                .description("Chilanzar district")
                .active(true)
                .sortOrder(1)
                .build();

        // When
        DeliveryVillageDTO dto = deliveryVillageMapper.toDto(entity);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getRegionId());
        assertEquals("Tashkent Region", dto.getRegionName());
        assertEquals("Chilanzar", dto.getName());
        assertEquals("CHIL", dto.getCode());
        assertEquals("Chilanzar district", dto.getDescription());
        assertTrue(dto.isActive());
        assertEquals(1, dto.getSortOrder());
    }

    @Test
    void toEntity_fromDto_mapsBasicFields() {
        // Given
        DeliveryVillageDTO dto = DeliveryVillageDTO.builder()
                .name("New Village")
                .code("NEW")
                .description("A new village")
                .active(true)
                .sortOrder(5)
                .build();

        // When
        DeliveryVillage entity = deliveryVillageMapper.toEntity(dto);

        // Then
        assertNotNull(entity);
        assertEquals("New Village", entity.getName());
        assertEquals("NEW", entity.getCode());
        assertEquals("A new village", entity.getDescription());
        assertTrue(entity.isActive());
        assertEquals(5, entity.getSortOrder());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getRegion());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        DeliveryRegion region = DeliveryRegion.builder()
                .id(10L).name("Tashkent").build();

        DeliveryVillage existing = DeliveryVillage.builder()
                .id(1L)
                .tenantId(1L)
                .region(region)
                .name("Old Name")
                .code("OLD")
                .active(true)
                .sortOrder(1)
                .build();

        DeliveryVillageDTO dto = DeliveryVillageDTO.builder()
                .name("Updated Name")
                .code("UPD")
                .description("Updated description")
                .active(false)
                .sortOrder(10)
                .build();

        // When
        deliveryVillageMapper.updateEntity(dto, existing);

        // Then
        assertEquals("Updated Name", existing.getName());
        assertEquals("UPD", existing.getCode());
        assertEquals("Updated description", existing.getDescription());
        assertFalse(existing.isActive());
        assertEquals(10, existing.getSortOrder());
        // ID, tenantId, region should remain unchanged
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
        assertNotNull(existing.getRegion());
    }

    @Test
    void toDtoList_mapsAllVillages() {
        // Given
        DeliveryRegion region = DeliveryRegion.builder()
                .id(10L).name("Region").build();

        DeliveryVillage v1 = DeliveryVillage.builder()
                .id(1L).region(region).name("Village 1").code("V1").build();
        DeliveryVillage v2 = DeliveryVillage.builder()
                .id(2L).region(region).name("Village 2").code("V2").build();

        // When
        List<DeliveryVillageDTO> dtos = deliveryVillageMapper.toDtoList(List.of(v1, v2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Village 1", dtos.get(0).getName());
        assertEquals("Village 2", dtos.get(1).getName());
    }
}
