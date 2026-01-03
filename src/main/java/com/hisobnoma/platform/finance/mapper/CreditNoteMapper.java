package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateCreditNoteRequest;
import com.hisobnoma.platform.finance.dto.CreditNoteDto;
import com.hisobnoma.platform.finance.entity.CreditNote;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface CreditNoteMapper {

    CreditNoteDto toDto(CreditNote creditNote);

    List<CreditNoteDto> toDtoList(List<CreditNote> creditNotes);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "creditNoteNumber", ignore = true)
    @Mapping(target = "customerName", ignore = true)
    @Mapping(target = "originalInvoiceNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalAmount", ignore = true)
    @Mapping(target = "appliedAmount", ignore = true)
    @Mapping(target = "balance", ignore = true)
    @Mapping(target = "refundedAmount", ignore = true)
    @Mapping(target = "glPosted", ignore = true)
    @Mapping(target = "glJournalEntryId", ignore = true)
    @Mapping(target = "glPostedAt", ignore = true)
    @Mapping(target = "approvedBy", ignore = true)
    @Mapping(target = "approvedAt", ignore = true)
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    CreditNote toEntity(CreateCreditNoteRequest request);
}
