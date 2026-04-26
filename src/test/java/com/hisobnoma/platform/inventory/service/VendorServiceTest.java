package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.CreateVendorRequest;
import com.hisobnoma.platform.inventory.dto.VendorDto;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.mapper.VendorMapper;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private VendorMapper vendorMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private VendorService vendorService;

    private static final Long TENANT_ID = 1L;
    private Vendor vendor;
    private VendorDto vendorDto;

    @BeforeEach
    void setUp() {
        vendor = Vendor.builder()
                .id(1L)
                .code("VND-001")
                .name("Acme Supplies")
                .email("info@acme.com")
                .phone("+998901234567")
                .active(true)
                .currentBalance(BigDecimal.ZERO)
                .tenantId(TENANT_ID)
                .build();

        vendorDto = VendorDto.builder()
                .id(1L)
                .code("VND-001")
                .name("Acme Supplies")
                .email("info@acme.com")
                .active(true)
                .build();
    }

    @Test
    void getVendors_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vendor> page = new PageImpl<>(List.of(vendor), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(vendorMapper.toDto(vendor)).thenReturn(vendorDto);

        // When
        PageResponse<VendorDto> result = vendorService.getVendors(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void getActiveVendors_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findAllActiveByTenantId(TENANT_ID)).thenReturn(List.of(vendor));
        when(vendorMapper.toDtoList(List.of(vendor))).thenReturn(List.of(vendorDto));

        // When
        List<VendorDto> result = vendorService.getActiveVendors();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getVendor_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(vendorMapper.toDto(vendor)).thenReturn(vendorDto);

        // When
        VendorDto result = vendorService.getVendor(1L);

        // Then
        assertNotNull(result);
        assertEquals("Acme Supplies", result.getName());
    }

    @Test
    void getVendor_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> vendorService.getVendor(999L));
    }

    @Test
    void getVendorByCode_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByCodeAndTenantId("VND-001", TENANT_ID)).thenReturn(Optional.of(vendor));
        when(vendorMapper.toDto(vendor)).thenReturn(vendorDto);

        // When
        VendorDto result = vendorService.getVendorByCode("VND-001");

        // Then
        assertNotNull(result);
        assertEquals("VND-001", result.getCode());
    }

    @Test
    void getVendorByCode_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByCodeAndTenantId("MISSING", TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> vendorService.getVendorByCode("MISSING"));
    }

    @Test
    void searchVendors_returnsMatches() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Vendor> page = new PageImpl<>(List.of(vendor), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.searchByTenantId(TENANT_ID, "Acme", pageable)).thenReturn(page);
        when(vendorMapper.toDto(vendor)).thenReturn(vendorDto);

        // When
        PageResponse<VendorDto> result = vendorService.searchVendors("Acme", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void createVendor_success() {
        // Given
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-002")
                .name("New Vendor")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.existsByCodeAndTenantId("VND-002", TENANT_ID)).thenReturn(false);
        when(vendorMapper.toEntity(request)).thenReturn(vendor);
        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);
        when(vendorMapper.toDto(vendor)).thenReturn(vendorDto);

        // When
        VendorDto result = vendorService.createVendor(request);

        // Then
        assertNotNull(result);
        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void createVendor_duplicateCode_throwsBusinessException() {
        // Given
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-001")
                .name("Duplicate Vendor")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.existsByCodeAndTenantId("VND-001", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> vendorService.createVendor(request));
        verify(vendorRepository, never()).save(any());
    }

    @Test
    void updateVendor_success() {
        // Given
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-001")
                .name("Updated Vendor")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);
        when(vendorMapper.toDto(vendor)).thenReturn(vendorDto);

        // When
        VendorDto result = vendorService.updateVendor(1L, request);

        // Then
        assertNotNull(result);
        verify(vendorRepository).save(any(Vendor.class));
    }

    @Test
    void updateVendor_notFound_throwsNotFoundException() {
        // Given
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-999")
                .name("Updated")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> vendorService.updateVendor(999L, request));
    }

    @Test
    void updateVendor_duplicateCode_throwsBusinessException() {
        // Given
        CreateVendorRequest request = CreateVendorRequest.builder()
                .code("VND-002")
                .name("Updated")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(vendorRepository.existsByCodeAndTenantId("VND-002", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> vendorService.updateVendor(1L, request));
    }

    @Test
    void deleteVendor_success_softDeletes() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));

        // When
        vendorService.deleteVendor(1L);

        // Then
        assertFalse(vendor.isActive());
        verify(vendorRepository).save(vendor);
    }

    @Test
    void deleteVendor_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> vendorService.deleteVendor(999L));
    }

    @Test
    void activateVendor_success() {
        // Given
        vendor.setActive(false);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(vendorRepository.save(any(Vendor.class))).thenReturn(vendor);
        when(vendorMapper.toDto(vendor)).thenReturn(vendorDto);

        // When
        VendorDto result = vendorService.activateVendor(1L);

        // Then
        assertNotNull(result);
        assertTrue(vendor.isActive());
    }

    @Test
    void getPreferredVendors_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(vendorRepository.findPreferredVendorsByTenantId(TENANT_ID)).thenReturn(List.of(vendor));
        when(vendorMapper.toDtoList(List.of(vendor))).thenReturn(List.of(vendorDto));

        // When
        List<VendorDto> result = vendorService.getPreferredVendors();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
