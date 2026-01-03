package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.APInvoiceDto;
import com.hisobnoma.platform.finance.dto.CreateAPInvoiceRequest;
import com.hisobnoma.platform.finance.entity.APInvoice;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring",
        uses = {APInvoiceLineMapper.class},
        builder = @Builder(disableBuilder = true))
public interface APInvoiceMapper {

    @Named("toDto")
    @Mapping(target = "overdue", expression = "java(apInvoice.isOverdue())")
    @Mapping(target = "daysOverdue", expression = "java(apInvoice.getDaysOverdue())")
    @Mapping(target = "daysUntilDue", expression = "java(apInvoice.getDaysUntilDue())")
    APInvoiceDto toDto(APInvoice apInvoice);

    @IterableMapping(qualifiedByName = "toDtoWithoutLines")
    List<APInvoiceDto> toDtoList(List<APInvoice> apInvoices);

    @Named("toDtoWithoutLines")
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "overdue", expression = "java(apInvoice.isOverdue())")
    @Mapping(target = "daysOverdue", expression = "java(apInvoice.getDaysOverdue())")
    @Mapping(target = "daysUntilDue", expression = "java(apInvoice.getDaysUntilDue())")
    APInvoiceDto toDtoWithoutLines(APInvoice apInvoice);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "vendorName", ignore = true)
    @Mapping(target = "purchaseOrderNumber", ignore = true)
    @Mapping(target = "receivingOrderNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "balanceDue", ignore = true)
    @Mapping(target = "matchingStatus", ignore = true)
    @Mapping(target = "matchingNotes", ignore = true)
    @Mapping(target = "glPosted", ignore = true)
    @Mapping(target = "glJournalEntryId", ignore = true)
    @Mapping(target = "glPostedAt", ignore = true)
    @Mapping(target = "submittedBy", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectedBy", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "internalNotes", ignore = true)
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    APInvoice toEntity(CreateAPInvoiceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "vendorName", ignore = true)
    @Mapping(target = "purchaseOrderNumber", ignore = true)
    @Mapping(target = "receivingOrderNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "subtotal", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "paidAmount", ignore = true)
    @Mapping(target = "balanceDue", ignore = true)
    @Mapping(target = "matchingStatus", ignore = true)
    @Mapping(target = "matchingNotes", ignore = true)
    @Mapping(target = "glPosted", ignore = true)
    @Mapping(target = "glJournalEntryId", ignore = true)
    @Mapping(target = "glPostedAt", ignore = true)
    @Mapping(target = "submittedBy", ignore = true)
    @Mapping(target = "submittedAt", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "rejectedBy", ignore = true)
    @Mapping(target = "rejectedAt", ignore = true)
    @Mapping(target = "rejectionReason", ignore = true)
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "internalNotes", ignore = true)
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(CreateAPInvoiceRequest request, @MappingTarget APInvoice apInvoice);
}
