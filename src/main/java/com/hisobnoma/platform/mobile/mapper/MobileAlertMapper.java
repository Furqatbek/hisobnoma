package com.hisobnoma.platform.mobile.mapper;

import com.hisobnoma.platform.mobile.dto.MobileAlertDTO;
import com.hisobnoma.platform.mobile.entity.MobileAlert;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MobileAlertMapper {

    MobileAlertDTO toDto(MobileAlert entity);

    List<MobileAlertDTO> toDtoList(List<MobileAlert> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    @Mapping(target = "sentViaPush", ignore = true)
    @Mapping(target = "pushSentAt", ignore = true)
    MobileAlert toEntity(MobileAlertDTO dto);
}
