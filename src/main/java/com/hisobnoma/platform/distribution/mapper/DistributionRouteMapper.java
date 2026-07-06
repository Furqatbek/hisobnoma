package com.hisobnoma.platform.distribution.mapper;

import com.hisobnoma.platform.distribution.dto.DistributionRouteDto;
import com.hisobnoma.platform.distribution.dto.DistributionRouteStopDto;
import com.hisobnoma.platform.distribution.entity.DistributionRoute;
import com.hisobnoma.platform.distribution.entity.DistributionRouteStop;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DistributionRouteMapper {

    DistributionRouteDto toDto(DistributionRoute route);

    DistributionRouteStopDto toDto(DistributionRouteStop stop);

    List<DistributionRouteDto> toDtoList(List<DistributionRoute> routes);
}
