package com.hisobnoma.platform.delivery.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.delivery.dto.DeliveryVillageDTO;
import com.hisobnoma.platform.delivery.entity.DeliveryRegion;
import com.hisobnoma.platform.delivery.entity.DeliveryVillage;
import com.hisobnoma.platform.delivery.mapper.DeliveryVillageMapper;
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
class DeliveryVillageServiceTest {

    @Mock
    private DeliveryVillageRepository villageRepository;

    @Mock
    private DeliveryRegionRepository regionRepository;

    @Mock
    private DeliveryVillageMapper villageMapper;

    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private DeliveryVillageService deliveryVillageService;

    private DeliveryRegion region;
    private DeliveryVillage village;
    private DeliveryVillageDTO villageDTO;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        region = DeliveryRegion.builder()
                .id(1L)
                .name("Tashkent")
                .code("TASH")
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        village = DeliveryVillage.builder()
                .id(1L)
                .name("Yunusabad")
                .code("YUN")
                .description("Yunusabad district")
                .active(true)
                .sortOrder(1)
                .region(region)
                .tenantId(TENANT_ID)
                .build();

        villageDTO = DeliveryVillageDTO.builder()
                .id(1L)
                .regionId(1L)
                .regionName("Tashkent")
                .name("Yunusabad")
                .code("YUN")
                .description("Yunusabad district")
                .active(true)
                .sortOrder(1)
                .build();
    }

    // ====== findAll ======

    @Test
    void findAll_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<DeliveryVillage> page = new PageImpl<>(List.of(village), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(villageMapper.toDto(village)).thenReturn(villageDTO);

        // When
        var result = deliveryVillageService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Yunusabad", result.getContent().get(0).getName());
    }

    // ====== search ======

    @Test
    void search_returnsMatchingVillages() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<DeliveryVillage> page = new PageImpl<>(List.of(village), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.searchByTenantId("Yunus", TENANT_ID, pageable)).thenReturn(page);
        when(villageMapper.toDto(village)).thenReturn(villageDTO);

        // When
        var result = deliveryVillageService.search("Yunus", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ====== findById ======

    @Test
    void findById_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(village));
        when(villageMapper.toDto(village)).thenReturn(villageDTO);

        // When
        DeliveryVillageDTO result = deliveryVillageService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Yunusabad", result.getName());
    }

    @Test
    void findById_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> deliveryVillageService.findById(999L));
    }

    // ====== findByRegion ======

    @Test
    void findByRegion_returnsVillages() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findActiveByRegionIdAndTenantId(1L, TENANT_ID)).thenReturn(List.of(village));
        when(villageMapper.toDtoList(List.of(village))).thenReturn(List.of(villageDTO));

        // When
        List<DeliveryVillageDTO> result = deliveryVillageService.findByRegion(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getRegionId());
    }

    // ====== findActive ======

    @Test
    void findActive_returnsActiveVillages() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findActiveByTenantId(TENANT_ID)).thenReturn(List.of(village));
        when(villageMapper.toDtoList(List.of(village))).thenReturn(List.of(villageDTO));

        // When
        List<DeliveryVillageDTO> result = deliveryVillageService.findActive();

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
        when(regionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(region));
        when(villageRepository.existsByCodeAndTenantId("YUN", TENANT_ID, null)).thenReturn(false);
        when(villageMapper.toEntity(villageDTO)).thenReturn(village);
        when(villageRepository.save(any(DeliveryVillage.class))).thenReturn(village);
        when(villageMapper.toDto(village)).thenReturn(villageDTO);

        // When
        DeliveryVillageDTO result = deliveryVillageService.create(villageDTO);

        // Then
        assertNotNull(result);
        assertEquals("Yunusabad", result.getName());
        verify(villageRepository).save(any(DeliveryVillage.class));
    }

    @Test
    void create_regionNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> deliveryVillageService.create(villageDTO));
        verify(villageRepository, never()).save(any());
    }

    @Test
    void create_duplicateCode_throwsDuplicateResourceException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(regionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(region));
        when(villageRepository.existsByCodeAndTenantId("YUN", TENANT_ID, null)).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> deliveryVillageService.create(villageDTO));
        verify(villageRepository, never()).save(any());
    }

    // ====== update ======

    @Test
    void update_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(village));
        when(villageRepository.existsByCodeAndTenantId("YUN", TENANT_ID, 1L)).thenReturn(false);
        when(villageRepository.save(any(DeliveryVillage.class))).thenReturn(village);
        when(villageMapper.toDto(any(DeliveryVillage.class))).thenReturn(villageDTO);

        // When
        DeliveryVillageDTO result = deliveryVillageService.update(1L, villageDTO);

        // Then
        assertNotNull(result);
        verify(villageMapper).updateEntity(eq(villageDTO), any(DeliveryVillage.class));
        verify(villageRepository).save(any(DeliveryVillage.class));
    }

    @Test
    void update_changeRegion_success() {
        // Given
        DeliveryRegion newRegion = DeliveryRegion.builder()
                .id(2L)
                .name("Samarkand")
                .code("SAM")
                .tenantId(TENANT_ID)
                .build();

        DeliveryVillageDTO updateDTO = DeliveryVillageDTO.builder()
                .id(1L)
                .regionId(2L)
                .name("Yunusabad")
                .code("YUN")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(village));
        when(regionRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(newRegion));
        when(villageRepository.existsByCodeAndTenantId("YUN", TENANT_ID, 1L)).thenReturn(false);
        when(villageRepository.save(any(DeliveryVillage.class))).thenReturn(village);
        when(villageMapper.toDto(any(DeliveryVillage.class))).thenReturn(villageDTO);

        // When
        DeliveryVillageDTO result = deliveryVillageService.update(1L, updateDTO);

        // Then
        assertNotNull(result);
        verify(regionRepository).findByIdAndTenantId(2L, TENANT_ID);
    }

    @Test
    void update_changeRegionNotFound_throwsNotFoundException() {
        // Given
        DeliveryVillageDTO updateDTO = DeliveryVillageDTO.builder()
                .id(1L)
                .regionId(999L)
                .name("Yunusabad")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(village));
        when(regionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> deliveryVillageService.update(1L, updateDTO));
    }

    @Test
    void update_duplicateCode_throwsDuplicateResourceException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(village));
        when(villageRepository.existsByCodeAndTenantId("YUN", TENANT_ID, 1L)).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> deliveryVillageService.update(1L, villageDTO));
        verify(villageRepository, never()).save(any());
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> deliveryVillageService.update(999L, villageDTO));
    }

    // ====== delete ======

    @Test
    void delete_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(village));

        // When
        deliveryVillageService.delete(1L);

        // Then
        verify(villageRepository).delete(village);
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(villageRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> deliveryVillageService.delete(999L));
    }
}
