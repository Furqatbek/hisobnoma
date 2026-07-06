package com.hisobnoma.platform.distribution.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Nested territory assignment carried inside a create/update agent request.
 * The full set replaces the agent's existing territories on update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistributionTerritoryRequest {

    @NotNull(message = "Region is required")
    private Long regionId;

    private Long villageId;

    @Builder.Default
    private Integer priority = 0;

    @Builder.Default
    private boolean exclusive = false;

    @Builder.Default
    private boolean active = true;
}
