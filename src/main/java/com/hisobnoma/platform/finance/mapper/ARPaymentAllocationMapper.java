package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.ARPaymentAllocationDto;
import com.hisobnoma.platform.finance.dto.CreateARPaymentAllocationRequest;
import com.hisobnoma.platform.finance.entity.ARPaymentAllocation;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ARPaymentAllocationMapper {

    @Mapping(target = "arPaymentId", source = "arPayment.id")
    @Mapping(target = "arInvoiceId", source = "arInvoice.id")
    @Mapping(target = "creditNoteId", source = "creditNote.id")
    @Mapping(target = "totalApplied", expression = "java(arPaymentAllocation.getTotalApplied())")
    ARPaymentAllocationDto toDto(ARPaymentAllocation arPaymentAllocation);

    List<ARPaymentAllocationDto> toDtoList(List<ARPaymentAllocation> arPaymentAllocations);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "arPayment", ignore = true)
    @Mapping(target = "arInvoice", ignore = true)
    @Mapping(target = "creditNote", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "invoiceAmount", ignore = true)
    @Mapping(target = "invoiceBalanceBefore", ignore = true)
    @Mapping(target = "invoiceBalanceAfter", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ARPaymentAllocation toEntity(CreateARPaymentAllocationRequest request);
}
