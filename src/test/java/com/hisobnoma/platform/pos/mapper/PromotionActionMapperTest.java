package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CreatePromotionActionRequest;
import com.hisobnoma.platform.pos.dto.PromotionActionDto;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.entity.PromotionAction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PromotionActionMapperTest {

    @Autowired
    private PromotionActionMapper promotionActionMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Promotion promotion = Promotion.builder().id(10L).build();

        PromotionAction action = PromotionAction.builder()
                .id(1L)
                .promotion(promotion)
                .actionType("PERCENTAGE_OFF")
                .discountPercent(new BigDecimal("20.00"))
                .discountAmount(new BigDecimal("50000.0000"))
                .setPrice(new BigDecimal("100000.0000"))
                .maxDiscount(new BigDecimal("200000.0000"))
                .freeProductId(50L)
                .freeQuantity(2)
                .targetProductIds("1,2,3")
                .targetCategoryIds("10,20")
                .applyTo("CHEAPEST")
                .applyCount(1)
                .sortOrder(0)
                .notes("Action notes")
                .build();

        // When
        PromotionActionDto dto = promotionActionMapper.toDto(action);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getPromotionId());
        assertEquals("PERCENTAGE_OFF", dto.getActionType());
        assertEquals(new BigDecimal("20.00"), dto.getDiscountPercent());
        assertEquals(new BigDecimal("50000.0000"), dto.getDiscountAmount());
        assertEquals(new BigDecimal("100000.0000"), dto.getSetPrice());
        assertEquals(new BigDecimal("200000.0000"), dto.getMaxDiscount());
        assertEquals(50L, dto.getFreeProductId());
        assertEquals(2, dto.getFreeQuantity());
        assertEquals("1,2,3", dto.getTargetProductIds());
        assertEquals("10,20", dto.getTargetCategoryIds());
        assertEquals("CHEAPEST", dto.getApplyTo());
        assertEquals(1, dto.getApplyCount());
        assertEquals(0, dto.getSortOrder());
        assertEquals("Action notes", dto.getNotes());
        // freeProductName is ignored in mapping
        assertNull(dto.getFreeProductName());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreatePromotionActionRequest request = CreatePromotionActionRequest.builder()
                .actionType("FIXED_AMOUNT_OFF")
                .discountAmount(new BigDecimal("10000.0000"))
                .maxDiscount(new BigDecimal("50000.0000"))
                .targetProductIds("4,5,6")
                .applyTo("ALL")
                .sortOrder(1)
                .notes("New action")
                .build();

        // When
        PromotionAction entity = promotionActionMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals("FIXED_AMOUNT_OFF", entity.getActionType());
        assertEquals(new BigDecimal("10000.0000"), entity.getDiscountAmount());
        assertEquals(new BigDecimal("50000.0000"), entity.getMaxDiscount());
        assertEquals("4,5,6", entity.getTargetProductIds());
        assertEquals("ALL", entity.getApplyTo());
        assertEquals(1, entity.getSortOrder());
        assertEquals("New action", entity.getNotes());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getPromotion());
    }

    @Test
    void toDtoList_mapsAllActions() {
        // Given
        Promotion promotion = Promotion.builder().id(10L).build();

        PromotionAction a1 = PromotionAction.builder()
                .id(1L).promotion(promotion).actionType("PERCENTAGE_OFF").build();
        PromotionAction a2 = PromotionAction.builder()
                .id(2L).promotion(promotion).actionType("FIXED_AMOUNT_OFF").build();

        // When
        List<PromotionActionDto> dtos = promotionActionMapper.toDtoList(List.of(a1, a2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
