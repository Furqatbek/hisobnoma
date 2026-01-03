package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.APPaymentAllocationDto;
import com.hisobnoma.platform.finance.dto.CreateAPPaymentAllocationRequest;
import com.hisobnoma.platform.finance.entity.APPaymentAllocation;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface APPaymentAllocationMapper {

    @Mapping(target = "apPaymentId", source = "apPayment.id")
    @Mapping(target = "apInvoiceId", source = "apInvoice.id")
    @Mapping(target = "totalApplied", expression = "java(allocation.getTotalApplied())")
    APPaymentAllocationDto toDto(APPaymentAllocation allocation);

    List<APPaymentAllocationDto> toDtoList(List<APPaymentAllocation> allocations);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "apPayment", ignore = true)
    @Mapping(target = "apInvoice", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "invoiceAmount", ignore = true)
    @Mapping(target = "invoiceBalanceBefore", ignore = true)
    @Mapping(target = "invoiceBalanceAfter", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    APPaymentAllocation toEntity(CreateAPPaymentAllocationRequest request);
}
