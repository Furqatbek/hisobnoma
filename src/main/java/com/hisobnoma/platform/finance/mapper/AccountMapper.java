package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.AccountDto;
import com.hisobnoma.platform.finance.dto.CreateAccountRequest;
import com.hisobnoma.platform.finance.entity.Account;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface AccountMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "parentAccount.id", target = "parentAccountId")
    @Mapping(source = "parentAccount.code", target = "parentAccountCode")
    @Mapping(source = "parentAccount.name", target = "parentAccountName")
    AccountDto toDto(Account account);

    List<AccountDto> toDtoList(List<Account> accounts);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "parentAccount", ignore = true)
    @Mapping(target = "childAccounts", ignore = true)
    @Mapping(target = "accountLevel", ignore = true)
    @Mapping(target = "fullPath", ignore = true)
    @Mapping(target = "systemAccount", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "ytdDebit", ignore = true)
    @Mapping(target = "ytdCredit", ignore = true)
    @Mapping(target = "normalBalance", ignore = true)
    Account toEntity(CreateAccountRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true), nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "parentAccount", ignore = true)
    @Mapping(target = "childAccounts", ignore = true)
    @Mapping(target = "accountLevel", ignore = true)
    @Mapping(target = "fullPath", ignore = true)
    @Mapping(target = "systemAccount", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "currentBalance", ignore = true)
    @Mapping(target = "ytdDebit", ignore = true)
    @Mapping(target = "ytdCredit", ignore = true)
    @Mapping(target = "normalBalance", ignore = true)
    void updateEntity(CreateAccountRequest request, @MappingTarget Account account);

    @Named("toDtoWithChildren")
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "parentAccount.id", target = "parentAccountId")
    @Mapping(source = "parentAccount.code", target = "parentAccountCode")
    @Mapping(source = "parentAccount.name", target = "parentAccountName")
    @Mapping(source = "childAccounts", target = "childAccounts", qualifiedByName = "toDtoList")
    AccountDto toDtoWithChildren(Account account);

    @Named("toDtoList")
    List<AccountDto> toDtoListWithChildren(List<Account> accounts);
}
