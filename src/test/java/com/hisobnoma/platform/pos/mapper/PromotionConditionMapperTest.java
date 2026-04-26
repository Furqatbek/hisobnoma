package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CreatePromotionConditionRequest;
import com.hisobnoma.platform.pos.dto.PromotionConditionDto;
import com.hisobnoma.platform.pos.entity.Promotion;
import com.hisobnoma.platform.pos.entity.PromotionCondition;
import com.hisobnoma.platform.pos.enums.PromotionConditionType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class PromotionConditionMapperTest {

    @Autowired
    private PromotionConditionMapper promotionConditionMapper;

    @Test
    void toDto_mapsAllFields() {
        // Given
        Promotion promotion = Promotion.builder().id(10L).build();

        PromotionCondition condition = PromotionCondition.builder()
                .id(1L)
                .promotion(promotion)
                .conditionType(PromotionConditionType.MINIMUM_PURCHASE)
                .operator("GTE")
                .value("100000")
                .value2("500000")
                .thresholdAmount(new BigDecimal("100000.0000"))
                .productIds("1,2,3")
                .categoryIds("10,20")
                .brandIds("5,6")
                .customerGroups("VIP,WHOLESALE")
                .required(true)
                .notes("Minimum purchase condition")
                .build();

        // When
        PromotionConditionDto dto = promotionConditionMapper.toDto(condition);

        // Then
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals(10L, dto.getPromotionId());
        assertEquals(PromotionConditionType.MINIMUM_PURCHASE, dto.getConditionType());
        assertEquals("GTE", dto.getOperator());
        assertEquals("100000", dto.getValue());
        assertEquals("500000", dto.getValue2());
        assertEquals(new BigDecimal("100000.0000"), dto.getThresholdAmount());
        assertEquals("1,2,3", dto.getProductIds());
        assertEquals("10,20", dto.getCategoryIds());
        assertEquals("5,6", dto.getBrandIds());
        assertEquals("VIP,WHOLESALE", dto.getCustomerGroups());
        assertTrue(dto.isRequired());
        assertEquals("Minimum purchase condition", dto.getNotes());
    }

    @Test
    void toEntity_fromCreateRequest_mapsBasicFields() {
        // Given
        CreatePromotionConditionRequest request = CreatePromotionConditionRequest.builder()
                .conditionType(PromotionConditionType.MINIMUM_PURCHASE)
                .operator("GTE")
                .value("50000")
                .thresholdAmount(new BigDecimal("50000.0000"))
                .productIds("4,5")
                .required(true)
                .notes("New condition")
                .build();

        // When
        PromotionCondition entity = promotionConditionMapper.toEntity(request);

        // Then
        assertNotNull(entity);
        assertEquals(PromotionConditionType.MINIMUM_PURCHASE, entity.getConditionType());
        assertEquals("GTE", entity.getOperator());
        assertEquals("50000", entity.getValue());
        assertEquals(new BigDecimal("50000.0000"), entity.getThresholdAmount());
        assertEquals("4,5", entity.getProductIds());
        assertTrue(entity.isRequired());
        assertEquals("New condition", entity.getNotes());
        // Ignored fields
        assertNull(entity.getId());
        assertNull(entity.getPromotion());
    }

    @Test
    void toDtoList_mapsAllConditions() {
        // Given
        Promotion promotion = Promotion.builder().id(10L).build();

        PromotionCondition c1 = PromotionCondition.builder()
                .id(1L).promotion(promotion)
                .conditionType(PromotionConditionType.MINIMUM_PURCHASE).build();
        PromotionCondition c2 = PromotionCondition.builder()
                .id(2L).promotion(promotion)
                .conditionType(PromotionConditionType.MINIMUM_PURCHASE).build();

        // When
        List<PromotionConditionDto> dtos = promotionConditionMapper.toDtoList(List.of(c1, c2));

        // Then
        assertNotNull(dtos);
        assertEquals(2, dtos.size());
    }
}
