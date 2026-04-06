package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CashOperationDto;
import com.hisobnoma.platform.pos.entity.CashOperation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CashOperationMapper {

    @Mapping(source = "shift.id", target = "shiftId")
    CashOperationDto toDto(CashOperation cashOperation);

    List<CashOperationDto> toDtoList(List<CashOperation> cashOperations);
}
