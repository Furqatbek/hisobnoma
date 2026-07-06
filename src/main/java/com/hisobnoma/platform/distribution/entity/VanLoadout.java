package com.hisobnoma.platform.distribution.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A van-sales loadout: stock loaded from a warehouse onto an agent's vehicle
 * (a {@code VEHICLE} inventory location), sold in the field, then reconciled
 * (returns, damages, cash) at end of day.
 */
@Entity
@Table(name = "distribution_van_loadouts",
        uniqueConstraints = @UniqueConstraint(name = "uk_van_loadouts_number", columnNames = {"tenant_id", "loadout_number"}),
        indexes = {
                @Index(name = "idx_van_loadouts_tenant", columnList = "tenant_id"),
                @Index(name = "idx_van_loadouts_status", columnList = "tenant_id, status"),
                @Index(name = "idx_van_loadouts_agent", columnList = "agent_id"),
                @Index(name = "idx_van_loadouts_created", columnList = "tenant_id, created_at")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class VanLoadout extends TenantAwareEntity {

    @Column(name = "loadout_number", nullable = false, length = 50)
    private String loadoutNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VanLoadoutStatus status = VanLoadoutStatus.DRAFT;

    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    /** The VEHICLE location stock is loaded onto. */
    @Column(name = "vehicle_location_id", nullable = false)
    private Long vehicleLocationId;

    /** The warehouse the stock is loaded from / returned to. */
    @Column(name = "source_location_id", nullable = false)
    private Long sourceLocationId;

    @Column(name = "loadout_date", nullable = false)
    private LocalDate loadoutDate;

    @Column(name = "reconciled_at")
    private Instant reconciledAt;

    @Column(name = "reconciled_by")
    private Long reconciledBy;

    @Column(name = "total_loaded_value", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalLoadedValue = BigDecimal.ZERO;

    @Column(name = "total_sold_value", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalSoldValue = BigDecimal.ZERO;

    @Column(name = "total_returned_value", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalReturnedValue = BigDecimal.ZERO;

    @Column(name = "total_damaged_value", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal totalDamagedValue = BigDecimal.ZERO;

    /** Value of sold goods at selling price — what the agent is expected to hand in. */
    @Column(name = "expected_cash", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal expectedCash = BigDecimal.ZERO;

    @Column(name = "actual_cash", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal actualCash = BigDecimal.ZERO;

    /** actualCash − expectedCash (negative = shortfall). */
    @Column(name = "cash_difference", precision = 18, scale = 4, nullable = false)
    @Builder.Default
    private BigDecimal cashDifference = BigDecimal.ZERO;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String currency = "UZS";

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "loadout", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VanLoadoutLine> lines = new ArrayList<>();

    public void addLine(VanLoadoutLine line) {
        line.setLoadout(this);
        this.lines.add(line);
    }
}
