package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.BrandDto;
import com.hisobnoma.platform.inventory.dto.CreateBrandRequest;
import com.hisobnoma.platform.inventory.dto.UpdateBrandRequest;
import com.hisobnoma.platform.inventory.entity.Brand;
import com.hisobnoma.platform.inventory.mapper.BrandMapper;
import com.hisobnoma.platform.inventory.repository.BrandRepository;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrandServiceTest {

    @Mock
    private BrandRepository brandRepository;
    @Mock
    private BrandMapper brandMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private BrandService brandService;

    private static final Long TENANT_ID = 1L;
    private Brand brand;
    private BrandDto brandDto;

    @BeforeEach
    void setUp() {
        brand = Brand.builder()
                .id(1L)
                .code("SAMSUNG")
                .name("Samsung")
                .description("Samsung Electronics")
                .active(true)
                .sortOrder(0)
                .tenantId(TENANT_ID)
                .build();

        brandDto = BrandDto.builder()
                .id(1L)
                .code("SAMSUNG")
                .name("Samsung")
                .description("Samsung Electronics")
                .active(true)
                .sortOrder(0)
                .build();
    }

    @Test
    void getBrands_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Brand> page = new PageImpl<>(List.of(brand), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(brandRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(brandMapper.toDto(brand)).thenReturn(brandDto);

        // When
        PageResponse<BrandDto> result = brandService.getBrands(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("Samsung", result.getContent().get(0).getName());
    }

    @Test
    void getAllBrands_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(brandRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(brandMapper.toDtoList(List.of(brand))).thenReturn(List.of(brandDto));

        // When
        List<BrandDto> result = brandService.getAllBrands();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getActiveBrands_returnsActiveOnly() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(brandRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(brand));
        when(brandMapper.toDtoList(List.of(brand))).thenReturn(List.of(brandDto));

        // When
        List<BrandDto> result = brandService.getActiveBrands();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void getBrand_found_returnsDto() {
        // Given
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandMapper.toDto(brand)).thenReturn(brandDto);

        // When
        BrandDto result = brandService.getBrand(1L);

        // Then
        assertNotNull(result);
        assertEquals("Samsung", result.getName());
    }

    @Test
    void getBrand_notFound_throwsNotFoundException() {
        // Given
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> brandService.getBrand(999L));
    }

    @Test
    void searchBrands_returnsMatches() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Brand> page = new PageImpl<>(List.of(brand), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(brandRepository.searchByTenantId(TENANT_ID, "Sam", pageable)).thenReturn(page);
        when(brandMapper.toDto(brand)).thenReturn(brandDto);

        // When
        PageResponse<BrandDto> result = brandService.searchBrands("Sam", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createBrand_success() {
        // Given
        CreateBrandRequest request = CreateBrandRequest.builder()
                .code("APPLE")
                .name("Apple")
                .description("Apple Inc.")
                .active(true)
                .build();

        Brand newBrand = Brand.builder()
                .id(2L)
                .code("APPLE")
                .name("Apple")
                .tenantId(TENANT_ID)
                .build();

        BrandDto newBrandDto = BrandDto.builder()
                .id(2L)
                .code("APPLE")
                .name("Apple")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(brandRepository.existsByCodeAndTenantId("APPLE", TENANT_ID)).thenReturn(false);
        when(brandMapper.toEntity(request)).thenReturn(newBrand);
        when(brandRepository.save(any(Brand.class))).thenReturn(newBrand);
        when(brandMapper.toDto(newBrand)).thenReturn(newBrandDto);

        // When
        BrandDto result = brandService.createBrand(request);

        // Then
        assertNotNull(result);
        assertEquals("Apple", result.getName());
        verify(brandRepository).save(any(Brand.class));
    }

    @Test
    void createBrand_duplicateCode_throwsDuplicateResourceException() {
        // Given
        CreateBrandRequest request = CreateBrandRequest.builder()
                .code("SAMSUNG")
                .name("Samsung")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(brandRepository.existsByCodeAndTenantId("SAMSUNG", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> brandService.createBrand(request));
        verify(brandRepository, never()).save(any());
    }

    @Test
    void updateBrand_success() {
        // Given
        UpdateBrandRequest request = UpdateBrandRequest.builder()
                .name("Samsung Updated")
                .description("Updated description")
                .build();

        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));
        when(brandRepository.save(any(Brand.class))).thenReturn(brand);
        when(brandMapper.toDto(brand)).thenReturn(brandDto);

        // When
        BrandDto result = brandService.updateBrand(1L, request);

        // Then
        assertNotNull(result);
        verify(brandRepository).save(brand);
    }

    @Test
    void updateBrand_notFound_throwsNotFoundException() {
        // Given
        UpdateBrandRequest request = UpdateBrandRequest.builder()
                .name("Updated")
                .build();

        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> brandService.updateBrand(999L, request));
    }

    @Test
    void deleteBrand_success() {
        // Given
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        // When
        brandService.deleteBrand(1L);

        // Then
        verify(brandRepository).delete(brand);
    }

    @Test
    void deleteBrand_notFound_throwsNotFoundException() {
        // Given
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> brandService.deleteBrand(999L));
    }
}
