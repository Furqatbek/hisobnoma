package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateCustomerRequest;
import com.hisobnoma.platform.finance.dto.CustomerDto;
import com.hisobnoma.platform.finance.entity.Customer;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface CustomerMapper {

    @Mapping(target = "availableCredit", expression = "java(customer.getAvailableCredit())")
    @Mapping(target = "overCreditLimit", expression = "java(customer.isOverCreditLimit())")
    CustomerDto toDto(Customer customer);

    List<CustomerDto> toDtoList(List<Customer> customers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "totalInvoiced", ignore = true)
    @Mapping(target = "totalReceived", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Customer toEntity(CreateCustomerRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "totalInvoiced", ignore = true)
    @Mapping(target = "totalReceived", ignore = true)
    @Mapping(target = "contacts", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateCustomerRequest request, @MappingTarget Customer customer);
}
