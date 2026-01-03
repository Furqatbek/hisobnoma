package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.ARPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateARPaymentRequest;
import com.hisobnoma.platform.finance.entity.ARPayment;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ARPaymentAllocationMapper.class}, builder = @Builder(disableBuilder = true))
public interface ARPaymentMapper {

    @Mapping(target = "fullyAllocated", expression = "java(arPayment.isFullyAllocated())")
    @Mapping(target = "availableAmount", expression = "java(arPayment.getAvailableAmount())")
    ARPaymentDto toDto(ARPayment arPayment);

    @Named("toDtoWithoutAllocations")
    @Mapping(target = "fullyAllocated", expression = "java(arPayment.isFullyAllocated())")
    @Mapping(target = "availableAmount", expression = "java(arPayment.getAvailableAmount())")
    @Mapping(target = "allocations", ignore = true)
    ARPaymentDto toDtoWithoutAllocations(ARPayment arPayment);

    @IterableMapping(qualifiedByName = "toDtoWithoutAllocations")
    List<ARPaymentDto> toDtoList(List<ARPayment> arPayments);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "paymentNumber", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "allocatedAmount", ignore = true)
    @Mapping(target = "unallocatedAmount", ignore = true)
    @Mapping(target = "bankAccountName", ignore = true)
    @Mapping(target = "glPosted", ignore = true)
    @Mapping(target = "glJournalEntryId", ignore = true)
    @Mapping(target = "glPostedAt", ignore = true)
    @Mapping(target = "deposited", ignore = true)
    @Mapping(target = "depositedAt", ignore = true)
    @Mapping(target = "depositedBy", ignore = true)
    @Mapping(target = "depositReference", ignore = true)
    @Mapping(target = "processedBy", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "refundedBy", ignore = true)
    @Mapping(target = "refundedAt", ignore = true)
    @Mapping(target = "refundReason", ignore = true)
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "allocations", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ARPayment toEntity(CreateARPaymentRequest request);
}
