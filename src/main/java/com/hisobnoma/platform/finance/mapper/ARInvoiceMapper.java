package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.ARInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateARInvoiceRequest;
import com.hisobnoma.platform.finance.entity.ARInvoice;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ARInvoiceLineMapper.class}, builder = @Builder(disableBuilder = true))
public interface ARInvoiceMapper {

    @Mapping(target = "overdue", expression = "java(arInvoice.isOverdue())")
    @Mapping(target = "daysOverdue", expression = "java(arInvoice.getDaysOverdue())")
    @Mapping(target = "daysUntilDue", expression = "java(arInvoice.getDaysUntilDue())")
    ARInvoiceDto toDto(ARInvoice arInvoice);

    @Named("toDtoWithoutLines")
    @Mapping(target = "overdue", expression = "java(arInvoice.isOverdue())")
    @Mapping(target = "daysOverdue", expression = "java(arInvoice.getDaysOverdue())")
    @Mapping(target = "daysUntilDue", expression = "java(arInvoice.getDaysUntilDue())")
    @Mapping(target = "lines", ignore = true)
    ARInvoiceDto toDtoWithoutLines(ARInvoice arInvoice);

    @IterableMapping(qualifiedByName = "toDtoWithoutLines")
    List<ARInvoiceDto> toDtoList(List<ARInvoice> arInvoices);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "customerEmail", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "posTransactionNumber", ignore = true)
    @Mapping(target = "salesOrderNumber", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "taxAmount", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "creditApplied", ignore = true)
    @Mapping(target = "balanceDue", ignore = true)
    @Mapping(target = "glPosted", ignore = true)
    @Mapping(target = "glJournalEntryId", ignore = true)
    @Mapping(target = "glPostedAt", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "sentBy", ignore = true)
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "writtenOffBy", ignore = true)
    @Mapping(target = "writtenOffAt", ignore = true)
    @Mapping(target = "writeOffReason", ignore = true)
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ARInvoice toEntity(CreateARInvoiceRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateARInvoiceRequest request, @MappingTarget ARInvoice arInvoice);
}
