package com.hisobnoma.platform.delivery.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.delivery.dto.DeliveryRegionDTO;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.mapper.DeliveryRegionMapper;
import com.hisobnoma.platform.delivery.repository.DeliveryRegionRepository;
import com.hisobnoma.platform.delivery.repository.DeliveryVillageRepository;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeliveryRegionServiceTest {

    @Mock
    private DeliveryRegionRepository regionRepository;

    @Mock
    private DeliveryVillageRepository villageRepository;

    @Mock
    private DeliveryRegionMapper regionMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private DeliveryRegionService deliveryRegionService;

    private DeliveryRegion region;
    private DeliveryRegionDTO regionDTO;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        region = DeliveryRegion.builder()
                .id(1L)
                .name("Tashkent")
                .code("TASH")
                .description("Tashkent region")
                .active(true)
                .sortOrder(1)
                .tenantId(TENANT_ID)
                .build();

        regionDTO = DeliveryRegionDTO.builder()
                .id(1L)
                .name("Tashkent")
                .code("TASH")
                .description("Tashkent region")
                .active(true)
                .sortOrder(1)
                .villageCount(5)
                .build();
    }

    // ====== findAll ======

    @Test
    void findAll_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<DeliveryRegion> page = new PageImpl<>(List.of(region), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(regionMapper.toDto(region)).thenReturn(regionDTO);

        // When
        var result = deliveryRegionService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Tashkent", result.getContent().get(0).getName());
    }

    // ====== search ======

    @Test
    void search_returnsMatchingRegions() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<DeliveryRegion> page = new PageImpl<>(List.of(region), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.searchByTenantId("Tash", TENANT_ID, pageable)).thenReturn(page);
        when(regionMapper.toDto(region)).thenReturn(regionDTO);

        // When
        var result = deliveryRegionService.search("Tash", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ====== findById ======

    @Test
    void findById_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(region));
        when(regionMapper.toDto(region)).thenReturn(regionDTO);

        // When
        DeliveryRegionDTO result = deliveryRegionService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Tashkent", result.getName());
    }

    @Test
    void findById_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> deliveryRegionService.findById(999L));
    }

    // ====== findActive ======

    @Test
    void findActive_returnsActiveRegions() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findActiveByTenantId(TENANT_ID)).thenReturn(List.of(region));
        when(regionMapper.toDtoList(List.of(region))).thenReturn(List.of(regionDTO));

        // When
        List<DeliveryRegionDTO> result = deliveryRegionService.findActive();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    // ====== create ======

    @Test
    void create_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.existsByCodeAndTenantId("TASH", TENANT_ID, null)).thenReturn(false);
        when(regionMapper.toEntity(regionDTO)).thenReturn(region);
        when(regionRepository.save(any(DeliveryRegion.class))).thenReturn(region);
        when(regionMapper.toDto(region)).thenReturn(regionDTO);

        // When
        DeliveryRegionDTO result = deliveryRegionService.create(regionDTO);

        // Then
        assertNotNull(result);
        assertEquals("Tashkent", result.getName());
        verify(regionRepository).save(any(DeliveryRegion.class));
    }

    @Test
    void create_duplicateCode_throwsDuplicateResourceException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.existsByCodeAndTenantId("TASH", TENANT_ID, null)).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> deliveryRegionService.create(regionDTO));
        verify(regionRepository, never()).save(any());
    }

    @Test
    void create_nullCode_skipsUniquenessCheck() {
        // Given
        regionDTO.setCode(null);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionMapper.toEntity(regionDTO)).thenReturn(region);
        when(regionRepository.save(any(DeliveryRegion.class))).thenReturn(region);
        when(regionMapper.toDto(region)).thenReturn(regionDTO);

        // When
        DeliveryRegionDTO result = deliveryRegionService.create(regionDTO);

        // Then
        assertNotNull(result);
        verify(regionRepository, never()).existsByCodeAndTenantId(any(), any(), any());
    }

    // ====== update ======

    @Test
    void update_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(region));
        when(regionRepository.existsByCodeAndTenantId("TASH", TENANT_ID, 1L)).thenReturn(false);
        when(regionRepository.save(any(DeliveryRegion.class))).thenReturn(region);
        when(regionMapper.toDto(any(DeliveryRegion.class))).thenReturn(regionDTO);

        // When
        DeliveryRegionDTO result = deliveryRegionService.update(1L, regionDTO);

        // Then
        assertNotNull(result);
        verify(regionMapper).updateEntity(eq(regionDTO), any(DeliveryRegion.class));
        verify(regionRepository).save(any(DeliveryRegion.class));
    }

    @Test
    void update_duplicateCode_throwsDuplicateResourceException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(region));
        when(regionRepository.existsByCodeAndTenantId("TASH", TENANT_ID, 1L)).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> deliveryRegionService.update(1L, regionDTO));
        verify(regionRepository, never()).save(any());
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> deliveryRegionService.update(999L, regionDTO));
    }

    // ====== delete ======

    @Test
    void delete_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(region));
        when(villageRepository.countByRegionId(1L)).thenReturn(0L);

        // When
        deliveryRegionService.delete(1L);

        // Then
        verify(regionRepository).delete(region);
    }

    @Test
    void delete_withVillages_throwsIllegalStateException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(region));
        when(villageRepository.countByRegionId(1L)).thenReturn(5L);

        // When/Then
        assertThrows(IllegalStateException.class, () -> deliveryRegionService.delete(1L));
        verify(regionRepository, never()).delete(any());
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> deliveryRegionService.delete(999L));
    }
}
