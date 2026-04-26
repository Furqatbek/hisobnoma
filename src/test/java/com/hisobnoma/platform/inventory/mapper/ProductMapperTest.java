package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateProductRequest;
import com.hisobnoma.platform.inventory.dto.ProductDto;
import com.hisobnoma.platform.inventory.entity.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ProductMapperTest {

    @Autowired
    private ProductMapper productMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Category category = Category.builder()
                .id(5L)
                .name("Smartphones")
                .children(new ArrayList<>())
                .build();

        Brand brand = Brand.builder()
                .id(3L)
                .name("Samsung")
                .build();

        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .build();

        Product product = Product.builder()
                .id(1L)
                .tenantId(1L)
                .sku("SKU-001")
                .barcode("1234567890123")
                .name("Galaxy S24")
                .description("Samsung Galaxy S24 Smartphone")
                .shortDescription("Galaxy S24")
                .category(category)
                .brand(brand)
                .baseUom(uom)
                .costPrice(new BigDecimal("500.0000"))
                .sellingPrice(new BigDecimal("800.0000"))
                .minSellingPrice(new BigDecimal("750.0000"))
                .wholesalePrice(new BigDecimal("700.0000"))
                .trackInventory(true)
                .allowNegativeStock(false)
                .minStockLevel(new BigDecimal("10.0000"))
                .reorderPoint(new BigDecimal("20.0000"))
                .reorderQuantity(new BigDecimal("50.0000"))
                .maxStockLevel(new BigDecimal("200.0000"))
                .active(true)
                .service(false)
                .sellable(true)
                .purchasable(true)
                .hasVariants(false)
                .trackBatch(false)
                .trackSerial(true)
                .weight(new BigDecimal("0.168"))
                .weightUnit("kg")
                .length(new BigDecimal("14.70"))
                .width(new BigDecimal("7.06"))
                .height(new BigDecimal("0.76"))
                .dimensionUnit("cm")
                .primaryImageUrl("https://example.com/s24.png")
                .manufacturer("Samsung Electronics")
                .manufacturerPartNumber("SM-S921B")
                .taxCode("VAT-12")
                .notes("Flagship phone")
                .tags("phone,samsung,flagship")
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        // When
        ProductDto dto = productMapper.toDto(product);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("SKU-001", dto.getSku());
        assertEquals("1234567890123", dto.getBarcode());
        assertEquals("Galaxy S24", dto.getName());
        assertEquals("Samsung Galaxy S24 Smartphone", dto.getDescription());
        assertEquals("Galaxy S24", dto.getShortDescription());
        assertEquals(5L, dto.getCategoryId());
        assertEquals("Smartphones", dto.getCategoryName());
        assertEquals(3L, dto.getBrandId());
        assertEquals("Samsung", dto.getBrandName());
        assertEquals(1L, dto.getBaseUomId());
        assertEquals("PCS", dto.getBaseUomCode());
        assertEquals("Pieces", dto.getBaseUomName());
        assertEquals(new BigDecimal("500.0000"), dto.getCostPrice());
        assertEquals(new BigDecimal("800.0000"), dto.getSellingPrice());
        assertEquals(new BigDecimal("750.0000"), dto.getMinSellingPrice());
        assertEquals(new BigDecimal("700.0000"), dto.getWholesalePrice());
        assertTrue(dto.isTrackInventory());
        assertFalse(dto.isAllowNegativeStock());
        assertEquals(new BigDecimal("10.0000"), dto.getMinStockLevel());
        assertEquals(new BigDecimal("20.0000"), dto.getReorderPoint());
        assertEquals(new BigDecimal("50.0000"), dto.getReorderQuantity());
        assertEquals(new BigDecimal("200.0000"), dto.getMaxStockLevel());
        assertTrue(dto.isActive());
        assertFalse(dto.isService());
        assertTrue(dto.isSellable());
        assertTrue(dto.isPurchasable());
        assertFalse(dto.isHasVariants());
        assertFalse(dto.isTrackBatch());
        assertTrue(dto.isTrackSerial());
        assertEquals("https://example.com/s24.png", dto.getPrimaryImageUrl());
        assertEquals("Samsung Electronics", dto.getManufacturer());
        assertEquals("SM-S921B", dto.getManufacturerPartNumber());
        assertEquals("VAT-12", dto.getTaxCode());
        assertEquals("Flagship phone", dto.getNotes());
        assertEquals("phone,samsung,flagship", dto.getTags());
        // Expression-based computed fields
        assertNotNull(dto.getMargin());
        assertNotNull(dto.getMarkup());
    }

    @Test
    void toDto_withNullCategoryAndBrand() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .build();

        Product product = Product.builder()
                .id(1L)
                .sku("SKU-002")
                .name("Generic Product")
                .category(null)
                .brand(null)
                .baseUom(uom)
                .sellingPrice(new BigDecimal("100.0000"))
                .costPrice(BigDecimal.ZERO)
                .active(true)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        // When
        ProductDto dto = productMapper.toDto(product);

        // Then
        assertNotNull(dto);
        assertNull(dto.getCategoryId());
        assertNull(dto.getCategoryName());
        assertNull(dto.getBrandId());
        assertNull(dto.getBrandName());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L)
                .code("PCS")
                .name("Pieces")
                .build();

        Product product1 = Product.builder()
                .id(1L)
                .sku("SKU-001")
                .name("Product A")
                .baseUom(uom)
                .sellingPrice(new BigDecimal("100.0000"))
                .costPrice(BigDecimal.ZERO)
                .active(true)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        Product product2 = Product.builder()
                .id(2L)
                .sku("SKU-002")
                .name("Product B")
                .baseUom(uom)
                .sellingPrice(new BigDecimal("200.0000"))
                .costPrice(BigDecimal.ZERO)
                .active(true)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        // When
        List<ProductDto> dtos = productMapper.toDtoList(List.of(product1, product2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Product A", dtos.get(0).getName());
        assertEquals("Product B", dtos.get(1).getName());
        // toDtoSimple ignores variants, images, attributes
        assertNull(dtos.get(0).getVariants());
        assertNull(dtos.get(0).getImages());
        assertNull(dtos.get(0).getAttributes());
    }

    @Test
    void toEntity_mapsFromCreateRequest() {
        // Given
        CreateProductRequest request = CreateProductRequest.builder()
                .sku("SKU-003")
                .barcode("9999999999999")
                .name("New Product")
                .description("A new product")
                .shortDescription("New")
                .sellingPrice(new BigDecimal("150.0000"))
                .costPrice(new BigDecimal("100.0000"))
                .trackInventory(true)
                .active(true)
                .sellable(true)
                .purchasable(true)
                .taxCode("VAT-12")
                .notes("Test notes")
                .build();

        // When
        Product entity = productMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("SKU-003", entity.getSku());
        assertEquals("9999999999999", entity.getBarcode());
        assertEquals("New Product", entity.getName());
        assertEquals("A new product", entity.getDescription());
        assertEquals("New", entity.getShortDescription());
        assertEquals(new BigDecimal("150.0000"), entity.getSellingPrice());
        assertEquals(new BigDecimal("100.0000"), entity.getCostPrice());
        assertTrue(entity.isTrackInventory());
        assertTrue(entity.isActive());
        assertTrue(entity.isSellable());
        assertTrue(entity.isPurchasable());
        assertEquals("VAT-12", entity.getTaxCode());
        assertEquals("Test notes", entity.getNotes());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getCategory());
        assertNull(entity.getBrand());
        assertNull(entity.getBaseUom());
    }
}
