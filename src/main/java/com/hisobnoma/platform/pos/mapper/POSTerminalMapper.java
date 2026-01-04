package com.hisobnoma.platform.pos.mapper;

import com.hisobnoma.platform.pos.dto.CreateTerminalRequest;
import com.hisobnoma.platform.pos.dto.POSTerminalDto;
import com.hisobnoma.platform.pos.entity.POSTerminal;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface POSTerminalMapper {

    @Mapping(source = "location.id", target = "locationId")
    @Mapping(source = "location.name", target = "locationName")
    POSTerminalDto toDto(POSTerminal terminal);

    List<POSTerminalDto> toDtoList(List<POSTerminal> terminals);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "lastSyncAt", ignore = true)
    @Mapping(target = "currentShiftId", ignore = true)
    POSTerminal toEntity(CreateTerminalRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "lastSyncAt", ignore = true)
    @Mapping(target = "currentShiftId", ignore = true)
    void updateEntity(CreateTerminalRequest request, @MappingTarget POSTerminal terminal);
}
