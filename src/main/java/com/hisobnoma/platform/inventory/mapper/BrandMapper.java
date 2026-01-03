package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.BrandDto;
import com.hisobnoma.platform.inventory.dto.CreateBrandRequest;
import com.hisobnoma.platform.inventory.entity.Brand;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrandMapper {

    BrandDto toDto(Brand brand);

    List<BrandDto> toDtoList(List<Brand> brands);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    Brand toEntity(CreateBrandRequest request);
}
