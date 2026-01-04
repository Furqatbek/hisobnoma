package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CouponDto;
import com.hisobnoma.platform.pos.dto.CreateCouponRequest;
import com.hisobnoma.platform.pos.entity.Coupon;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface CouponMapper {

    @Mapping(source = "promotion.id", target = "promotionId")
    @Mapping(source = "promotion.code", target = "promotionCode")
    @Mapping(source = "promotion.name", target = "promotionName")
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "redemptionCount", expression = "java(coupon.getRedemptions() != null ? coupon.getRedemptions().size() : 0)")
    CouponDto toDto(Coupon coupon);

    List<CouponDto> toDtoList(List<Coupon> coupons);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    @Mapping(target = "status", constant = "ACTIVE")
    @Mapping(target = "currentUses", constant = "0")
    @Mapping(target = "firstUsedAt", ignore = true)
    @Mapping(target = "lastUsedAt", ignore = true)
    @Mapping(target = "redemptions", ignore = true)
    Coupon toEntity(CreateCouponRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "promotion", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "currentUses", ignore = true)
    @Mapping(target = "firstUsedAt", ignore = true)
    @Mapping(target = "lastUsedAt", ignore = true)
    @Mapping(target = "redemptions", ignore = true)
    void updateEntity(CreateCouponRequest request, @MappingTarget Coupon coupon);
}
