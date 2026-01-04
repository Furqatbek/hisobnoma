package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.ShiftDto;
import com.hisobnoma.platform.pos.entity.Shift;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ShiftMapper {

    @Mapping(source = "terminal.id", target = "terminalId")
    @Mapping(source = "terminal.terminalCode", target = "terminalCode")
    @Mapping(source = "terminal.name", target = "terminalName")
    ShiftDto toDto(Shift shift);

    List<ShiftDto> toDtoList(List<Shift> shifts);
}
