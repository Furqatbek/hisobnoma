package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.inventory.entity.Product;
import com.hisobnoma.platform.inventory.entity.ProductVariant;
import com.hisobnoma.platform.pos.dto.CreatePriceListItemRequest;
import com.hisobnoma.platform.pos.dto.PriceListItemDto;
import com.hisobnoma.platform.pos.entity.PriceList;
import com.hisobnoma.platform.pos.entity.PriceListItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PriceListItemMapperTest {

    @Autowired
    private PriceListItemMapper priceListItemMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        PriceList priceList = PriceList.builder().id(1L).build();
        Product product = Product.builder().id(10L).sku("SKU-001").name("Test Product").build();
        ProductVariant variant = ProductVariant.builder().id(20L).name("Red / Large").build();

        PriceListItem item = PriceListItem.builder()
                .id(100L)
                .priceList(priceList)
                .product(product)
                .variant(variant)
                .price(new BigDecimal("50000.0000"))
                .minPrice(new BigDecimal("40000.0000"))
                .maxPrice(new BigDecimal("60000.0000"))
                .markupPercent(new BigDecimal("10.00"))
                .minQuantity(BigDecimal.ONE)
                .maxQuantity(new BigDecimal("100.0000"))
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .active(true)
                .notes("Item notes")
                .build();

        // When
        PriceListItemDto dto = priceListItemMapper.toDto(item);

        // Then
        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals(1L, dto.getPriceListId());
        assertEquals(10L, dto.getProductId());
        assertEquals("SKU-001", dto.getProductCode());
        assertEquals("Test Product", dto.getProductName());
        assertEquals(20L, dto.getVariantId());
        assertEquals("Red / Large", dto.getVariantName());
        assertEquals(new BigDecimal("50000.0000"), dto.getPrice());
        assertEquals(new BigDecimal("40000.0000"), dto.getMinPrice());
        assertEquals(new BigDecimal("60000.0000"), dto.getMaxPrice());
        assertEquals(new BigDecimal("10.00"), dto.getMarkupPercent());
        assertEquals(BigDecimal.ONE, dto.getMinQuantity());
        assertEquals(new BigDecimal("100.0000"), dto.getMaxQuantity());
        assertEquals(LocalDate.of(2024, 1, 1), dto.getStartDate());
        assertEquals(LocalDate.of(2024, 12, 31), dto.getEndDate());
        assertTrue(dto.isActive());
        assertEquals("Item notes", dto.getNotes());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreatePriceListItemRequest request = CreatePriceListItemRequest.builder()
                .productId(10L)
                .variantId(20L)
                .price(new BigDecimal("45000.0000"))
                .minPrice(new BigDecimal("35000.0000"))
                .maxPrice(new BigDecimal("55000.0000"))
                .markupPercent(new BigDecimal("5.00"))
                .minQuantity(new BigDecimal("5.0000"))
                .maxQuantity(new BigDecimal("50.0000"))
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .notes("New item notes")
                .build();

        // When
        PriceListItem entity = priceListItemMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(new BigDecimal("45000.0000"), entity.getPrice());
        assertEquals(new BigDecimal("35000.0000"), entity.getMinPrice());
        assertEquals(new BigDecimal("55000.0000"), entity.getMaxPrice());
        assertEquals(new BigDecimal("5.00"), entity.getMarkupPercent());
        assertEquals(new BigDecimal("5.0000"), entity.getMinQuantity());
        assertEquals(new BigDecimal("50.0000"), entity.getMaxQuantity());
        assertTrue(entity.isActive()); // constant = "true"
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getPriceList());
        assertNull(entity.getProduct());
        assertNull(entity.getVariant());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        PriceListItem existing = PriceListItem.builder()
                .id(100L)
                .price(new BigDecimal("50000.0000"))
                .active(true)
                .build();

        CreatePriceListItemRequest request = CreatePriceListItemRequest.builder()
                .productId(10L)
                .price(new BigDecimal("55000.0000"))
                .notes("Updated notes")
                .build();

        // When
        priceListItemMapper.updateEntity(request, existing);

        // Then
        assertEquals(new BigDecimal("55000.0000"), existing.getPrice());
        assertEquals("Updated notes", existing.getNotes());
        assertEquals(100L, existing.getId());
    }

    @Test
    void toDtoList_mapsAllItems() {
        // Given
        PriceList pl = PriceList.builder().id(1L).build();
        Product p = Product.builder().id(10L).sku("SKU").name("Product").build();

        PriceListItem i1 = PriceListItem.builder().id(1L).priceList(pl).product(p)
                .price(BigDecimal.TEN).minQuantity(BigDecimal.ONE).build();
        PriceListItem i2 = PriceListItem.builder().id(2L).priceList(pl).product(p)
                .price(BigDecimal.TEN).minQuantity(BigDecimal.ONE).build();

        // When
        List<PriceListItemDto> dtos = priceListItemMapper.toDtoList(List.of(i1, i2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
