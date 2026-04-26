package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.CreateLocationRequest;
import com.hisobnoma.platform.inventory.dto.LocationDto;
import com.hisobnoma.platform.inventory.entity.Location;
import com.hisobnoma.platform.inventory.entity.LocationType;
import com.hisobnoma.platform.inventory.mapper.LocationMapper;
import com.hisobnoma.platform.inventory.repository.LocationRepository;
import com.hisobnoma.platform.inventory.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocationServiceTest {

    @Mock
    private LocationRepository locationRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private LocationMapper locationMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private LocationService locationService;

    private static final Long TENANT_ID = 1L;
    private Location location;
    private LocationDto locationDto;

    @BeforeEach
    void setUp() {
        location = Location.builder()
                .id(1L)
                .code("WH-001")
                .name("Main Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .active(true)
                .sortOrder(0)
                .childLocations(new ArrayList<>())
                .tenantId(TENANT_ID)
                .build();

        locationDto = LocationDto.builder()
                .id(1L)
                .code("WH-001")
                .name("Main Warehouse")
                .active(true)
                .build();
    }

    @Test
    void getLocations_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Location> page = new PageImpl<>(List.of(location), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        // When
        PageResponse<LocationDto> result = locationService.getLocations(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getAllLocations_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByTenantIdOrderBySortOrderAsc(TENANT_ID)).thenReturn(List.of(location));
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        // When
        List<LocationDto> result = locationService.getAllLocations();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getActiveLocations_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByTenantIdAndActiveOrderBySortOrderAsc(TENANT_ID, true)).thenReturn(List.of(location));
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        // When
        List<LocationDto> result = locationService.getActiveLocations();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getLocation_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        // When
        LocationDto result = locationService.getLocation(1L);

        // Then
        assertNotNull(result);
        assertEquals("Main Warehouse", result.getName());
    }

    @Test
    void getLocation_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> locationService.getLocation(999L));
    }

    @Test
    void getLocationByCode_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByCodeAndTenantId("WH-001", TENANT_ID)).thenReturn(Optional.of(location));
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        // When
        LocationDto result = locationService.getLocationByCode("WH-001");

        // Then
        assertNotNull(result);
        assertEquals("WH-001", result.getCode());
    }

    @Test
    void createLocation_success() {
        // Given
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("WH-002")
                .name("New Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.existsByCodeAndTenantId("WH-002", TENANT_ID)).thenReturn(false);
        when(locationMapper.toEntity(request)).thenReturn(location);
        when(locationRepository.save(any(Location.class))).thenReturn(location);
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        // When
        LocationDto result = locationService.createLocation(request);

        // Then
        assertNotNull(result);
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void createLocation_duplicateCode_throwsBusinessException() {
        // Given
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("WH-001")
                .name("Duplicate")
                .locationType(LocationType.WAREHOUSE)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.existsByCodeAndTenantId("WH-001", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> locationService.createLocation(request));
        verify(locationRepository, never()).save(any());
    }

    @Test
    void updateLocation_success() {
        // Given
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("WH-001")
                .name("Updated Warehouse")
                .locationType(LocationType.WAREHOUSE)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(locationRepository.existsByCodeAndTenantIdAndIdNot("WH-001", TENANT_ID, 1L)).thenReturn(false);
        when(locationRepository.save(any(Location.class))).thenReturn(location);
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        // When
        LocationDto result = locationService.updateLocation(1L, request);

        // Then
        assertNotNull(result);
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void updateLocation_notFound_throwsNotFoundException() {
        // Given
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("WH-999")
                .name("Updated")
                .locationType(LocationType.WAREHOUSE)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> locationService.updateLocation(999L, request));
    }

    @Test
    void updateLocation_selfParent_throwsBusinessException() {
        // Given
        CreateLocationRequest request = CreateLocationRequest.builder()
                .code("WH-001")
                .name("Updated")
                .locationType(LocationType.WAREHOUSE)
                .parentLocationId(1L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(locationRepository.existsByCodeAndTenantIdAndIdNot("WH-001", TENANT_ID, 1L)).thenReturn(false);

        // When/Then
        assertThrows(BusinessException.class, () -> locationService.updateLocation(1L, request));
    }

    @Test
    void deleteLocation_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(locationRepository.hasChildren(1L, TENANT_ID)).thenReturn(false);
        when(stockRepository.findByLocationIdAndTenantId(1L, TENANT_ID)).thenReturn(Collections.emptyList());

        // When
        locationService.deleteLocation(1L);

        // Then
        verify(locationRepository).delete(location);
    }

    @Test
    void deleteLocation_hasChildren_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(locationRepository.hasChildren(1L, TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> locationService.deleteLocation(1L));
        verify(locationRepository, never()).delete(any());
    }

    @Test
    void deleteLocation_hasStock_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(location));
        when(locationRepository.hasChildren(1L, TENANT_ID)).thenReturn(false);
        when(stockRepository.findByLocationIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(new com.hisobnoma.platform.inventory.entity.Stock()));

        // When/Then
        assertThrows(BusinessException.class, () -> locationService.deleteLocation(1L));
        verify(locationRepository, never()).delete(any());
    }

    @Test
    void deleteLocation_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> locationService.deleteLocation(999L));
    }

    @Test
    void getLocationsByType_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByTenantIdAndLocationTypeOrderBySortOrderAsc(TENANT_ID, LocationType.WAREHOUSE))
                .thenReturn(List.of(location));
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        // When
        List<LocationDto> result = locationService.getLocationsByType(LocationType.WAREHOUSE);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void searchLocations_returnsResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Location> page = new PageImpl<>(List.of(location), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.searchByNameOrCode(TENANT_ID, "Main", pageable)).thenReturn(page);
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        // When
        PageResponse<LocationDto> result = locationService.searchLocations("Main", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getDefaultLocation_exists_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByTenantIdAndIsDefaultTrue(TENANT_ID)).thenReturn(Optional.of(location));
        when(locationMapper.toDto(location)).thenReturn(locationDto);

        // When
        LocationDto result = locationService.getDefaultLocation();

        // Then
        assertNotNull(result);
    }

    @Test
    void getDefaultLocation_notExists_returnsNull() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(locationRepository.findByTenantIdAndIsDefaultTrue(TENANT_ID)).thenReturn(Optional.empty());

        // When
        LocationDto result = locationService.getDefaultLocation();

        // Then
        assertNull(result);
    }
}
