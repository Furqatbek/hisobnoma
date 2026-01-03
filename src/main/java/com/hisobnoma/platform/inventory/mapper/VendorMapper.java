package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateVendorRequest;
import com.hisobnoma.platform.inventory.dto.VendorDto;
import com.hisobnoma.platform.inventory.entity.Vendor;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface VendorMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    VendorDto toDto(Vendor vendor);

    List<VendorDto> toDtoList(List<Vendor> vendors);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    Vendor toEntity(CreateVendorRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true), nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    void updateEntity(CreateVendorRequest request, @MappingTarget Vendor vendor);
}
