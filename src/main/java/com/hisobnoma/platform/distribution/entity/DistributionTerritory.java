package com.hisobnoma.platform.distribution.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * A coverage assignment linking a {@link DistributionAgent} to a delivery region
 * (and optionally a specific village within it).
 *
 * <p>{@code regionId} / {@code villageId} reference {@code delivery_regions} /
 * {@code delivery_villages} by id, matching the loose cross-module id-reference
 * convention used elsewhere (e.g. {@code Customer.priceListId}).
 */
@Entity
@Table(name = "distribution_territories", indexes = {
    @Index(name = "idx_dist_territories_tenant", columnList = "tenant_id"),
    @Index(name = "idx_dist_territories_agent", columnList = "agent_id"),
    @Index(name = "idx_dist_territories_region", columnList = "region_id")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class DistributionTerritory extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private DistributionAgent agent;

    @Column(name = "region_id", nullable = false)
    private Long regionId;

    /** Optional narrowing to a single village; null means the whole region. */
    @Column(name = "village_id")
    private Long villageId;

    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    /** When true, no other agent should be assigned the same region/village. */
    @Column(nullable = false)
    @Builder.Default
    private boolean exclusive = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
