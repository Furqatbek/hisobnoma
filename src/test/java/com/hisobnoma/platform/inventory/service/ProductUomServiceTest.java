package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.CreateProductUomRequest;
import com.hisobnoma.platform.inventory.dto.ProductUomDto;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductUom;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
import com.hisobnoma.platform.inventory.mapper.ProductUomMapper;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.inventory.repository.ProductUomRepository;
import com.hisobnoma.platform.inventory.repository.UnitOfMeasureRepository;
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
class ProductUomServiceTest {

    @Mock
    private ProductUomRepository productUomRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private UnitOfMeasureRepository uomRepository;
    @Mock
    private ProductUomMapper productUomMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ProductUomService productUomService;

    private static final Long TENANT_ID = 1L;
    private Product product;
    private UnitOfMeasure baseUom;
    private UnitOfMeasure alternateUom;
    private ProductUom productUom;
    private ProductUomDto productUomDto;

    @BeforeEach
    void setUp() {
        baseUom = UnitOfMeasure.builder()
                .id(1L)
                .code("KG")
                .name("Kilogram")
                .tenantId(TENANT_ID)
                .build();

        alternateUom = UnitOfMeasure.builder()
                .id(2L)
                .code("TON")
                .name("Tonne")
                .tenantId(TENANT_ID)
                .build();

        product = Product.builder()
                .id(1L)
                .sku("PRD-001")
                .name("Sand")
                .baseUom(baseUom)
                .sellingPrice(new BigDecimal("100.00"))
                .tenantId(TENANT_ID)
                .build();

        productUom = ProductUom.builder()
                .id(1L)
                .product(product)
                .uom(alternateUom)
                .conversionFactor(new BigDecimal("1000"))
                .sellingPrice(new BigDecimal("90000.00"))
                .defaultSale(false)
                .active(true)
                .sortOrder(0)
                .tenantId(TENANT_ID)
                .build();

        productUomDto = ProductUomDto.builder()
                .id(1L)
                .productId(1L)
                .productName("Sand")
                .uomId(2L)
                .uomCode("TON")
                .uomName("Tonne")
                .conversionFactor(new BigDecimal("1000"))
                .sellingPrice(new BigDecimal("90000.00"))
                .defaultSale(false)
                .active(true)
                .sortOrder(0)
                .build();
    }

    @Test
    void getByProduct_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByProductIdAndTenantIdOrderBySortOrderAsc(1L, TENANT_ID)).thenReturn(List.of(productUom));
        when(productUomMapper.toDtoList(List.of(productUom))).thenReturn(List.of(productUomDto));

        // When
        List<ProductUomDto> result = productUomService.getByProduct(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("TON", result.get(0).getUomCode());
    }

    @Test
    void getActiveByProduct_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByProductIdAndActiveTrueAndTenantIdOrderBySortOrderAsc(1L, TENANT_ID)).thenReturn(List.of(productUom));
        when(productUomMapper.toDtoList(List.of(productUom))).thenReturn(List.of(productUomDto));

        // When
        List<ProductUomDto> result = productUomService.getActiveByProduct(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void create_success() {
        // Given
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("1000"))
                .sellingPrice(new BigDecimal("90000.00"))
                .defaultSale(false)
                .active(true)
                .sortOrder(0)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(uomRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(alternateUom));
        when(productUomRepository.existsByProductIdAndUomIdAndTenantId(1L, 2L, TENANT_ID)).thenReturn(false);
        when(productUomRepository.save(any(ProductUom.class))).thenReturn(productUom);
        when(productUomMapper.toDto(productUom)).thenReturn(productUomDto);

        // When
        ProductUomDto result = productUomService.create(1L, request);

        // Then
        assertNotNull(result);
        assertEquals("TON", result.getUomCode());
        verify(productUomRepository).save(any(ProductUom.class));
    }

    @Test
    void create_productNotFound_throwsNotFoundException() {
        // Given
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("1000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productUomService.create(999L, request));
        verify(productUomRepository, never()).save(any());
    }

    @Test
    void create_uomNotFound_throwsNotFoundException() {
        // Given
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(999L)
                .conversionFactor(new BigDecimal("1000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(uomRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productUomService.create(1L, request));
        verify(productUomRepository, never()).save(any());
    }

    @Test
    void create_baseUomAsAlternate_throwsBusinessException() {
        // Given
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(1L)
                .conversionFactor(BigDecimal.ONE)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(uomRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(baseUom));

        // When/Then
        assertThrows(BusinessException.class, () -> productUomService.create(1L, request));
        verify(productUomRepository, never()).save(any());
    }

    @Test
    void create_duplicateUom_throwsBusinessException() {
        // Given
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("1000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(uomRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(alternateUom));
        when(productUomRepository.existsByProductIdAndUomIdAndTenantId(1L, 2L, TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> productUomService.create(1L, request));
        verify(productUomRepository, never()).save(any());
    }

    @Test
    void create_asDefaultSale_clearsExistingDefault() {
        // Given
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("1000"))
                .defaultSale(true)
                .active(true)
                .build();

        ProductUom existingDefault = ProductUom.builder()
                .id(10L)
                .product(product)
                .uom(alternateUom)
                .conversionFactor(new BigDecimal("500"))
                .defaultSale(true)
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(uomRepository.findByIdAndTenantId(2L, TENANT_ID)).thenReturn(Optional.of(alternateUom));
        when(productUomRepository.existsByProductIdAndUomIdAndTenantId(1L, 2L, TENANT_ID)).thenReturn(false);
        when(productUomRepository.findDefaultSaleUom(1L, TENANT_ID)).thenReturn(Optional.of(existingDefault));
        when(productUomRepository.save(any(ProductUom.class))).thenReturn(productUom);
        when(productUomMapper.toDto(productUom)).thenReturn(productUomDto);

        // When
        ProductUomDto result = productUomService.create(1L, request);

        // Then
        assertNotNull(result);
        assertFalse(existingDefault.isDefaultSale());
    }

    @Test
    void update_success() {
        // Given
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("1200"))
                .sellingPrice(new BigDecimal("100000.00"))
                .defaultSale(false)
                .active(true)
                .sortOrder(1)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productUom));
        when(productUomRepository.save(any(ProductUom.class))).thenReturn(productUom);
        when(productUomMapper.toDto(productUom)).thenReturn(productUomDto);

        // When
        ProductUomDto result = productUomService.update(1L, 1L, request);

        // Then
        assertNotNull(result);
        verify(productUomRepository).save(any(ProductUom.class));
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        // Given
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("1000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productUomService.update(1L, 999L, request));
    }

    @Test
    void update_wrongProduct_throwsBusinessException() {
        // Given
        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("1000"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productUom));

        // When/Then
        assertThrows(BusinessException.class, () -> productUomService.update(999L, 1L, request));
    }

    @Test
    void update_changingUomToDuplicate_throwsBusinessException() {
        // Given
        UnitOfMeasure newUom = UnitOfMeasure.builder()
                .id(3L)
                .code("LB")
                .name("Pound")
                .tenantId(TENANT_ID)
                .build();

        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(3L)
                .conversionFactor(new BigDecimal("2.2"))
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productUom));
        when(productUomRepository.existsByProductIdAndUomIdAndTenantId(1L, 3L, TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(BusinessException.class, () -> productUomService.update(1L, 1L, request));
    }

    @Test
    void update_setAsDefaultSale_clearsExisting() {
        // Given
        ProductUom existingDefault = ProductUom.builder()
                .id(10L)
                .product(product)
                .uom(alternateUom)
                .conversionFactor(new BigDecimal("500"))
                .defaultSale(true)
                .tenantId(TENANT_ID)
                .build();

        CreateProductUomRequest request = CreateProductUomRequest.builder()
                .uomId(2L)
                .conversionFactor(new BigDecimal("1000"))
                .defaultSale(true)
                .active(true)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productUom));
        when(productUomRepository.findDefaultSaleUom(1L, TENANT_ID)).thenReturn(Optional.of(existingDefault));
        when(productUomRepository.save(any(ProductUom.class))).thenReturn(productUom);
        when(productUomMapper.toDto(productUom)).thenReturn(productUomDto);

        // When
        ProductUomDto result = productUomService.update(1L, 1L, request);

        // Then
        assertNotNull(result);
        assertFalse(existingDefault.isDefaultSale());
    }

    @Test
    void delete_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productUom));

        // When
        productUomService.delete(1L, 1L);

        // Then
        verify(productUomRepository).delete(productUom);
    }

    @Test
    void delete_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productUomService.delete(1L, 999L));
    }

    @Test
    void delete_wrongProduct_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productUom));

        // When/Then
        assertThrows(BusinessException.class, () -> productUomService.delete(999L, 1L));
    }

    @Test
    void convertToBaseQuantity_withProductUomId_convertsCorrectly() {
        // Given
        BigDecimal saleQuantity = new BigDecimal("5");
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(productUom));

        // When
        BigDecimal result = productUomService.convertToBaseQuantity(1L, saleQuantity);

        // Then
        assertEquals(new BigDecimal("5000"), result);
    }

    @Test
    void convertToBaseQuantity_withNullId_returnsQuantityAsIs() {
        // Given
        BigDecimal saleQuantity = new BigDecimal("5");

        // When
        BigDecimal result = productUomService.convertToBaseQuantity(null, saleQuantity);

        // Then
        assertEquals(new BigDecimal("5"), result);
    }

    @Test
    void convertToBaseQuantity_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productUomRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () ->
                productUomService.convertToBaseQuantity(999L, BigDecimal.ONE));
    }
}
