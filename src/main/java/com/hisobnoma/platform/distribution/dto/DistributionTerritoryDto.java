package com.hisobnoma.platform.distribution.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionTerritoryDto {
    private Long id;
    private Long regionId;
    private Long villageId;
    private Integer priority;
    private boolean exclusive;
    private boolean active;
}
