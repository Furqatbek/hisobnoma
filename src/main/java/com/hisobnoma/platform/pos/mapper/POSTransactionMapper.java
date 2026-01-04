package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.POSTransactionDto;
import com.hisobnoma.platform.pos.entity.POSTransaction;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring", uses = {POSTransactionLineMapper.class, POSPaymentMapper.class})
public interface POSTransactionMapper {

    @Named("toDto")
    @Mapping(source = "terminal.id", target = "terminalId")
    @Mapping(source = "terminal.terminalCode", target = "terminalCode")
    @Mapping(source = "shift.id", target = "shiftId")
    @Mapping(source = "shift.shiftNumber", target = "shiftNumber")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(target = "balanceDue", expression = "java(transaction.getBalanceDue())")
    POSTransactionDto toDto(POSTransaction transaction);

    @Named("toDtoWithoutDetails")
    @Mapping(source = "terminal.id", target = "terminalId")
    @Mapping(source = "terminal.terminalCode", target = "terminalCode")
    @Mapping(source = "shift.id", target = "shiftId")
    @Mapping(source = "shift.shiftNumber", target = "shiftNumber")
    @Mapping(source = "customer.id", target = "customerId")
    @Mapping(target = "balanceDue", expression = "java(transaction.getBalanceDue())")
    @Mapping(target = "lines", ignore = true)
    @Mapping(target = "payments", ignore = true)
    POSTransactionDto toDtoWithoutDetails(POSTransaction transaction);

    @IterableMapping(qualifiedByName = "toDtoWithoutDetails")
    List<POSTransactionDto> toDtoList(List<POSTransaction> transactions);
}
