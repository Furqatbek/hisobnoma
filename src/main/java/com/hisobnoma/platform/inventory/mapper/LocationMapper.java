package com.hisobnoma.platform.inventory.mapper;

import com.hisobnoma.platform.inventory.dto.CreateLocationRequest;
import com.hisobnoma.platform.inventory.dto.LocationDto;
import com.hisobnoma.platform.inventory.entity.Location;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    @Mapping(target = "parentLocationId", source = "parentLocation.id")
    @Mapping(target = "parentLocationName", source = "parentLocation.name")
    @Mapping(target = "fullPath", expression = "java(location.getFullPath())")
    @Mapping(target = "level", expression = "java(location.getLevel())")
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "isDefault", source = "default")
    LocationDto toDto(Location location);

    List<LocationDto> toDtoList(List<Location> locations);

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentLocation", ignore = true)
    @Mapping(target = "childLocations", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    Location toEntity(CreateLocationRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
                 builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "parentLocation", ignore = true)
    @Mapping(target = "childLocations", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(@MappingTarget Location location, CreateLocationRequest request);
}
