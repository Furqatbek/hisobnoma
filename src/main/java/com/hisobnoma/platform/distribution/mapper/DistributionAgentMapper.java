package com.hisobnoma.platform.distribution.mapper;

import com.hisobnoma.platform.distribution.dto.CreateDistributionAgentRequest;
import com.hisobnoma.platform.distribution.dto.DistributionAgentDto;
import com.hisobnoma.platform.distribution.dto.DistributionTerritoryDto;
import com.hisobnoma.platform.distribution.entity.DistributionAgent;
import com.hisobnoma.platform.distribution.entity.DistributionTerritory;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DistributionAgentMapper {

    DistributionAgentDto toDto(DistributionAgent agent);

    DistributionTerritoryDto toDto(DistributionTerritory territory);

    List<DistributionAgentDto> toDtoList(List<DistributionAgent> agents);

    /**
     * Maps the scalar agent fields only. Territories are built and attached by the
     * service (they need the back-reference and tenant id set explicitly).
     */
    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tenantId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "updatedBy", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "terminatedAt", ignore = true)
    @Mapping(target = "territories", ignore = true)
    DistributionAgent toEntity(CreateDistributionAgentRequest request);
}
