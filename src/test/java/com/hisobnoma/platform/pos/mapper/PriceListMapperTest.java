package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CreatePriceListRequest;
import com.hisobnoma.platform.pos.dto.PriceListDto;
import com.hisobnoma.platform.pos.entity.PriceList;
import com.hisobnoma.platform.pos.enums.PriceListType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PriceListMapperTest {

    @Autowired
    private PriceListMapper priceListMapper;

    @Test
    void toDto_mapsAllFieldsExceptItems() {
        // Given
        PriceList priceList = PriceList.builder()
                .id(1L)
                .tenantId(1L)
                .code("PL-001")
                .name("Standard Price List")
                .description("Default prices")
                .type(PriceListType.STANDARD)
                .currency("UZS")
                .priority(10)
                .defaultMarkupPercent(new BigDecimal("5.00"))
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .active(true)
                .defaultPriceList(true)
                .locationId(100L)
                .notes("Standard list notes")
                .items(new ArrayList<>())
                .customerAssignments(new ArrayList<>())
                .build();

        // When
        PriceListDto dto = priceListMapper.toDto(priceList);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("PL-001", dto.getCode());
        assertEquals("Standard Price List", dto.getName());
        assertEquals("Default prices", dto.getDescription());
        assertEquals(PriceListType.STANDARD, dto.getType());
        assertEquals("UZS", dto.getCurrency());
        assertEquals(10, dto.getPriority());
        assertEquals(new BigDecimal("5.00"), dto.getDefaultMarkupPercent());
        assertEquals(LocalDate.of(2024, 1, 1), dto.getStartDate());
        assertEquals(LocalDate.of(2024, 12, 31), dto.getEndDate());
        assertTrue(dto.isActive());
        assertTrue(dto.isDefaultPriceList());
        assertEquals(100L, dto.getLocationId());
        assertEquals("Standard list notes", dto.getNotes());
        assertEquals(0, dto.getItemCount());
        assertEquals(0, dto.getCustomerCount());
        // toDto ignores items
        assertNull(dto.getItems());
    }

    @Test
    void toDtoWithItems_includesItems() {
        // Given
        PriceList priceList = PriceList.builder()
                .id(1L)
                .code("PL-001")
                .name("Standard")
                .type(PriceListType.STANDARD)
                .items(new ArrayList<>())
                .customerAssignments(new ArrayList<>())
                .build();

        // When
        PriceListDto dto = priceListMapper.toDtoWithItems(priceList);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.getItems());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreatePriceListRequest request = CreatePriceListRequest.builder()
                .code("PL-002")
                .name("Wholesale")
                .description("Wholesale prices")
                .type(PriceListType.STANDARD)
                .currency("USD")
                .priority(5)
                .defaultMarkupPercent(new BigDecimal("-10.00"))
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .defaultPriceList(false)
                .locationId(200L)
                .notes("Wholesale notes")
                .build();

        // When
        PriceList entity = priceListMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("PL-002", entity.getCode());
        assertEquals("Wholesale", entity.getName());
        assertEquals("Wholesale prices", entity.getDescription());
        assertEquals(PriceListType.STANDARD, entity.getType());
        assertEquals("USD", entity.getCurrency());
        assertEquals(5, entity.getPriority());
        assertEquals(new BigDecimal("-10.00"), entity.getDefaultMarkupPercent());
        assertTrue(entity.isActive()); // constant = "true"
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        PriceList existing = PriceList.builder()
                .id(1L)
                .tenantId(1L)
                .code("PL-001")
                .name("Old Name")
                .type(PriceListType.STANDARD)
                .active(true)
                .items(new ArrayList<>())
                .customerAssignments(new ArrayList<>())
                .build();

        CreatePriceListRequest request = CreatePriceListRequest.builder()
                .code("PL-001")
                .name("Updated Name")
                .description("Updated description")
                .type(PriceListType.STANDARD)
                .build();

        // When
        priceListMapper.updateEntity(request, existing);

        // Then
        assertEquals("Updated Name", existing.getName());
        assertEquals("Updated description", existing.getDescription());
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
    }

    @Test
    void toDtoList_mapsAllPriceLists() {
        // Given
        PriceList pl1 = PriceList.builder()
                .id(1L).code("PL-001").name("Standard")
                .type(PriceListType.STANDARD).items(new ArrayList<>())
                .customerAssignments(new ArrayList<>()).build();
        PriceList pl2 = PriceList.builder()
                .id(2L).code("PL-002").name("Wholesale")
                .type(PriceListType.STANDARD).items(new ArrayList<>())
                .customerAssignments(new ArrayList<>()).build();

        // When
        List<PriceListDto> dtos = priceListMapper.toDtoList(List.of(pl1, pl2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
