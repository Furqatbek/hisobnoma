package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CreatePromotionRequest;
import com.hisobnoma.platform.pos.dto.PromotionDto;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.PromotionScope;
import com.hisobnoma.platform.pos.enums.PromotionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PromotionMapperTest {

    @Autowired
    private PromotionMapper promotionMapper;

    @Test
    void toDto_mapsAllFieldsExceptConditionsAndActions() {
        // Given
        Promotion promotion = Promotion.builder()
                .id(1L)
                .tenantId(1L)
                .code("PROMO-001")
                .name("Summer Sale")
                .description("20% off everything")
                .type(PromotionType.PERCENTAGE_OFF)
                .scope(PromotionScope.ORDER)
                .priority(10)
                .discountValue(new BigDecimal("20.0000"))
                .maxDiscountAmount(new BigDecimal("500000.0000"))
                .buyQuantity(2)
                .getQuantity(1)
                .getDiscountPercent(new BigDecimal("100.00"))
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 8, 31))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(21, 0))
                .daysOfWeek("MON,TUE,WED,THU,FRI")
                .active(true)
                .stackable(false)
                .requiresCoupon(true)
                .maxUses(1000)
                .currentUses(50)
                .maxUsesPerCustomer(5)
                .minOrderAmount(new BigDecimal("100000.0000"))
                .locationId(100L)
                .notes("Summer promotion notes")
                .conditions(new ArrayList<>())
                .actions(new ArrayList<>())
                .coupons(new ArrayList<>())
                .build();

        // When
        PromotionDto dto = promotionMapper.toDto(promotion);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("PROMO-001", dto.getCode());
        assertEquals("Summer Sale", dto.getName());
        assertEquals("20% off everything", dto.getDescription());
        assertEquals(PromotionType.PERCENTAGE_OFF, dto.getType());
        assertEquals(PromotionScope.ORDER, dto.getScope());
        assertEquals(10, dto.getPriority());
        assertEquals(new BigDecimal("20.0000"), dto.getDiscountValue());
        assertEquals(new BigDecimal("500000.0000"), dto.getMaxDiscountAmount());
        assertEquals(2, dto.getBuyQuantity());
        assertEquals(1, dto.getGetQuantity());
        assertEquals(new BigDecimal("100.00"), dto.getGetDiscountPercent());
        assertEquals(LocalDate.of(2024, 6, 1), dto.getStartDate());
        assertEquals(LocalDate.of(2024, 8, 31), dto.getEndDate());
        assertEquals(LocalTime.of(9, 0), dto.getStartTime());
        assertEquals(LocalTime.of(21, 0), dto.getEndTime());
        assertEquals("MON,TUE,WED,THU,FRI", dto.getDaysOfWeek());
        assertTrue(dto.isActive());
        assertFalse(dto.isStackable());
        assertTrue(dto.isRequiresCoupon());
        assertEquals(1000, dto.getMaxUses());
        assertEquals(50, dto.getCurrentUses());
        assertEquals(5, dto.getMaxUsesPerCustomer());
        assertEquals(new BigDecimal("100000.0000"), dto.getMinOrderAmount());
        assertEquals(100L, dto.getLocationId());
        assertEquals("Summer promotion notes", dto.getNotes());
        assertEquals(0, dto.getCouponCount());
        // toDto ignores conditions and actions
        assertNull(dto.getConditions());
        assertNull(dto.getActions());
    }

    @Test
    void toDtoWithDetails_includesConditionsAndActions() {
        // Given
        Promotion promotion = Promotion.builder()
                .id(1L)
                .code("PROMO-001")
                .name("Summer Sale")
                .type(PromotionType.PERCENTAGE_OFF)
                .scope(PromotionScope.ORDER)
                .conditions(new ArrayList<>())
                .actions(new ArrayList<>())
                .coupons(new ArrayList<>())
                .build();

        // When
        PromotionDto dto = promotionMapper.toDtoWithDetails(promotion);

        // Then
        assertNotNull(dto);
        assertNotNull(dto.getConditions());
        assertNotNull(dto.getActions());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .code("PROMO-002")
                .name("Winter Sale")
                .description("15% off")
                .type(PromotionType.PERCENTAGE_OFF)
                .scope(PromotionScope.LINE_ITEM)
                .priority(5)
                .discountValue(new BigDecimal("15.0000"))
                .maxUses(500)
                .maxUsesPerCustomer(3)
                .minOrderAmount(new BigDecimal("50000.0000"))
                .locationId(200L)
                .notes("Winter notes")
                .build();

        // When
        Promotion entity = promotionMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("PROMO-002", entity.getCode());
        assertEquals("Winter Sale", entity.getName());
        assertEquals("15% off", entity.getDescription());
        assertEquals(PromotionType.PERCENTAGE_OFF, entity.getType());
        assertEquals(PromotionScope.LINE_ITEM, entity.getScope());
        assertEquals(5, entity.getPriority());
        assertEquals(new BigDecimal("15.0000"), entity.getDiscountValue());
        assertTrue(entity.isActive()); // constant = "true"
        assertEquals(0, entity.getCurrentUses()); // constant = "0"
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
    }

    @Test
    void updateEntity_updatesFieldsOnExisting() {
        // Given
        Promotion existing = Promotion.builder()
                .id(1L)
                .tenantId(1L)
                .code("PROMO-001")
                .name("Old Name")
                .type(PromotionType.PERCENTAGE_OFF)
                .scope(PromotionScope.ORDER)
                .active(true)
                .currentUses(50)
                .conditions(new ArrayList<>())
                .actions(new ArrayList<>())
                .coupons(new ArrayList<>())
                .build();

        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .code("PROMO-001")
                .name("Updated Name")
                .description("Updated description")
                .type(PromotionType.PERCENTAGE_OFF)
                .build();

        // When
        promotionMapper.updateEntity(request, existing);

        // Then
        assertEquals("Updated Name", existing.getName());
        assertEquals("Updated description", existing.getDescription());
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
        assertEquals(50, existing.getCurrentUses()); // ignored in update
    }
}
