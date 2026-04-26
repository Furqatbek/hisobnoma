package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.ProductUomDto;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductUom;
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
class ProductUomMapperTest {

    @Autowired
    private ProductUomMapper productUomMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        UnitOfMeasure baseUom = UnitOfMeasure.builder()
                .id(1L).code("TON").name("Tonna").build();

        Product product = Product.builder()
                .id(10L)
                .name("Sand")
                .sku("SAND-001")
                .baseUom(baseUom)
                .sellingPrice(new BigDecimal("100.0000"))
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        UnitOfMeasure altUom = UnitOfMeasure.builder()
                .id(2L)
                .code("SAM")
                .name("Samosvol")
                .symbol("sam")
                .build();

        ProductUom productUom = ProductUom.builder()
                .id(1L)
                .tenantId(1L)
                .product(product)
                .uom(altUom)
                .conversionFactor(new BigDecimal("12.000000"))
                .sellingPrice(new BigDecimal("1100.0000"))
                .defaultSale(true)
                .active(true)
                .sortOrder(1)
                .build();

        // When
        ProductUomDto dto = productUomMapper.toDto(productUom);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getProductId());
        assertEquals("Sand", dto.getProductName());
        assertEquals(2L, dto.getUomId());
        assertEquals("SAM", dto.getUomCode());
        assertEquals("Samosvol", dto.getUomName());
        assertEquals("sam", dto.getUomSymbol());
        assertEquals(new BigDecimal("12.000000"), dto.getConversionFactor());
        assertEquals(new BigDecimal("1100.0000"), dto.getSellingPrice());
        // effectiveSellingPrice should return the override price
        assertEquals(new BigDecimal("1100.0000"), dto.getEffectiveSellingPrice());
        assertTrue(dto.isDefaultSale());
        assertTrue(dto.isActive());
        assertEquals(1, dto.getSortOrder());
    }

    @Test
    void toDto_effectiveSellingPrice_fallsBackToProductPrice() {
        // Given
        UnitOfMeasure baseUom = UnitOfMeasure.builder()
                .id(1L).code("TON").name("Tonna").build();

        Product product = Product.builder()
                .id(10L)
                .name("Sand")
                .sku("SAND-001")
                .baseUom(baseUom)
                .sellingPrice(new BigDecimal("100.0000"))
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        UnitOfMeasure altUom = UnitOfMeasure.builder()
                .id(2L).code("SAM").name("Samosvol").build();

        ProductUom productUom = ProductUom.builder()
                .id(2L)
                .product(product)
                .uom(altUom)
                .conversionFactor(new BigDecimal("12.000000"))
                .sellingPrice(null) // no override
                .defaultSale(false)
                .active(true)
                .build();

        // When
        ProductUomDto dto = productUomMapper.toDto(productUom);

        // Then
        assertNotNull(dto);
        assertNull(dto.getSellingPrice());
        // effectiveSellingPrice = product.sellingPrice * conversionFactor = 100 * 12 = 1200.0000
        assertEquals(new BigDecimal("1200.0000"), dto.getEffectiveSellingPrice());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        UnitOfMeasure baseUom = UnitOfMeasure.builder()
                .id(1L).code("TON").name("Tonna").build();
        Product product = Product.builder()
                .id(10L).name("Sand").sku("SAND-001").baseUom(baseUom)
                .sellingPrice(new BigDecimal("100.0000"))
                .variants(new ArrayList<>()).images(new ArrayList<>()).attributes(new ArrayList<>())
                .build();
        UnitOfMeasure altUom = UnitOfMeasure.builder()
                .id(2L).code("SAM").name("Samosvol").build();

        ProductUom pu1 = ProductUom.builder()
                .id(1L).product(product).uom(altUom)
                .conversionFactor(new BigDecimal("12.000000"))
                .active(true).build();
        ProductUom pu2 = ProductUom.builder()
                .id(2L).product(product).uom(altUom)
                .conversionFactor(new BigDecimal("6.000000"))
                .active(true).build();

        // When
        List<ProductUomDto> dtos = productUomMapper.toDtoList(List.of(pu1, pu2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
