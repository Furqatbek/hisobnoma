package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CreatePromotionConditionRequest;
import com.hisobnoma.platform.pos.dto.PromotionConditionDto;
import com.hisobnoma.platform.pos.entity.PromotionCondition;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PromotionConditionMapper {

    @Mapping(source = "promotion.id", target = "promotionId")
    PromotionConditionDto toDto(PromotionCondition condition);

    List<PromotionConditionDto> toDtoList(List<PromotionCondition> conditions);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    PromotionCondition toEntity(CreatePromotionConditionRequest request);
}
