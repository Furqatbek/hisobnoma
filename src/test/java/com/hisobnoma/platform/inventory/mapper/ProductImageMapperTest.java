package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.ProductImageDto;
import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductImage;
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
class ProductImageMapperTest {

    @Autowired
    private ProductImageMapper productImageMapper;

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

        ProductImage image = ProductImage.builder()
                .id(1L)
                .tenantId(1L)
                .product(product)
                .imageUrl("https://example.com/images/galaxy-s24.png")
                .thumbnailUrl("https://example.com/images/galaxy-s24-thumb.png")
                .altText("Galaxy S24 front view")
                .title("Galaxy S24 Product Image")
                .sortOrder(0)
                .primary(true)
                .active(true)
                .build();

        // When
        ProductImageDto dto = productImageMapper.toDto(image);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getProductId());
        assertEquals("https://example.com/images/galaxy-s24.png", dto.getImageUrl());
        assertEquals("https://example.com/images/galaxy-s24-thumb.png", dto.getThumbnailUrl());
        assertEquals("Galaxy S24 front view", dto.getAltText());
        assertEquals("Galaxy S24 Product Image", dto.getTitle());
        assertEquals(0, dto.getSortOrder());
        assertTrue(dto.isPrimary());
        assertTrue(dto.isActive());
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

        ProductImage img1 = ProductImage.builder()
                .id(1L).product(product)
                .imageUrl("https://example.com/img1.png")
                .sortOrder(0).primary(true).active(true)
                .build();

        ProductImage img2 = ProductImage.builder()
                .id(2L).product(product)
                .imageUrl("https://example.com/img2.png")
                .sortOrder(1).primary(false).active(true)
                .build();

        // When
        List<ProductImageDto> dtos = productImageMapper.toDtoList(List.of(img1, img2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("https://example.com/img1.png", dtos.get(0).getImageUrl());
        assertEquals("https://example.com/img2.png", dtos.get(1).getImageUrl());
    }
}
