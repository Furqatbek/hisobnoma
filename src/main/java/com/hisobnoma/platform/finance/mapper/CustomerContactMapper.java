package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateCustomerContactRequest;
import com.hisobnoma.platform.finance.dto.CustomerContactDto;
import com.hisobnoma.platform.finance.entity.CustomerContact;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface CustomerContactMapper {

    @Mapping(target = "customerId", source = "customer.id")
    CustomerContactDto toDto(CustomerContact customerContact);

    List<CustomerContactDto> toDtoList(List<CustomerContact> customerContacts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    CustomerContact toEntity(CreateCustomerContactRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateCustomerContactRequest request, @MappingTarget CustomerContact customerContact);
}
