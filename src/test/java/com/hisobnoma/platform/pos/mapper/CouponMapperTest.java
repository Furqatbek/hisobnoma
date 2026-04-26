package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CouponDto;
import com.hisobnoma.platform.pos.dto.CreateCouponRequest;
import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.entity.CouponRedemption;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.CouponStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class CouponMapperTest {

    @Autowired
    private CouponMapper couponMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Promotion promotion = Promotion.builder()
                .id(10L)
                .code("PROMO-001")
                .name("Summer Sale")
                .build();

        List<CouponRedemption> redemptions = new ArrayList<>();

        Coupon coupon = Coupon.builder()
                .id(1L)
                .tenantId(1L)
                .code("COUPON-ABC")
                .promotion(promotion)
                .status(CouponStatus.ACTIVE)
                .description("Test coupon")
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .maxUses(100)
                .currentUses(5)
                .maxUsesPerCustomer(2)
                .customerId(30L)
                .customerEmail("test@example.com")
                .notes("Test notes")
                .redemptions(redemptions)
                .build();

        // When
        CouponDto dto = couponMapper.toDto(coupon);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("COUPON-ABC", dto.getCode());
        assertEquals(10L, dto.getPromotionId());
        assertEquals("PROMO-001", dto.getPromotionCode());
        assertEquals("Summer Sale", dto.getPromotionName());
        assertEquals(CouponStatus.ACTIVE, dto.getStatus());
        assertEquals("Test coupon", dto.getDescription());
        assertEquals(LocalDate.of(2024, 1, 1), dto.getStartDate());
        assertEquals(LocalDate.of(2024, 12, 31), dto.getEndDate());
        assertEquals(100, dto.getMaxUses());
        assertEquals(5, dto.getCurrentUses());
        assertEquals(2, dto.getMaxUsesPerCustomer());
        assertEquals(30L, dto.getCustomerId());
        assertEquals("test@example.com", dto.getCustomerEmail());
        assertEquals("Test notes", dto.getNotes());
        assertEquals(0, dto.getRedemptionCount());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("NEW-COUPON")
                .promotionId(10L)
                .description("New coupon")
                .startDate(LocalDate.of(2024, 6, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .maxUses(50)
                .maxUsesPerCustomer(1)
                .customerId(30L)
                .customerEmail("new@example.com")
                .notes("New coupon notes")
                .build();

        // When
        Coupon entity = couponMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("NEW-COUPON", entity.getCode());
        assertEquals("New coupon", entity.getDescription());
        assertEquals(LocalDate.of(2024, 6, 1), entity.getStartDate());
        assertEquals(LocalDate.of(2024, 12, 31), entity.getEndDate());
        assertEquals(50, entity.getMaxUses());
        assertEquals(1, entity.getMaxUsesPerCustomer());
        assertEquals(30L, entity.getCustomerId());
        assertEquals("new@example.com", entity.getCustomerEmail());
        assertEquals("New coupon notes", entity.getNotes());
        assertEquals(CouponStatus.ACTIVE, entity.getStatus()); // constant
        assertEquals(0, entity.getCurrentUses()); // constant
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getTenantId());
        assertNull(entity.getPromotion());
    }

    @Test
    void updateEntity_updatesFieldsOnExistingCoupon() {
        // Given
        Coupon existing = Coupon.builder()
                .id(1L)
                .tenantId(1L)
                .code("COUPON-ABC")
                .status(CouponStatus.ACTIVE)
                .currentUses(5)
                .redemptions(new ArrayList<>())
                .build();

        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("COUPON-ABC")
                .promotionId(10L)
                .description("Updated description")
                .maxUses(200)
                .build();

        // When
        couponMapper.updateEntity(request, existing);

        // Then
        assertEquals("Updated description", existing.getDescription());
        assertEquals(200, existing.getMaxUses());
        // ID and tenantId should remain unchanged
        assertEquals(1L, existing.getId());
        assertEquals(1L, existing.getTenantId());
        // Status and currentUses should remain unchanged (ignored in updateEntity)
        assertEquals(CouponStatus.ACTIVE, existing.getStatus());
        assertEquals(5, existing.getCurrentUses());
    }

    @Test
    void toDtoList_mapsAllCoupons() {
        // Given
        Promotion promotion = Promotion.builder()
                .id(10L).code("PROMO-001").name("Sale").build();

        Coupon c1 = Coupon.builder()
                .id(1L).code("C1").promotion(promotion)
                .status(CouponStatus.ACTIVE).redemptions(new ArrayList<>()).build();
        Coupon c2 = Coupon.builder()
                .id(2L).code("C2").promotion(promotion)
                .status(CouponStatus.ACTIVE).redemptions(new ArrayList<>()).build();

        // When
        List<CouponDto> dtos = couponMapper.toDtoList(List.of(c1, c2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
        assertEquals("C1", dtos.get(0).getCode());
        assertEquals("C2", dtos.get(1).getCode());
    }
}
