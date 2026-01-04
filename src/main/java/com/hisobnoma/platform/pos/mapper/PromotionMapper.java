package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CreatePromotionRequest;
import com.hisobnoma.platform.pos.dto.PromotionDto;
import com.hisobnoma.platform.pos.entity.Promotion;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {PromotionConditionMapper.class, PromotionActionMapper.class},
        builder = @Builder(disableBuilder = true))
public interface PromotionMapper {

    @Named("toDto")
    @Mapping(target = "conditions", ignore = true)
    @Mapping(target = "actions", ignore = true)
    @Mapping(target = "couponCount", expression = "java(promotion.getCoupons() != null ? promotion.getCoupons().size() : 0)")
    @Mapping(target = "locationName", ignore = true)
    PromotionDto toDto(Promotion promotion);

    @Named("toDtoWithDetails")
    @Mapping(target = "couponCount", expression = "java(promotion.getCoupons() != null ? promotion.getCoupons().size() : 0)")
    @Mapping(target = "locationName", ignore = true)
    PromotionDto toDtoWithDetails(Promotion promotion);

    @IterableMapping(qualifiedByName = "toDto")
    List<PromotionDto> toDtoList(List<Promotion> promotions);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "conditions", ignore = true)
    @Mapping(target = "actions", ignore = true)
    @Mapping(target = "coupons", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "currentUses", constant = "0")
    Promotion toEntity(CreatePromotionRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "conditions", ignore = true)
    @Mapping(target = "actions", ignore = true)
    @Mapping(target = "coupons", ignore = true)
    @Mapping(target = "currentUses", ignore = true)
    void updateEntity(CreatePromotionRequest request, @MappingTarget Promotion promotion);
}
