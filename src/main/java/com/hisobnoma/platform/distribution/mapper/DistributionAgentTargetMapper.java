package com.hisobnoma.platform.distribution.mapper;

import com.hisobnoma.platform.distribution.dto.DistributionAgentTargetDto;
import com.hisobnoma.platform.distribution.entity.DistributionAgentTarget;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DistributionAgentTargetMapper {

    DistributionAgentTargetDto toDto(DistributionAgentTarget target);

    List<DistributionAgentTargetDto> toDtoList(List<DistributionAgentTarget> targets);
}
