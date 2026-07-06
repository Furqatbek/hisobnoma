package com.hisobnoma.platform.distribution.mapper;

import com.hisobnoma.platform.distribution.dto.DistributionOrderDto;
import com.hisobnoma.platform.distribution.dto.DistributionOrderLineDto;
import com.hisobnoma.platform.distribution.entity.DistributionOrder;
import com.hisobnoma.platform.distribution.entity.DistributionOrderLine;
import org.mapstruct.Mapper;

import java.util.List;

/**
 * Read-side mapping only. Orders are constructed by the service (server-side
 * pricing, order-number generation, line back-references), never by MapStruct.
 */
@Mapper(componentModel = "spring")
public interface DistributionOrderMapper {

    DistributionOrderDto toDto(DistributionOrder order);

    DistributionOrderLineDto toDto(DistributionOrderLine line);

    List<DistributionOrderDto> toDtoList(List<DistributionOrder> orders);
}
