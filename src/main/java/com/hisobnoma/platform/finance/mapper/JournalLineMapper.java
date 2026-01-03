package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.CreateJournalLineRequest;
import com.hisobnoma.platform.finance.dto.JournalLineDto;
import com.hisobnoma.platform.finance.entity.JournalLine;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface JournalLineMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "journalEntry.id", target = "journalEntryId")
    @Mapping(source = "account.id", target = "accountId")
    @Mapping(source = "account.code", target = "accountCode")
    @Mapping(source = "account.name", target = "accountName")
    JournalLineDto toDto(JournalLine journalLine);

    List<JournalLineDto> toDtoList(List<JournalLine> journalLines);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "journalEntry", ignore = true)
    @Mapping(target = "lineNumber", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "baseDebitAmount", ignore = true)
    @Mapping(target = "baseCreditAmount", ignore = true)
    JournalLine toEntity(CreateJournalLineRequest request);
}
