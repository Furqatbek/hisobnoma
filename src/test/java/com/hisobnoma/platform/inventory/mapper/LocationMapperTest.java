package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateLocationRequest;
import com.hisobnoma.platform.inventory.dto.LocationDto;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class LocationMapperTest {

    @Autowired
    private LocationMapper locationMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Location parentLocation = Location.builder()
                .id(10L)
                .code("WH-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true)
                .childLocations(new ArrayList<>())
                .build();

        Location location = Location.builder()
                .id(1L)
                .tenantId(1L)
                .code("ZN-001")
                .name("Zone A")
                .description("Storage zone A")
                .locationType(LocationType.ZONE)
                .parentLocation(parentLocation)
                .address("123 Warehouse St")
                .city("Tashkent")
                .state("Tashkent")
                .country("Uzbekistan")
                .postalCode("100000")
                .phone("+998901234567")
                .email("zone-a@example.com")
                .managerName("John Doe")
                .active(true)
                .isDefault(false)
                .allowNegativeStock(false)
                .sortOrder(1)
                .childLocations(new ArrayList<>())
                .build();

        // When
        LocationDto dto = locationMapper.toDto(location);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("ZN-001", dto.getCode());
        assertEquals("Zone A", dto.getName());
        assertEquals("Storage zone A", dto.getDescription());
        assertEquals(LocationType.ZONE, dto.getLocationType());
        assertEquals(10L, dto.getParentLocationId());
        assertEquals("Main Warehouse", dto.getParentLocationName());
        assertEquals("123 Warehouse St", dto.getAddress());
        assertEquals("Tashkent", dto.getCity());
        assertEquals("Tashkent", dto.getState());
        assertEquals("Uzbekistan", dto.getCountry());
        assertEquals("100000", dto.getPostalCode());
        assertEquals("+998901234567", dto.getPhone());
        assertEquals("zone-a@example.com", dto.getEmail());
        assertEquals("John Doe", dto.getManagerName());
        assertTrue(dto.isActive());
        assertFalse(dto.isDefault());
        assertFalse(dto.isAllowNegativeStock());
        assertEquals(1, dto.getSortOrder());
        // Expression-based fields
        assertEquals("Main Warehouse > Zone A", dto.getFullPath());
        assertEquals(1, dto.getLevel());
    }

    @Test
    void toDto_withNullParent_mapsParentFieldsToNull() {
        // Given
        Location location = Location.builder()
                .id(1L)
                .code("WH-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .parentLocation(null)
                .active(true)
                .childLocations(new ArrayList<>())
                .build();

        // When
        LocationDto dto = locationMapper.toDto(location);

        // Then
        assertNotNull(dto);
        assertNull(dto.getParentLocationId());
        assertNull(dto.getParentLocationName());
        assertEquals("Main Warehouse", dto.getFullPath());
        assertEquals(0, dto.getLevel());
    }

    @Test
    void toEntity_mapsFromCreateRequest() {
        // Given
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("WH-002")
                .name("Secondary Warehouse")
                .description("Backup warehouse")
                .locationType(LocationType.WAREHOUSE)
                .address("456 Storage Blvd")
                .city("Samarkand")
                .state("Samarkand")
                .country("Uzbekistan")
                .postalCode("140000")
                .phone("+998901112233")
                .email("secondary@example.com")
                .managerName("Jane Smith")
                .active(true)
                .isDefault(true)
                .allowNegativeStock(false)
                .sortOrder(2)
                .build();

        // When
        Location entity = locationMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("WH-002", entity.getCode());
        assertEquals("Secondary Warehouse", entity.getName());
        assertEquals("Backup warehouse", entity.getDescription());
        assertEquals(LocationType.WAREHOUSE, entity.getLocationType());
        assertEquals("456 Storage Blvd", entity.getAddress());
        assertEquals("Samarkand", entity.getCity());
        assertEquals("Samarkand", entity.getState());
        assertEquals("Uzbekistan", entity.getCountry());
        assertEquals("140000", entity.getPostalCode());
        assertEquals("+998901112233", entity.getPhone());
        assertEquals("secondary@example.com", entity.getEmail());
        assertEquals("Jane Smith", entity.getManagerName());
        assertTrue(entity.isActive());
        assertTrue(entity.isDefault());
        assertFalse(entity.isAllowNegativeStock());
        assertEquals(2, entity.getSortOrder());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getParentLocation());
    }

    @Test
    void updateEntity_updatesFieldsOnExistingLocation() {
        // Given
        Location existing = Location.builder()
                .id(1L)
                .tenantId(1L)
                .code("WH-001")
                .name("Old Name")
                .locationType(LocationType.WAREHOUSE)
                .active(true)
                .childLocations(new ArrayList<>())
                .build();

        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("WH-001")
                .name("Updated Warehouse")
                .description("Updated description")
                .locationType(LocationType.WAREHOUSE)
                .active(true)
                .build();

        // When
        locationMapper.updateEntity(existing, request);

        // Then
        assertEquals("Updated Warehouse", existing.getName());
        assertEquals("Updated description", existing.getDescription());
        // ID and tenantId should remain unchanged
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
    }
}
