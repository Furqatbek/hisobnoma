package com.hisobnoma.platform.finance.mapper;

import com.hisobnoma.platform.finance.dto.BankReconciliationDto;
import com.hisobnoma.platform.finance.dto.CreateBankReconciliationRequest;
import com.hisobnoma.platform.finance.entity.BankReconciliation;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BankReconciliationMapper {

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(source = "bankAccount.id", target = "bankAccountId")
    @Mapping(source = "bankAccount.accountCode", target = "bankAccountCode")
    @Mapping(source = "bankAccount.accountName", target = "bankAccountName")
    BankReconciliationDto toDto(BankReconciliation reconciliation);

    List<BankReconciliationDto> toDtoList(List<BankReconciliation> reconciliations);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "reconciliationNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "bookBalance", ignore = true)
    @Mapping(target = "clearedDeposits", ignore = true)
    @Mapping(target = "clearedWithdrawals", ignore = true)
    @Mapping(target = "outstandingDeposits", ignore = true)
    @Mapping(target = "outstandingWithdrawals", ignore = true)
    @Mapping(target = "adjustedBankBalance", ignore = true)
    @Mapping(target = "adjustedBookBalance", ignore = true)
    @Mapping(target = "difference", ignore = true)
    @Mapping(target = "transactionsCleared", ignore = true)
    @Mapping(target = "transactionsOutstanding", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "completedBy", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "bankAccount", ignore = true)
    @Mapping(target = "reconciledTransactions", ignore = true)
    BankReconciliation toEntity(CreateBankReconciliationRequest request);

    @BeanMapping(builder = @Builder(disableBuilder = true), nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "reconciliationNumber", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "bookBalance", ignore = true)
    @Mapping(target = "clearedDeposits", ignore = true)
    @Mapping(target = "clearedWithdrawals", ignore = true)
    @Mapping(target = "outstandingDeposits", ignore = true)
    @Mapping(target = "outstandingWithdrawals", ignore = true)
    @Mapping(target = "adjustedBankBalance", ignore = true)
    @Mapping(target = "adjustedBookBalance", ignore = true)
    @Mapping(target = "difference", ignore = true)
    @Mapping(target = "transactionsCleared", ignore = true)
    @Mapping(target = "transactionsOutstanding", ignore = true)
    @Mapping(target = "completedAt", ignore = true)
    @Mapping(target = "completedBy", ignore = true)
    @Mapping(target = "cancelledAt", ignore = true)
    @Mapping(target = "cancelledBy", ignore = true)
    @Mapping(target = "cancellationReason", ignore = true)
    @Mapping(target = "bankAccount", ignore = true)
    @Mapping(target = "reconciledTransactions", ignore = true)
    void updateEntity(CreateBankReconciliationRequest request, @MappingTarget BankReconciliation reconciliation);
}
