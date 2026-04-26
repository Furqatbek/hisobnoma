package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.CreateProductVendorRequest;
import com.hisobnoma.platform.inventory.dto.ProductVendorDto;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVendor;
import com.hisobnoma.platform.inventory.entity.Vendor;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductVendorRepository;
import com.hisobnoma.platform.inventory.repository.VendorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductVendorServiceTest {

    @Mock
    private ProductVendorRepository productVendorRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ProductVendorService productVendorService;

    private static final Long TENANT_ID = 1L;
    private Product product;
    private Vendor vendor;
    private ProductVendor productVendor;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .sku("PRD-001")
                .name("Test Product")
                .sellingPrice(new BigDecimal("100.00"))
                .tenantId(TENANT_ID)
                .build();

        vendor = Vendor.builder()
                .id(1L)
                .code("VND-001")
                .name("Acme Supplies")
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        productVendor = ProductVendor.builder()
                .id(1L)
                .tenantId(TENANT_ID)
                .product(product)
                .vendor(vendor)
                .vendorSku("ACM-001")
                .vendorProductName("Acme Test Product")
                .unitCost(new BigDecimal("80.00"))
                .minOrderQuantity(new BigDecimal("10"))
                .leadTimeDays(7)
                .preferred(true)
                .active(true)
                .notes("Test notes")
                .build();
    }

    @Test
    void getVendorsForProduct_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productVendorRepository.findByTenantIdAndProductId(TENANT_ID, 1L)).thenReturn(List.of(productVendor));

        // When
        List<ProductVendorDto> result = productVendorService.getVendorsForProduct(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("ACM-001", result.get(0).getVendorSku());
        assertEquals("Acme Supplies", result.get(0).getVendorName());
    }

    @Test
    void getProductsForVendor_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productVendorRepository.findByTenantIdAndVendorId(TENANT_ID, 1L)).thenReturn(List.of(productVendor));

        // When
        List<ProductVendorDto> result = productVendorService.getProductsForVendor(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("PRD-001", result.get(0).getProductSku());
    }

    @Test
    void addVendorToProduct_success() {
        // Given
        CreateProductVendorRequest request = CreateProductVendorRequest.builder()
                .vendorId(1L)
                .vendorSku("ACM-001")
                .vendorProductName("Acme Test Product")
                .unitCost(new BigDecimal("80.00"))
                .minOrderQuantity(new BigDecimal("10"))
                .leadTimeDays(7)
                .preferred(false)
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(productVendorRepository.existsByTenantIdAndProductIdAndVendorId(TENANT_ID, 1L, 1L)).thenReturn(false);
        when(productVendorRepository.save(any(ProductVendor.class))).thenReturn(productVendor);

        // When
        ProductVendorDto result = productVendorService.addVendorToProduct(1L, request);

        // Then
        assertNotNull(result);
        assertEquals("ACM-001", result.getVendorSku());
        verify(productVendorRepository).save(any(ProductVendor.class));
    }

    @Test
    void addVendorToProduct_productNotFound_throwsNotFoundException() {
        // Given
        CreateProductVendorRequest request = CreateProductVendorRequest.builder()
                .vendorId(1L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productVendorService.addVendorToProduct(999L, request));
        verify(productVendorRepository, never()).save(any());
    }

    @Test
    void addVendorToProduct_vendorNotFound_throwsNotFoundException() {
        // Given
        CreateProductVendorRequest request = CreateProductVendorRequest.builder()
                .vendorId(999L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(vendorRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productVendorService.addVendorToProduct(1L, request));
        verify(productVendorRepository, never()).save(any());
    }

    @Test
    void addVendorToProduct_alreadyLinked_throwsBusinessException() {
        // Given
        CreateProductVendorRequest request = CreateProductVendorRequest.builder()
                .vendorId(1L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(productVendorRepository.existsByTenantIdAndProductIdAndVendorId(TENANT_ID, 1L, 1L)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> productVendorService.addVendorToProduct(1L, request));
        verify(productVendorRepository, never()).save(any());
    }

    @Test
    void addVendorToProduct_asPreferred_clearsExistingPreferred() {
        // Given
        CreateProductVendorRequest request = CreateProductVendorRequest.builder()
                .vendorId(1L)
                .preferred(true)
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(vendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(vendor));
        when(productVendorRepository.existsByTenantIdAndProductIdAndVendorId(TENANT_ID, 1L, 1L)).thenReturn(false);
        when(productVendorRepository.save(any(ProductVendor.class))).thenReturn(productVendor);

        // When
        ProductVendorDto result = productVendorService.addVendorToProduct(1L, request);

        // Then
        assertNotNull(result);
        verify(productVendorRepository).clearPreferredForProduct(TENANT_ID, 1L);
    }

    @Test
    void updateProductVendor_success() {
        // Given
        CreateProductVendorRequest request = CreateProductVendorRequest.builder()
                .vendorId(1L)
                .vendorSku("ACM-002")
                .unitCost(new BigDecimal("85.00"))
                .preferred(false)
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productVendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productVendor));
        when(productVendorRepository.save(any(ProductVendor.class))).thenReturn(productVendor);

        // When
        ProductVendorDto result = productVendorService.updateProductVendor(1L, 1L, request);

        // Then
        assertNotNull(result);
        verify(productVendorRepository).save(any(ProductVendor.class));
    }

    @Test
    void updateProductVendor_notFound_throwsNotFoundException() {
        // Given
        CreateProductVendorRequest request = CreateProductVendorRequest.builder()
                .vendorId(1L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productVendorRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productVendorService.updateProductVendor(1L, 999L, request));
    }

    @Test
    void updateProductVendor_wrongProduct_throwsBusinessException() {
        // Given
        CreateProductVendorRequest request = CreateProductVendorRequest.builder()
                .vendorId(1L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productVendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productVendor));

        // When/Then
        assertThrows(BusinessException.class, () -> productVendorService.updateProductVendor(999L, 1L, request));
    }

    @Test
    void updateProductVendor_setAsPreferred_clearsExisting() {
        // Given
        productVendor.setPreferred(false);

        CreateProductVendorRequest request = CreateProductVendorRequest.builder()
                .vendorId(1L)
                .preferred(true)
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productVendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productVendor));
        when(productVendorRepository.save(any(ProductVendor.class))).thenReturn(productVendor);

        // When
        ProductVendorDto result = productVendorService.updateProductVendor(1L, 1L, request);

        // Then
        assertNotNull(result);
        verify(productVendorRepository).clearPreferredForProduct(TENANT_ID, 1L);
    }

    @Test
    void removeVendorFromProduct_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productVendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productVendor));

        // When
        productVendorService.removeVendorFromProduct(1L, 1L);

        // Then
        verify(productVendorRepository).delete(productVendor);
    }

    @Test
    void removeVendorFromProduct_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productVendorRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productVendorService.removeVendorFromProduct(1L, 999L));
    }

    @Test
    void removeVendorFromProduct_wrongProduct_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productVendorRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productVendor));

        // When/Then
        assertThrows(BusinessException.class, () -> productVendorService.removeVendorFromProduct(999L, 1L));
    }
}
