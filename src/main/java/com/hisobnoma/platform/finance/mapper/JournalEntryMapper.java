package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateJournalEntryRequest;
import com.hisobnoma.platform.finance.dto.JournalEntryDto;
import com.hisobnoma.platform.finance.entity.JournalEntry;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", uses = {JournalLineMapper.class})
public interface JournalEntryMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "fiscalPeriod.id", target = "fiscalPeriodId")
    @Mapping(source = "fiscalPeriod.name", target = "fiscalPeriodName")
    @Mapping(expression = "java(journalEntry.isBalanced())", target = "balanced")
    JournalEntryDto toDto(JournalEntry journalEntry);

    List<JournalEntryDto> toDtoList(List<JournalEntry> journalEntries);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "lines", ignore = true)
    @Mapping(source = "fiscalPeriod.id", target = "fiscalPeriodId")
    @Mapping(source = "fiscalPeriod.name", target = "fiscalPeriodName")
    @Mapping(expression = "java(journalEntry.isBalanced())", target = "balanced")
    JournalEntryDto toDtoWithoutLines(JournalEntry journalEntry);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "entryNumber", ignore = true)
    @Mapping(target = "fiscalPeriod", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalDebit", ignore = true)
    @Mapping(target = "totalCredit", ignore = true)
    @Mapping(target = "recurring", ignore = true)
    @Mapping(target = "recurringTemplateId", ignore = true)
    @Mapping(target = "reversedEntryId", ignore = true)
    @Mapping(target = "reversingEntryId", ignore = true)
    @Mapping(target = "postedAt", ignore = true)
    @Mapping(target = "postedBy", ignore = true)
    @Mapping(target = "reversedAt", ignore = true)
    @Mapping(target = "reversedBy", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "lines", ignore = true)
    JournalEntry toEntity(CreateJournalEntryRequest request);
}
