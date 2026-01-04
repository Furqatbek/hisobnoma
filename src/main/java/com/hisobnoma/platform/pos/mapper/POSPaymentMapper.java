package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.POSPaymentDto;
import com.hisobnoma.platform.pos.entity.POSPayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface POSPaymentMapper {

    @Mapping(source = "transaction.id", target = "transactionId")
    POSPaymentDto toDto(POSPayment payment);

    List<POSPaymentDto> toDtoList(List<POSPayment> payments);
}
