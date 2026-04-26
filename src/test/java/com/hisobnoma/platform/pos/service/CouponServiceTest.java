package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.pos.dto.CouponDto;
import com.hisobnoma.platform.pos.dto.CreateCouponRequest;
import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.entity.CouponRedemption;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.CouponStatus;
import com.hisobnoma.platform.pos.enums.PromotionType;
import com.hisobnoma.platform.pos.mapper.CouponMapper;
import com.hisobnoma.platform.pos.repository.CouponRedemptionRepository;
import com.hisobnoma.platform.pos.repository.CouponRepository;
import com.hisobnoma.platform.pos.repository.PromotionRepository;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceTest {

    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponRedemptionRepository couponRedemptionRepository;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private CouponMapper couponMapper;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private CouponService couponService;

    private Coupon coupon;
    private Promotion promotion;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        promotion = Promotion.builder()
                .id(1L)
                .code("PROMO10")
                .name("10% Off")
                .type(PromotionType.PERCENTAGE_OFF)
                .conditions(new ArrayList<>())
                .actions(new ArrayList<>())
                .coupons(new ArrayList<>())
                .build();

        coupon = Coupon.builder()
                .id(1L)
                .code("COUPON-001")
                .promotion(promotion)
                .status(CouponStatus.ACTIVE)
                .maxUses(100)
                .currentUses(0)
                .maxUsesPerCustomer(1)
                .tenantId(TENANT_ID)
                .redemptions(new ArrayList<>())
                .build();
    }

    // ==================== findAllCoupons ====================

    @Test
    void findAllCoupons_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Coupon> page = new PageImpl<>(List.of(coupon), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(couponMapper.toDto(any(Coupon.class))).thenReturn(
                CouponDto.builder().id(1L).code("COUPON-001").maxUses(100).currentUses(0).build());

        // When
        Page<CouponDto> result = couponService.findAllCoupons(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ==================== findCouponById ====================

    @Test
    void findCouponById_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(coupon));
        when(couponMapper.toDto(coupon)).thenReturn(
                CouponDto.builder().id(1L).code("COUPON-001").build());

        // When
        CouponDto result = couponService.findCouponById(1L);

        // Then
        assertNotNull(result);
        assertEquals("COUPON-001", result.getCode());
    }

    @Test
    void findCouponById_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> couponService.findCouponById(999L));
    }

    // ==================== findCouponByCode ====================

    @Test
    void findCouponByCode_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByCodeAndTenantId("COUPON-001", TENANT_ID)).thenReturn(Optional.of(coupon));
        when(couponMapper.toDto(coupon)).thenReturn(
                CouponDto.builder().id(1L).code("COUPON-001").build());

        // When
        CouponDto result = couponService.findCouponByCode("COUPON-001");

        // Then
        assertNotNull(result);
        assertEquals("COUPON-001", result.getCode());
    }

    @Test
    void findCouponByCode_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByCodeAndTenantId("INVALID", TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> couponService.findCouponByCode("INVALID"));
    }

    // ==================== createCoupon ====================

    @Test
    void createCoupon_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(promotion));
        when(couponRepository.findByCodeAndTenantId("NEW-COUPON", TENANT_ID)).thenReturn(Optional.empty());
        when(couponMapper.toEntity(any())).thenReturn(Coupon.builder()
                .code("NEW-COUPON").status(CouponStatus.ACTIVE).currentUses(0)
                .redemptions(new ArrayList<>()).build());
        when(couponRepository.save(any())).thenAnswer(inv -> {
            Coupon saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(couponMapper.toDto(any(Coupon.class))).thenReturn(
                CouponDto.builder().id(2L).code("NEW-COUPON").build());

        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("NEW-COUPON")
                .promotionId(1L)
                .build();

        // When
        CouponDto result = couponService.createCoupon(request);

        // Then
        assertNotNull(result);
        assertEquals("NEW-COUPON", result.getCode());
        verify(couponRepository).save(any());
    }

    @Test
    void createCoupon_duplicateCode_throwsIllegalArgumentException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(promotion));
        when(couponRepository.findByCodeAndTenantId("COUPON-001", TENANT_ID)).thenReturn(Optional.of(coupon));

        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("COUPON-001")
                .promotionId(1L)
                .build();

        // When/Then
        assertThrows(IllegalArgumentException.class, () -> couponService.createCoupon(request));
    }

    @Test
    void createCoupon_promotionNotFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("NEW-COUPON")
                .promotionId(999L)
                .build();

        // When/Then
        assertThrows(NotFoundException.class, () -> couponService.createCoupon(request));
    }

    // ==================== updateCoupon ====================

    @Test
    void updateCoupon_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(couponMapper.toDto(any(Coupon.class))).thenReturn(
                CouponDto.builder().id(1L).code("COUPON-001").description("Updated").build());

        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("COUPON-001")
                .promotionId(1L)
                .description("Updated description")
                .build();

        // When
        CouponDto result = couponService.updateCoupon(1L, request);

        // Then
        assertNotNull(result);
        verify(couponRepository).save(any());
    }

    @Test
    void updateCoupon_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        CreateCouponRequest request = CreateCouponRequest.builder()
                .code("X")
                .promotionId(1L)
                .build();

        // When/Then
        assertThrows(NotFoundException.class, () -> couponService.updateCoupon(999L, request));
    }

    // ==================== activateCoupon ====================

    @Test
    void activateCoupon_success() {
        // Given
        coupon.setStatus(CouponStatus.INACTIVE);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(couponMapper.toDto(any(Coupon.class))).thenReturn(
                CouponDto.builder().id(1L).status(CouponStatus.ACTIVE).build());

        // When
        CouponDto result = couponService.activateCoupon(1L);

        // Then
        assertNotNull(result);
        assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
    }

    // ==================== deactivateCoupon ====================

    @Test
    void deactivateCoupon_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(couponMapper.toDto(any(Coupon.class))).thenReturn(
                CouponDto.builder().id(1L).status(CouponStatus.INACTIVE).build());

        // When
        CouponDto result = couponService.deactivateCoupon(1L);

        // Then
        assertNotNull(result);
        assertEquals(CouponStatus.INACTIVE, coupon.getStatus());
    }

    // ==================== cancelCoupon ====================

    @Test
    void cancelCoupon_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(coupon));
        when(couponRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(couponMapper.toDto(any(Coupon.class))).thenReturn(
                CouponDto.builder().id(1L).status(CouponStatus.CANCELLED).build());

        // When
        CouponDto result = couponService.cancelCoupon(1L);

        // Then
        assertNotNull(result);
        assertEquals(CouponStatus.CANCELLED, coupon.getStatus());
    }

    // ==================== deleteCoupon ====================

    @Test
    void deleteCoupon_unused_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(coupon));

        // When
        couponService.deleteCoupon(1L);

        // Then
        verify(couponRepository).delete(coupon);
    }

    @Test
    void deleteCoupon_alreadyUsed_throwsIllegalStateException() {
        // Given
        coupon.setCurrentUses(5);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(coupon));

        // When/Then
        assertThrows(IllegalStateException.class, () -> couponService.deleteCoupon(1L));
        verify(couponRepository, never()).delete(any());
    }

    // ==================== getCouponRedemptions ====================

    @Test
    void getCouponRedemptions_returnsList() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(couponRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(coupon));
        when(couponRedemptionRepository.findByCouponId(1L)).thenReturn(Collections.emptyList());

        // When
        List<CouponRedemption> result = couponService.getCouponRedemptions(1L);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== generateCoupons ====================

    @Test
    void generateCoupons_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(promotion));
        when(couponRepository.findByCodeAndTenantId(any(), eq(TENANT_ID))).thenReturn(Optional.empty());
        when(couponRepository.save(any())).thenAnswer(inv -> {
            Coupon saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(couponMapper.toDto(any(Coupon.class))).thenReturn(
                CouponDto.builder().id(10L).code("GENERATED").build());

        CreateCouponRequest request = CreateCouponRequest.builder()
                .promotionId(1L)
                .maxUses(10)
                .build();

        // When
        List<CouponDto> result = couponService.generateCoupons(1L, 3, request);

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(couponRepository, times(3)).save(any());
    }

    // ==================== findCouponsByPromotion ====================

    @Test
    void findCouponsByPromotion_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Coupon> page = new PageImpl<>(List.of(coupon), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(promotion));
        when(couponRepository.findByPromotionId(1L, pageable)).thenReturn(page);
        when(couponMapper.toDto(any(Coupon.class))).thenReturn(
                CouponDto.builder().id(1L).code("COUPON-001").build());

        // When
        Page<CouponDto> result = couponService.findCouponsByPromotion(1L, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }
}
