package com.hisobnoma.platform.mobile.mapper;

import com.hisobnoma.platform.mobile.dto.DeviceTokenDTO;
import com.hisobnoma.platform.mobile.dto.RegisterDeviceRequest;
import com.hisobnoma.platform.mobile.entity.DeviceToken;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeviceTokenMapper {

    DeviceTokenDTO toDto(DeviceToken entity);

    List<DeviceTokenDTO> toDtoList(List<DeviceToken> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "lastActiveAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    DeviceToken fromRequest(RegisterDeviceRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "deviceId", ignore = true)
    @Mapping(target = "lastActiveAt", ignore = true)
    @Mapping(target = "active", ignore = true)
    void updateFromRequest(RegisterDeviceRequest request, @MappingTarget DeviceToken entity);
}
