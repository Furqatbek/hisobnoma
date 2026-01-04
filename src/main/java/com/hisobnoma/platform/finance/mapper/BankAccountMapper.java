package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.BankAccountDto;
import com.hisobnoma.platform.finance.dto.CreateBankAccountRequest;
import com.hisobnoma.platform.finance.entity.BankAccount;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BankAccountMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "glAccount.id", target = "glAccountId")
    @Mapping(source = "glAccount.code", target = "glAccountCode")
    @Mapping(source = "glAccount.name", target = "glAccountName")
    BankAccountDto toDto(BankAccount bankAccount);

    List<BankAccountDto> toDtoList(List<BankAccount> bankAccounts);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "availableBalance", ignore = true)
    @Mapping(target = "lastReconciledDate", ignore = true)
    @Mapping(target = "lastReconciledBalance", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "glAccount", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "reconciliations", ignore = true)
    BankAccount toEntity(CreateBankAccountRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true), nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "availableBalance", ignore = true)
    @Mapping(target = "lastReconciledDate", ignore = true)
    @Mapping(target = "lastReconciledBalance", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "glAccount", ignore = true)
    @Mapping(target = "transactions", ignore = true)
    @Mapping(target = "reconciliations", ignore = true)
    void updateEntity(CreateBankAccountRequest request, @MappingTarget BankAccount bankAccount);
}
