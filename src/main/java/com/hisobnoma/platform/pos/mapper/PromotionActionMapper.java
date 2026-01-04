package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CreatePromotionActionRequest;
import com.hisobnoma.platform.pos.dto.PromotionActionDto;
import com.hisobnoma.platform.pos.entity.PromotionAction;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface PromotionActionMapper {

    @Mapping(source = "promotion.id", target = "promotionId")
    @Mapping(target = "freeProductName", ignore = true)
    PromotionActionDto toDto(PromotionAction action);

    List<PromotionActionDto> toDtoList(List<PromotionAction> actions);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    PromotionAction toEntity(CreatePromotionActionRequest request);
}
