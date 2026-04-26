package com.hisobnoma.platform.inventory.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.dto.PageResponse;
import com.hisobnoma.platform.common.exception.DuplicateResourceException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.inventory.dto.*;
import com.hisobnoma.platform.inventory.entity.*;
import com.hisobnoma.platform.inventory.mapper.ProductAttributeMapper;
import com.hisobnoma.platform.inventory.mapper.ProductMapper;
import com.hisobnoma.platform.inventory.mapper.ProductVariantMapper;
import com.hisobnoma.platform.inventory.repository.*;
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
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductVariantRepository variantRepository;
    @Mock
    private ProductImageRepository imageRepository;
    @Mock
    private ProductAttributeRepository attributeRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private BrandRepository brandRepository;
    @Mock
    private UnitOfMeasureRepository uomRepository;
    @Mock
    private StockRepository stockRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductVariantMapper variantMapper;
    @Mock
    private ProductAttributeMapper attributeMapper;
    @Mock
    private SkuGeneratorService skuGeneratorService;
    @Mock
    private BarcodeService barcodeService;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private ProductService productService;

    private static final Long TENANT_ID = 1L;
    private Product product;
    private ProductDto productDto;
    private Category category;
    private Brand brand;
    private UnitOfMeasure uom;

    @BeforeEach
    void setUp() {
        uom = UnitOfMeasure.builder()
                .id(1L)
                .code("PC")
                .name("Piece")
                .tenantId(TENANT_ID)
                .build();

        category = Category.builder()
                .id(1L)
                .code("CAT-001")
                .name("Electronics")
                .tenantId(TENANT_ID)
                .build();

        brand = Brand.builder()
                .id(1L)
                .code("BRD-001")
                .name("Samsung")
                .tenantId(TENANT_ID)
                .build();

        product = Product.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Samsung Galaxy S24")
                .description("Flagship phone")
                .category(category)
                .brand(brand)
                .baseUom(uom)
                .costPrice(new BigDecimal("500.00"))
                .sellingPrice(new BigDecimal("999.00"))
                .active(true)
                .tenantId(TENANT_ID)
                .build();

        productDto = ProductDto.builder()
                .id(1L)
                .sku("PROD-001")
                .name("Samsung Galaxy S24")
                .categoryId(1L)
                .categoryName("Electronics")
                .brandId(1L)
                .brandName("Samsung")
                .baseUomId(1L)
                .baseUomCode("PC")
                .costPrice(new BigDecimal("500.00"))
                .sellingPrice(new BigDecimal("999.00"))
                .active(true)
                .build();
    }

    // ==================== getProducts ====================

    @Test
    void getProducts_returnsPaginatedResults() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(productMapper.toDtoSimple(any(Product.class))).thenReturn(productDto);
        when(stockRepository.getTotalQuantitiesByTenant(TENANT_ID)).thenReturn(Collections.emptyList());
        when(imageRepository.findPrimaryImageUrlsByProductIds(anyList())).thenReturn(Collections.emptyList());

        // When
        PageResponse<ProductDto> result = productService.getProducts(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(productRepository).findAllByTenantId(TENANT_ID, pageable);
    }

    @Test
    void getProducts_returnsEmpty_whenNone() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllByTenantId(TENANT_ID, pageable)).thenReturn(emptyPage);

        // When
        PageResponse<ProductDto> result = productService.getProducts(pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== getProduct ====================

    @Test
    void getProduct_found_returnsDto() {
        // Given
        when(productRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDto(product)).thenReturn(productDto);
        when(variantRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(variantMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());
        when(imageRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(attributeRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(attributeMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(stockRepository.getTotalQuantityByProduct(1L, TENANT_ID)).thenReturn(new BigDecimal("50"));

        // When
        ProductDto result = productService.getProduct(1L);

        // Then
        assertNotNull(result);
        assertEquals("PROD-001", result.getSku());
        assertEquals("Samsung Galaxy S24", result.getName());
    }

    @Test
    void getProduct_notFound_throwsNotFoundException() {
        // Given
        when(productRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productService.getProduct(999L));
    }

    // ==================== createProduct ====================

    @Test
    void createProduct_success_returnsDtoWithGeneratedSku() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUomId(1L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(skuGeneratorService.generateSkuFromName("New Product")).thenReturn("NEW-PROD-001");
        when(productMapper.toEntity(request)).thenReturn(product);
        when(uomRepository.findById(1L)).thenReturn(Optional.of(uom));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDto(product)).thenReturn(productDto);
        when(variantRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(variantMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());
        when(imageRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(attributeRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(attributeMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());
        when(stockRepository.getTotalQuantityByProduct(1L, TENANT_ID)).thenReturn(BigDecimal.ZERO);

        // When
        ProductDto result = productService.createProduct(request);

        // Then
        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_duplicateSku_throwsDuplicateResourceException() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .sku("EXISTING-SKU")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUomId(1L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.existsBySkuAndTenantId("EXISTING-SKU", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_duplicateBarcode_throwsDuplicateResourceException() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .barcode("DUP-BARCODE")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUomId(1L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(skuGeneratorService.generateSkuFromName("New Product")).thenReturn("NEW-SKU");
        when(productRepository.existsByBarcodeAndTenantId("DUP-BARCODE", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> productService.createProduct(request));
        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_categoryNotFound_throwsNotFoundException() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .categoryId(999L)
                .sellingPrice(new BigDecimal("100.00"))
                .baseUomId(1L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(skuGeneratorService.generateSkuFromName("New Product")).thenReturn("NEW-SKU");
        when(productMapper.toEntity(request)).thenReturn(Product.builder().id(1L).build());
        when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productService.createProduct(request));
    }

    @Test
    void createProduct_brandNotFound_throwsNotFoundException() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .brandId(999L)
                .sellingPrice(new BigDecimal("100.00"))
                .baseUomId(1L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(skuGeneratorService.generateSkuFromName("New Product")).thenReturn("NEW-SKU");
        when(productMapper.toEntity(request)).thenReturn(Product.builder().id(1L).build());
        when(brandRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productService.createProduct(request));
    }

    @Test
    void createProduct_uomNotFound_throwsNotFoundException() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .name("New Product")
                .sellingPrice(new BigDecimal("100.00"))
                .baseUomId(999L)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(skuGeneratorService.generateSkuFromName("New Product")).thenReturn("NEW-SKU");
        when(productMapper.toEntity(request)).thenReturn(Product.builder().id(1L).build());
        when(uomRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productService.createProduct(request));
    }

    // ==================== updateProduct ====================

    @Test
    void updateProduct_success_updatesFields() {
        // Given
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Updated Product")
                .sellingPrice(new BigDecimal("1099.00"))
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDto(product)).thenReturn(productDto);
        when(variantRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(variantMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());
        when(imageRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(attributeRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(attributeMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());
        when(stockRepository.getTotalQuantityByProduct(1L, TENANT_ID)).thenReturn(BigDecimal.ZERO);

        // When
        ProductDto result = productService.updateProduct(1L, request);

        // Then
        assertNotNull(result);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void updateProduct_notFound_throwsNotFoundException() {
        // Given
        UpdateProductRequest request = UpdateProductRequest.builder()
                .name("Updated Product")
                .build();

        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productService.updateProduct(999L, request));
    }

    @Test
    void updateProduct_duplicateBarcode_throwsDuplicateResourceException() {
        // Given
        UpdateProductRequest request = UpdateProductRequest.builder()
                .barcode("EXISTING-BARCODE")
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.existsByBarcodeAndTenantId("EXISTING-BARCODE", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> productService.updateProduct(1L, request));
    }

    // ==================== deleteProduct ====================

    @Test
    void deleteProduct_success_softDeletes() {
        // Given
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        productService.deleteProduct(1L);

        // Then
        assertFalse(product.isActive());
        verify(productRepository).save(product);
    }

    @Test
    void deleteProduct_notFound_throwsNotFoundException() {
        // Given
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productService.deleteProduct(999L));
    }

    // ==================== searchProducts ====================

    @Test
    void searchProducts_byName_returnsMatches() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.searchByTenantId(TENANT_ID, "Galaxy", pageable)).thenReturn(page);
        when(productMapper.toDtoSimple(any(Product.class))).thenReturn(productDto);
        when(stockRepository.getTotalQuantitiesByTenant(TENANT_ID)).thenReturn(Collections.emptyList());
        when(imageRepository.findPrimaryImageUrlsByProductIds(anyList())).thenReturn(Collections.emptyList());

        // When
        PageResponse<ProductDto> result = productService.searchProducts("Galaxy", pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    @Test
    void searchProducts_noMatch_returnsEmpty() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.searchByTenantId(TENANT_ID, "xyz123", pageable)).thenReturn(emptyPage);

        // When
        PageResponse<ProductDto> result = productService.searchProducts("xyz123", pageable);

        // Then
        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
    }

    // ==================== getProductsByCategory ====================

    @Test
    void getProductsByCategory_returnsProductsInCategory() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByCategoryIdAndTenantId(1L, TENANT_ID, pageable)).thenReturn(page);
        when(productMapper.toDtoSimple(any(Product.class))).thenReturn(productDto);
        when(stockRepository.getTotalQuantitiesByTenant(TENANT_ID)).thenReturn(Collections.emptyList());
        when(imageRepository.findPrimaryImageUrlsByProductIds(anyList())).thenReturn(Collections.emptyList());

        // When
        PageResponse<ProductDto> result = productService.getProductsByCategory(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ==================== getProductsByBrand ====================

    @Test
    void getProductsByBrand_returnsProductsForBrand() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByBrandIdAndTenantId(1L, TENANT_ID, pageable)).thenReturn(page);
        when(productMapper.toDtoSimple(any(Product.class))).thenReturn(productDto);
        when(stockRepository.getTotalQuantitiesByTenant(TENANT_ID)).thenReturn(Collections.emptyList());
        when(imageRepository.findPrimaryImageUrlsByProductIds(anyList())).thenReturn(Collections.emptyList());

        // When
        PageResponse<ProductDto> result = productService.getProductsByBrand(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
    }

    // ==================== getActiveProducts ====================

    @Test
    void getActiveProducts_returnsOnlyActiveProducts() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(product), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findAllActiveByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(productMapper.toDtoSimple(any(Product.class))).thenReturn(productDto);
        when(stockRepository.getTotalQuantitiesByTenant(TENANT_ID)).thenReturn(Collections.emptyList());
        when(imageRepository.findPrimaryImageUrlsByProductIds(anyList())).thenReturn(Collections.emptyList());

        // When
        PageResponse<ProductDto> result = productService.getActiveProducts(pageable);

        // Then
        assertNotNull(result);
        assertFalse(result.getContent().isEmpty());
    }

    // ==================== Variant Management ====================

    @Test
    void getVariants_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(variantRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(variantMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());

        // When
        List<ProductVariantDto> result = productService.getVariants(1L);

        // Then
        assertNotNull(result);
    }

    @Test
    void addVariant_success() {
        // Given
        CreateVariantRequest request = CreateVariantRequest.builder()
                .option1Name("Color")
                .option1Value("Red")
                .active(true)
                .build();

        ProductVariant variant = ProductVariant.builder()
                .id(1L)
                .product(product)
                .sku("PROD-001-RED")
                .name("Red")
                .tenantId(TENANT_ID)
                .build();

        ProductVariantDto variantDto = ProductVariantDto.builder()
                .id(1L)
                .sku("PROD-001-RED")
                .name("Red")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(skuGeneratorService.generateVariantSku(anyString(), anyString(), any(), any()))
                .thenReturn("PROD-001-RED");
        when(variantRepository.countByProductId(1L)).thenReturn(0L);
        when(variantRepository.save(any(ProductVariant.class))).thenReturn(variant);
        when(variantMapper.toDto(variant)).thenReturn(variantDto);

        // When
        ProductVariantDto result = productService.addVariant(1L, request);

        // Then
        assertNotNull(result);
        assertEquals("PROD-001-RED", result.getSku());
        verify(variantRepository).save(any(ProductVariant.class));
    }

    @Test
    void addVariant_productNotFound_throwsNotFoundException() {
        // Given
        CreateVariantRequest request = CreateVariantRequest.builder()
                .option1Name("Color")
                .option1Value("Red")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productService.addVariant(999L, request));
    }

    @Test
    void addVariant_duplicateSku_throwsDuplicateResourceException() {
        // Given
        CreateVariantRequest request = CreateVariantRequest.builder()
                .sku("EXISTING-SKU")
                .option1Name("Color")
                .option1Value("Red")
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(variantRepository.existsBySkuAndTenantId("EXISTING-SKU", TENANT_ID)).thenReturn(true);

        // When/Then
        assertThrows(DuplicateResourceException.class, () -> productService.addVariant(1L, request));
    }

    @Test
    void deleteVariant_success() {
        // Given
        ProductVariant variant = ProductVariant.builder()
                .id(1L)
                .product(product)
                .sku("PROD-001-RED")
                .tenantId(TENANT_ID)
                .build();

        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(product));
        when(variantRepository.findById(1L)).thenReturn(Optional.of(variant));
        when(variantRepository.countByProductId(1L)).thenReturn(0L);

        // When
        productService.deleteVariant(1L, 1L);

        // Then
        verify(variantRepository).delete(variant);
    }

    // ==================== getProductBySku / getProductByBarcode ====================

    @Test
    void getProductBySku_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findBySkuAndTenantId("PROD-001", TENANT_ID)).thenReturn(Optional.of(product));
        when(productRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(product));
        when(productMapper.toDto(product)).thenReturn(productDto);
        when(variantRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(variantMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());
        when(imageRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(attributeRepository.findByProductIdOrderBySortOrder(1L)).thenReturn(Collections.emptyList());
        when(attributeMapper.toDtoList(anyList())).thenReturn(Collections.emptyList());
        when(stockRepository.getTotalQuantityByProduct(1L, TENANT_ID)).thenReturn(BigDecimal.ZERO);

        // When
        ProductDto result = productService.getProductBySku("PROD-001");

        // Then
        assertNotNull(result);
        assertEquals("PROD-001", result.getSku());
    }

    @Test
    void getProductBySku_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findBySkuAndTenantId("MISSING", TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productService.getProductBySku("MISSING"));
    }

    @Test
    void getProductByBarcode_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(productRepository.findByBarcodeAndTenantId("MISSING-BC", TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> productService.getProductByBarcode("MISSING-BC"));
    }
}
