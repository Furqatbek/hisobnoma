package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.common.exception.ValidationException;
import com.hisobnoma.platform.inventory.dto.CreateUomRequest;
import com.hisobnoma.platform.inventory.dto.UnitOfMeasureDto;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.mapper.UnitOfMeasureMapper;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnitOfMeasureServiceTest {

    @Mock
    private UnitOfMeasureRepository uomRepository;
    @Mock
    private UnitOfMeasureMapper uomMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private UnitOfMeasureService uomService;

    private static final Long TENANT_ID = 1L;
    private UnitOfMeasure uom;
    private UnitOfMeasureDto uomDto;

    @BeforeEach
    void setUp() {
        uom = UnitOfMeasure.builder()
                .id(1L)
                .code("KG")
                .name("Kilogram")
                .symbol("kg")
                .conversionFactor(BigDecimal.ONE)
                .active(true)
                .isBaseUnit(true)
                .tenantId(TENANT_ID)
                .build();

        uomDto = UnitOfMeasureDto.builder()
                .id(1L)
                .code("KG")
                .name("Kilogram")
                .symbol("kg")
                .active(true)
                .build();
    }

    @Test
    void getAllUoms_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(uomRepository.findAllByTenantId(TENANT_ID)).thenReturn(List.of(uom));
        when(uomMapper.toDtoList(List.of(uom))).thenReturn(List.of(uomDto));

        // When
        List<UnitOfMeasureDto> result = uomService.getAllUoms();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getUoms_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<UnitOfMeasure> page = new PageImpl<>(List.of(uom), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(uomRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(uomMapper.toDto(uom)).thenReturn(uomDto);

        // When
        PageResponse<UnitOfMeasureDto> result = uomService.getUoms(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getUom_found_returnsDto() {
        // Given
        when(uomRepository.findById(1L)).thenReturn(Optional.of(uom));
        when(uomMapper.toDto(uom)).thenReturn(uomDto);

        // When
        UnitOfMeasureDto result = uomService.getUom(1L);

        // Then
        assertNotNull(result);
        assertEquals("Kilogram", result.getName());
    }

    @Test
    void getUom_notFound_throwsNotFoundException() {
        // Given
        when(uomRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> uomService.getUom(999L));
    }

    @Test
    void createUom_success() {
        // Given
        CreateUomRequest request = CreateUomRequest.builder()
                .code("G")
                .name("Gram")
                .symbol("g")
                .active(true)
                .build();

        UnitOfMeasure newUom = UnitOfMeasure.builder()
                .id(2L)
                .code("G")
                .name("Gram")
                .symbol("g")
                .tenantId(TENANT_ID)
                .build();

        UnitOfMeasureDto newUomDto = UnitOfMeasureDto.builder()
                .id(2L)
                .code("G")
                .name("Gram")
                .symbol("g")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(uomRepository.existsByCodeAndTenantId("G", TENANT_ID)).thenReturn(false);
        when(uomMapper.toEntity(request)).thenReturn(newUom);
        when(uomRepository.save(any(UnitOfMeasure.class))).thenReturn(newUom);
        when(uomMapper.toDto(newUom)).thenReturn(newUomDto);

        // When
        UnitOfMeasureDto result = uomService.createUom(request);

        // Then
        assertNotNull(result);
        assertEquals("Gram", result.getName());
        verify(uomRepository).save(any(UnitOfMeasure.class));
    }

    @Test
    void createUom_duplicateCode_throwsDuplicateResourceException() {
        // Given
        CreateUomRequest request = CreateUomRequest.builder()
                .code("KG")
                .name("Kilogram Dup")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(uomRepository.existsByCodeAndTenantId("KG", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> uomService.createUom(request));
        verify(uomRepository, never()).save(any());
    }

    @Test
    void createUom_withBaseUom_success() {
        // Given
        CreateUomRequest request = CreateUomRequest.builder()
                .code("G")
                .name("Gram")
                .symbol("g")
                .baseUomId(1L)
                .conversionFactor(new BigDecimal("0.001"))
                .active(true)
                .build();

        UnitOfMeasure newUom = UnitOfMeasure.builder()
                .id(2L)
                .code("G")
                .name("Gram")
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(uomRepository.existsByCodeAndTenantId("G", TENANT_ID)).thenReturn(false);
        when(uomMapper.toEntity(request)).thenReturn(newUom);
        when(uomRepository.findById(1L)).thenReturn(Optional.of(uom));
        when(uomRepository.save(any(UnitOfMeasure.class))).thenReturn(newUom);
        when(uomMapper.toDto(newUom)).thenReturn(uomDto);

        // When
        UnitOfMeasureDto result = uomService.createUom(request);

        // Then
        assertNotNull(result);
    }

    @Test
    void createUom_baseUomNotFound_throwsNotFoundException() {
        // Given
        CreateUomRequest request = CreateUomRequest.builder()
                .code("G")
                .name("Gram")
                .baseUomId(999L)
                .active(true)
                .build();

        UnitOfMeasure newUom = UnitOfMeasure.builder()
                .id(2L)
                .code("G")
                .name("Gram")
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(uomRepository.existsByCodeAndTenantId("G", TENANT_ID)).thenReturn(false);
        when(uomMapper.toEntity(request)).thenReturn(newUom);
        when(uomRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> uomService.createUom(request));
    }

    @Test
    void updateUom_success() {
        // Given
        CreateUomRequest request = CreateUomRequest.builder()
                .code("KG")
                .name("Updated Kilogram")
                .symbol("kg")
                .active(true)
                .build();

        when(uomRepository.findById(1L)).thenReturn(Optional.of(uom));
        when(uomRepository.save(any(UnitOfMeasure.class))).thenReturn(uom);
        when(uomMapper.toDto(uom)).thenReturn(uomDto);

        // When
        UnitOfMeasureDto result = uomService.updateUom(1L, request);

        // Then
        assertNotNull(result);
        verify(uomRepository).save(uom);
    }

    @Test
    void updateUom_notFound_throwsNotFoundException() {
        // Given
        CreateUomRequest request = CreateUomRequest.builder()
                .code("XX")
                .name("Unknown")
                .active(true)
                .build();

        when(uomRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> uomService.updateUom(999L, request));
    }

    @Test
    void deleteUom_success() {
        // Given
        when(uomRepository.findById(1L)).thenReturn(Optional.of(uom));
        when(uomRepository.findByBaseUomId(1L)).thenReturn(Collections.emptyList());

        // When
        uomService.deleteUom(1L);

        // Then
        verify(uomRepository).delete(uom);
    }

    @Test
    void deleteUom_usedByOthers_throwsValidationException() {
        // Given
        UnitOfMeasure dependentUom = UnitOfMeasure.builder().id(2L).code("G").build();
        when(uomRepository.findById(1L)).thenReturn(Optional.of(uom));
        when(uomRepository.findByBaseUomId(1L)).thenReturn(List.of(dependentUom));

        // When/Then
        assertThrows(ValidationException.class, () -> uomService.deleteUom(1L));
        verify(uomRepository, never()).delete(any());
    }

    @Test
    void deleteUom_notFound_throwsNotFoundException() {
        // Given
        when(uomRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> uomService.deleteUom(999L));
    }

    @Test
    void convert_sameUom_returnsOriginalQuantity() {
        // When
        BigDecimal result = uomService.convert(new BigDecimal("10"), 1L, 1L);

        // Then
        assertEquals(new BigDecimal("10"), result);
    }

    @Test
    void convert_differentUoms_convertsCorrectly() {
        // Given
        UnitOfMeasure gramUom = UnitOfMeasure.builder()
                .id(2L)
                .code("G")
                .name("Gram")
                .baseUom(uom)
                .conversionFactor(new BigDecimal("0.001"))
                .isBaseUnit(false)
                .build();

        when(uomRepository.findById(1L)).thenReturn(Optional.of(uom));
        when(uomRepository.findById(2L)).thenReturn(Optional.of(gramUom));

        // When
        BigDecimal result = uomService.convert(new BigDecimal("1"), 1L, 2L);

        // Then
        assertNotNull(result);
    }

    @Test
    void convert_fromUomNotFound_throwsNotFoundException() {
        // Given
        when(uomRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> uomService.convert(BigDecimal.ONE, 999L, 1L));
    }

    @Test
    void getActiveUoms_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(uomRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(uom));
        when(uomMapper.toDtoList(List.of(uom))).thenReturn(List.of(uomDto));

        // When
        List<UnitOfMeasureDto> result = uomService.getActiveUoms();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getBaseUoms_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(uomRepository.findAllBaseUnitsByTenantId(TENANT_ID)).thenReturn(List.of(uom));
        when(uomMapper.toDtoList(List.of(uom))).thenReturn(List.of(uomDto));

        // When
        List<UnitOfMeasureDto> result = uomService.getBaseUoms();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
