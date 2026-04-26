package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateProductRequest;
import com.hisobnoma.platform.inventory.dto.ProductVariantDto;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import com.hisobnoma.platform.inventory.entity.UnitOfMeasure;
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
class ProductVariantMapperTest {

    @Autowired
    private ProductVariantMapper productVariantMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(10L)
                .name("Galaxy S24")
                .sku("SKU-001")
                .baseUom(uom)
                .sellingPrice(new BigDecimal("800.0000"))
                .costPrice(new BigDecimal("500.0000"))
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        ProductVariant variant = ProductVariant.builder()
                .id(1L)
                .tenantId(1L)
                .product(product)
                .sku("SKU-001-BLK-128")
                .barcode("1234567890124")
                .name("128GB Black")
                .option1Name("Color")
                .option1Value("Black")
                .option2Name("Storage")
                .option2Value("128GB")
                .option3Name(null)
                .option3Value(null)
                .costPrice(new BigDecimal("520.0000"))
                .sellingPrice(new BigDecimal("850.0000"))
                .priceDifference(new BigDecimal("50.0000"))
                .weight(new BigDecimal("0.170"))
                .imageUrl("https://example.com/s24-black.png")
                .sortOrder(1)
                .active(true)
                .build();

        // When
        ProductVariantDto dto = productVariantMapper.toDto(variant);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getProductId());
        assertEquals("SKU-001-BLK-128", dto.getSku());
        assertEquals("1234567890124", dto.getBarcode());
        assertEquals("128GB Black", dto.getName());
        assertEquals("Color", dto.getOption1Name());
        assertEquals("Black", dto.getOption1Value());
        assertEquals("Storage", dto.getOption2Name());
        assertEquals("128GB", dto.getOption2Value());
        assertNull(dto.getOption3Name());
        assertNull(dto.getOption3Value());
        assertEquals(new BigDecimal("520.0000"), dto.getCostPrice());
        assertEquals(new BigDecimal("850.0000"), dto.getSellingPrice());
        assertEquals(new BigDecimal("50.0000"), dto.getPriceDifference());
        assertEquals(new BigDecimal("0.170"), dto.getWeight());
        assertEquals("https://example.com/s24-black.png", dto.getImageUrl());
        assertEquals(1, dto.getSortOrder());
        assertTrue(dto.isActive());
        // Expression-based
        assertEquals("Galaxy S24 - Black / 128GB", dto.getFullName());
        assertEquals(new BigDecimal("850.0000"), dto.getEffectiveSellingPrice());
        assertEquals(new BigDecimal("520.0000"), dto.getEffectiveCostPrice());
    }

    @Test
    void toDto_effectivePrices_fallBackToProductPrices() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();

        Product product = Product.builder()
                .id(10L)
                .name("Galaxy S24")
                .sku("SKU-001")
                .baseUom(uom)
                .sellingPrice(new BigDecimal("800.0000"))
                .costPrice(new BigDecimal("500.0000"))
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        ProductVariant variant = ProductVariant.builder()
                .id(2L)
                .product(product)
                .sku("SKU-001-WHT")
                .name("White")
                .option1Name("Color")
                .option1Value("White")
                .costPrice(null) // no override
                .sellingPrice(null) // no override
                .priceDifference(new BigDecimal("25.0000"))
                .sortOrder(2)
                .active(true)
                .build();

        // When
        ProductVariantDto dto = productVariantMapper.toDto(variant);

        // Then
        // effectiveSellingPrice = product.sellingPrice + priceDifference = 800 + 25 = 825
        assertEquals(new BigDecimal("825.0000"), dto.getEffectiveSellingPrice());
        // effectiveCostPrice = product.costPrice = 500
        assertEquals(new BigDecimal("500.0000"), dto.getEffectiveCostPrice());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();
        Product product = Product.builder()
                .id(10L).name("Product").sku("SKU-001").baseUom(uom)
                .sellingPrice(new BigDecimal("100.0000")).costPrice(BigDecimal.ZERO)
                .variants(new ArrayList<>()).images(new ArrayList<>()).attributes(new ArrayList<>())
                .build();

        ProductVariant v1 = ProductVariant.builder()
                .id(1L).product(product).sku("V1").name("Variant A").active(true).build();
        ProductVariant v2 = ProductVariant.builder()
                .id(2L).product(product).sku("V2").name("Variant B").active(true).build();

        // When
        List<ProductVariantDto> dtos = productVariantMapper.toDtoList(List.of(v1, v2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Variant A", dtos.get(0).getName());
        assertEquals("Variant B", dtos.get(1).getName());
    }

    @Test
    void toEntity_mapsFromCreateVariantRequest() {
        // Given
        CreateProductRequest.CreateVariantRequest request =
                CreateProductRequest.CreateVariantRequest.builder()
                        .sku("SKU-VAR-001")
                        .barcode("9999999999001")
                        .name("Small Red")
                        .option1Name("Size")
                        .option1Value("Small")
                        .option2Name("Color")
                        .option2Value("Red")
                        .costPrice(new BigDecimal("40.0000"))
                        .sellingPrice(new BigDecimal("75.0000"))
                        .priceDifference(new BigDecimal("5.0000"))
                        .weight(new BigDecimal("0.250"))
                        .imageUrl("https://example.com/var-001.png")
                        .sortOrder(1)
                        .active(true)
                        .build();

        // When
        ProductVariant entity = productVariantMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("SKU-VAR-001", entity.getSku());
        assertEquals("9999999999001", entity.getBarcode());
        assertEquals("Small Red", entity.getName());
        assertEquals("Size", entity.getOption1Name());
        assertEquals("Small", entity.getOption1Value());
        assertEquals("Color", entity.getOption2Name());
        assertEquals("Red", entity.getOption2Value());
        assertEquals(new BigDecimal("40.0000"), entity.getCostPrice());
        assertEquals(new BigDecimal("75.0000"), entity.getSellingPrice());
        assertEquals(new BigDecimal("5.0000"), entity.getPriceDifference());
        assertEquals(new BigDecimal("0.250"), entity.getWeight());
        assertEquals("https://example.com/var-001.png", entity.getImageUrl());
        assertEquals(1, entity.getSortOrder());
        assertTrue(entity.isActive());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getProduct());
    }
}
