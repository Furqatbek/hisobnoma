package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.APPaymentDto;
import com.hisobnoma.platform.finance.dto.CreateAPPaymentRequest;
import com.hisobnoma.platform.finance.entity.APPayment;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {APPaymentAllocationMapper.class},
        builder = @Builder(disableBuilder = true))
public interface APPaymentMapper {

    @Named("toDto")
    @Mapping(target = "fullyAllocated", expression = "java(apPayment.isFullyAllocated())")
    @Mapping(target = "availableAmount", expression = "java(apPayment.getAvailableAmount())")
    APPaymentDto toDto(APPayment apPayment);

    @IterableMapping(qualifiedByName = "toDtoWithoutAllocations")
    List<APPaymentDto> toDtoList(List<APPayment> apPayments);

    @Named("toDtoWithoutAllocations")
    @Mapping(target = "allocations", ignore = true)
    @Mapping(target = "fullyAllocated", expression = "java(apPayment.isFullyAllocated())")
    @Mapping(target = "availableAmount", expression = "java(apPayment.getAvailableAmount())")
    APPaymentDto toDtoWithoutAllocations(APPayment apPayment);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "paymentNumber", ignore = true)
    @Mapping(target = "vendorName", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "allocatedAmount", ignore = true)
    @Mapping(target = "unallocatedAmount", ignore = true)
    @Mapping(target = "bankAccountName", ignore = true)
    @Mapping(target = "glPosted", ignore = true)
    @Mapping(target = "glJournalEntryId", ignore = true)
    @Mapping(target = "glPostedAt", ignore = true)
    @Mapping(target = "reconciled", ignore = true)
    @Mapping(target = "reconciledAt", ignore = true)
    @Mapping(target = "reconciledBy", ignore = true)
    @Mapping(target = "submittedBy", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "completedBy", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "voidedBy", ignore = true)
    @Mapping(target = "voidedAt", ignore = true)
    @Mapping(target = "voidReason", ignore = true)
    @Mapping(target = "allocations", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    APPayment toEntity(CreateAPPaymentRequest request);
}
