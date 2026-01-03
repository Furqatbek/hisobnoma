package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateVendorContactRequest;
import com.hisobnoma.platform.inventory.dto.VendorContactDto;
import com.hisobnoma.platform.inventory.entity.VendorContact;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface VendorContactMapper {

    @Mapping(target = "vendorId", source = "vendor.id")
    VendorContactDto toDto(VendorContact vendorContact);

    List<VendorContactDto> toDtoList(List<VendorContact> vendorContacts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    VendorContact toEntity(CreateVendorContactRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "vendor", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateVendorContactRequest request, @MappingTarget VendorContact vendorContact);
}
