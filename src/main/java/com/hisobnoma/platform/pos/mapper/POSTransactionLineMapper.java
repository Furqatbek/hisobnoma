package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.POSTransactionLineDto;
import com.hisobnoma.platform.pos.entity.POSTransactionLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface POSTransactionLineMapper {

    @Mapping(source = "transaction.id", target = "transactionId")
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "variant.id", target = "variantId")
    @Mapping(source = "return", target = "isReturn")
    POSTransactionLineDto toDto(POSTransactionLine line);

    List<POSTransactionLineDto> toDtoList(List<POSTransactionLine> lines);
}
