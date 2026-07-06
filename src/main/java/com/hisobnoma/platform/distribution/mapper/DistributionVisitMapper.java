package com.hisobnoma.platform.distribution.mapper;

import com.hisobnoma.platform.distribution.dto.DistributionVisitDto;
import com.hisobnoma.platform.distribution.entity.DistributionVisit;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DistributionVisitMapper {

    DistributionVisitDto toDto(DistributionVisit visit);
}
