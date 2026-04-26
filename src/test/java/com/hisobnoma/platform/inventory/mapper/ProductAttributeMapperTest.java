package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateProductRequest;
import com.hisobnoma.platform.inventory.dto.ProductAttributeDto;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductAttribute;
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
class ProductAttributeMapperTest {

    @Autowired
    private ProductAttributeMapper productAttributeMapper;

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
                .sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .attributes(new ArrayList<>())
                .build();

        ProductAttribute attribute = ProductAttribute.builder()
                .id(1L)
                .tenantId(1L)
                .product(product)
                .attributeName("Screen Size")
                .attributeValue("6.2 inches")
                .attributeType("TEXT")
                .attributeGroup("Display")
                .sortOrder(1)
                .visible(true)
                .searchable(true)
                .filterable(false)
                .build();

        // When
        ProductAttributeDto dto = productAttributeMapper.toDto(attribute);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getProductId());
        assertEquals("Screen Size", dto.getAttributeName());
        assertEquals("6.2 inches", dto.getAttributeValue());
        assertEquals("TEXT", dto.getAttributeType());
        assertEquals("Display", dto.getAttributeGroup());
        assertEquals(1, dto.getSortOrder());
        assertTrue(dto.isVisible());
        assertTrue(dto.isSearchable());
        assertFalse(dto.isFilterable());
    }

    @Test
    void toDtoList_mapsAllEntities() {
        // Given
        UnitOfMeasure uom = UnitOfMeasure.builder()
                .id(1L).code("PCS").name("Pieces").build();
        Product product = Product.builder()
                .id(10L).name("Product").sku("SKU-001").baseUom(uom)
                .sellingPrice(BigDecimal.TEN)
                .variants(new ArrayList<>()).images(new ArrayList<>()).attributes(new ArrayList<>())
                .build();

        ProductAttribute attr1 = ProductAttribute.builder()
                .id(1L).product(product)
                .attributeName("Color").attributeValue("Black")
                .build();
        ProductAttribute attr2 = ProductAttribute.builder()
                .id(2L).product(product)
                .attributeName("Storage").attributeValue("128GB")
                .build();

        // When
        List<ProductAttributeDto> dtos = productAttributeMapper.toDtoList(List.of(attr1, attr2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("Color", dtos.get(0).getAttributeName());
        assertEquals("Storage", dtos.get(1).getAttributeName());
    }

    @Test
    void toEntity_mapsFromCreateAttributeRequest() {
        // Given
        CreateProductRequest.CreateAttributeRequest request =
                CreateProductRequest.CreateAttributeRequest.builder()
                        .attributeName("Weight")
                        .attributeValue("168g")
                        .attributeType("TEXT")
                        .attributeGroup("Physical")
                        .sortOrder(5)
                        .visible(true)
                        .searchable(false)
                        .filterable(true)
                        .build();

        // When
        ProductAttribute entity = productAttributeMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("Weight", entity.getAttributeName());
        assertEquals("168g", entity.getAttributeValue());
        assertEquals("TEXT", entity.getAttributeType());
        assertEquals("Physical", entity.getAttributeGroup());
        assertEquals(5, entity.getSortOrder());
        assertTrue(entity.isVisible());
        assertFalse(entity.isSearchable());
        assertTrue(entity.isFilterable());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getProduct());
    }
}
