package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.POSTransactionDto;
import com.hisobnoma.platform.pos.entity.POSTransaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {POSTransactionLineMapper.class, POSPaymentMapper.class})
public interface POSTransactionMapper {

    @Mapping(source = "terminal.id", target = "terminalId")
    @Mapping(source = "terminal.terminalCode", target = "terminalCode")
    @Mapping(source = "shift.id", target = "shiftId")
    @Mapping(source = "shift.shiftNumber", target = "shiftNumber")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(target = "balanceDue", expression = "java(transaction.getBalanceDue())")
    POSTransactionDto toDto(POSTransaction transaction);

    @Mapping(source = "terminal.id", target = "terminalId")
    @Mapping(source = "terminal.terminalCode", target = "terminalCode")
    @Mapping(source = "shift.id", target = "shiftId")
    @Mapping(source = "shift.shiftNumber", target = "shiftNumber")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(target = "balanceDue", expression = "java(transaction.getBalanceDue())")
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "payments", ignore = true)
    POSTransactionDto toDtoWithoutDetails(POSTransaction transaction);

    List<POSTransactionDto> toDtoList(List<POSTransaction> transactions);
}
