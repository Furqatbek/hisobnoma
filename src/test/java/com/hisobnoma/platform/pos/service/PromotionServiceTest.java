package com.hisobnoma.platform.pos.service;

import com.hisobnoma.platform.auth.security.SecurityContextHelper;
import com.hisobnoma.platform.common.exception.BusinessException;
import com.hisobnoma.platform.common.exception.NotFoundException;
import com.hisobnoma.platform.finance.repository.CustomerRepository;
import com.hisobnoma.platform.inventory.repository.ProductRepository;
import com.hisobnoma.platform.pos.dto.CreatePromotionRequest;
import com.hisobnoma.platform.pos.dto.PromotionDto;
import com.hisobnoma.platform.pos.entity.Coupon;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.enums.PromotionScope;
import com.hisobnoma.platform.pos.enums.PromotionType;
import com.hisobnoma.platform.pos.mapper.PromotionMapper;
import com.hisobnoma.platform.pos.repository.*;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private PromotionConditionRepository conditionRepository;
    @Mock
    private PromotionActionRepository actionRepository;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private CouponRedemptionRepository redemptionRepository;
    @Mock
    private PromotionMapper promotionMapper;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private POSTransactionRepository transactionRepository;
    @Mock
    private SecurityContextHelper securityContextHelper;

    @InjectMocks
    private PromotionService promotionService;

    private Promotion promotion;
    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        promotion = Promotion.builder()
                .id(1L)
                .code("PROMO10")
                .name("10% Off")
                .description("10% discount on all items")
                .type(PromotionType.PERCENTAGE_OFF)
                .scope(PromotionScope.ORDER)
                .priority(10)
                .discountValue(new BigDecimal("10"))
                .active(true)
                .stackable(false)
                .requiresCoupon(false)
                .currentUses(0)
                .tenantId(TENANT_ID)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(LocalDate.now().plusDays(30))
                .conditions(new ArrayList<>())
                .actions(new ArrayList<>())
                .coupons(new ArrayList<>())
                .build();
    }

    // ==================== findAll ====================

    @Test
    void getPromotions_returnsPaged() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Promotion> page = new PageImpl<>(List.of(promotion), pageable, 1);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByTenantId(TENANT_ID, pageable)).thenReturn(page);
        when(promotionMapper.toDto(any())).thenReturn(
                PromotionDto.builder().id(1L).code("PROMO10").build());

        // When
        Page<PromotionDto> result = promotionService.findAll(pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    // ==================== findActivePromotions ====================

    @Test
    void getActivePromotions_returnsOnlyActive() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findActivePromotions(eq(TENANT_ID), any(LocalDate.class), isNull()))
                .thenReturn(List.of(promotion));
        when(promotionMapper.toDto(any())).thenReturn(
                PromotionDto.builder().id(1L).active(true).build());

        // When
        List<PromotionDto> result = promotionService.findActivePromotions(null);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // ==================== findById ====================

    @Test
    void getPromotion_found_returnsDto() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(promotion));
        when(promotionMapper.toDtoWithDetails(promotion)).thenReturn(
                PromotionDto.builder().id(1L).code("PROMO10").build());

        // When
        PromotionDto result = promotionService.findById(1L);

        // Then
        assertNotNull(result);
        assertEquals("PROMO10", result.getCode());
    }

    @Test
    void getPromotion_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        // When/Then
        assertThrows(NotFoundException.class, () -> promotionService.findById(999L));
    }

    // ==================== create ====================

    @Test
    void createPromotion_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.existsByCodeAndTenantId("NEW-PROMO", TENANT_ID)).thenReturn(false);
        when(promotionMapper.toEntity(any())).thenReturn(Promotion.builder()
                .code("NEW-PROMO").name("New").type(PromotionType.PERCENTAGE_OFF)
                .conditions(new ArrayList<>()).actions(new ArrayList<>()).coupons(new ArrayList<>()).build());
        when(promotionRepository.save(any())).thenAnswer(inv -> {
            Promotion saved = inv.getArgument(0);
            saved.setId(2L);
            return saved;
        });
        when(promotionMapper.toDtoWithDetails(any())).thenReturn(
                PromotionDto.builder().id(2L).code("NEW-PROMO").build());

        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .code("NEW-PROMO")
                .name("New Promotion")
                .type(PromotionType.PERCENTAGE_OFF)
                .build();

        // When
        PromotionDto result = promotionService.create(request);

        // Then
        assertNotNull(result);
        assertEquals("NEW-PROMO", result.getCode());
        verify(promotionRepository, times(2)).save(any());
    }

    @Test
    void createPromotion_duplicateCode_throwsBusinessException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.existsByCodeAndTenantId("PROMO10", TENANT_ID)).thenReturn(true);

        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .code("PROMO10")
                .name("Duplicate")
                .type(PromotionType.PERCENTAGE_OFF)
                .build();

        // When/Then
        assertThrows(BusinessException.class, () -> promotionService.create(request));
    }

    // ==================== update ====================

    @Test
    void updatePromotion_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(promotion));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(promotionMapper.toDtoWithDetails(any())).thenReturn(
                PromotionDto.builder().id(1L).code("PROMO10").name("Updated").build());

        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .code("PROMO10")
                .name("Updated 10% Off")
                .type(PromotionType.PERCENTAGE_OFF)
                .build();

        // When
        PromotionDto result = promotionService.update(1L, request);

        // Then
        assertNotNull(result);
        verify(promotionRepository).save(any());
    }

    @Test
    void updatePromotion_notFound_throwsNotFoundException() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(999L, TENANT_ID)).thenReturn(Optional.empty());

        CreatePromotionRequest request = CreatePromotionRequest.builder()
                .code("X")
                .name("Not found")
                .type(PromotionType.PERCENTAGE_OFF)
                .build();

        // When/Then
        assertThrows(NotFoundException.class, () -> promotionService.update(999L, request));
    }

    // ==================== activate ====================

    @Test
    void activatePromotion_success() {
        // Given
        promotion.setActive(false);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(promotion));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(promotionMapper.toDtoWithDetails(any())).thenReturn(
                PromotionDto.builder().id(1L).active(true).build());

        // When
        PromotionDto result = promotionService.activate(1L);

        // Then
        assertNotNull(result);
        assertTrue(promotion.isActive());
    }

    // ==================== deactivate ====================

    @Test
    void deactivatePromotion_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(promotion));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(promotionMapper.toDtoWithDetails(any())).thenReturn(
                PromotionDto.builder().id(1L).active(false).build());

        // When
        PromotionDto result = promotionService.deactivate(1L);

        // Then
        assertNotNull(result);
        assertFalse(promotion.isActive());
    }

    @Test
    void deactivatePromotion_alreadyInactive_idempotent() {
        // Given
        promotion.setActive(false);
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(promotion));
        when(promotionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(promotionMapper.toDtoWithDetails(any())).thenReturn(
                PromotionDto.builder().id(1L).active(false).build());

        // When
        PromotionDto result = promotionService.deactivate(1L);

        // Then
        assertNotNull(result);
        assertFalse(promotion.isActive());
    }

    // ==================== delete ====================

    @Test
    void deletePromotion_noCouponsRedeemed_success() {
        // Given
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(promotion));
        when(couponRepository.findByPromotionId(1L)).thenReturn(Collections.emptyList());

        // When
        promotionService.delete(1L);

        // Then
        verify(promotionRepository).delete(promotion);
    }

    @Test
    void deletePromotion_hasRedeemedCoupons_throwsBusinessException() {
        // Given
        Coupon coupon = Coupon.builder().id(1L).currentUses(5).build();
        when(securityContextHelper.getCurrentTenantId()).thenReturn(TENANT_ID);
        when(promotionRepository.findByIdAndTenantId(1L, TENANT_ID)).thenReturn(Optional.of(promotion));
        when(couponRepository.findByPromotionId(1L)).thenReturn(List.of(coupon));

        // When/Then
        assertThrows(BusinessException.class, () -> promotionService.delete(1L));
        verify(promotionRepository, never()).delete(any());
    }
}
