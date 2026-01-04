package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.BankTransactionDto;
import com.hisobnoma.platform.finance.dto.CreateBankTransactionRequest;
import com.hisobnoma.platform.finance.entity.BankTransaction;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BankTransactionMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "bankAccount.id", target = "bankAccountId")
    @Mapping(source = "bankAccount.accountCode", target = "bankAccountCode")
    @Mapping(source = "bankAccount.accountName", target = "bankAccountName")
    @Mapping(source = "counterAccount.id", target = "counterAccountId")
    @Mapping(source = "counterAccount.code", target = "counterAccountCode")
    @Mapping(source = "counterAccount.name", target = "counterAccountName")
    @Mapping(source = "transferToAccount.id", target = "transferToAccountId")
    @Mapping(source = "transferToAccount.accountCode", target = "transferToAccountCode")
    @Mapping(source = "transferToAccount.accountName", target = "transferToAccountName")
    @Mapping(source = "reconciliation.id", target = "reconciliationId")
    BankTransactionDto toDto(BankTransaction transaction);

    List<BankTransactionDto> toDtoList(List<BankTransaction> transactions);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "transactionNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "debitAmount", ignore = true)
    @Mapping(target = "creditAmount", ignore = true)
    @Mapping(target = "runningBalance", ignore = true)
    @Mapping(target = "baseAmount", ignore = true)
    @Mapping(target = "glPosted", ignore = true)
    @Mapping(target = "glJournalEntryId", ignore = true)
    @Mapping(target = "reconciliation", ignore = true)
    @Mapping(target = "reconciledDate", ignore = true)
    @Mapping(target = "bankStatementLine", ignore = true)
    @Mapping(target = "bankAccount", ignore = true)
    @Mapping(target = "counterAccount", ignore = true)
    @Mapping(target = "transferToAccount", ignore = true)
    BankTransaction toEntity(CreateBankTransactionRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true), nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "transactionNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "debitAmount", ignore = true)
    @Mapping(target = "creditAmount", ignore = true)
    @Mapping(target = "runningBalance", ignore = true)
    @Mapping(target = "baseAmount", ignore = true)
    @Mapping(target = "glPosted", ignore = true)
    @Mapping(target = "glJournalEntryId", ignore = true)
    @Mapping(target = "reconciliation", ignore = true)
    @Mapping(target = "reconciledDate", ignore = true)
    @Mapping(target = "bankStatementLine", ignore = true)
    @Mapping(target = "bankAccount", ignore = true)
    @Mapping(target = "counterAccount", ignore = true)
    @Mapping(target = "transferToAccount", ignore = true)
    void updateEntity(CreateBankTransactionRequest request, @MappingTarget BankTransaction transaction);
}
