package com.hisobnoma.platform.inventory.entity;

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
 * Inventory Count entity for physical inventory counting.
 */
@Entity
@Table(name = "inventory_counts", indexes = {
    @Index(name = "idx_inv_count_tenant", columnList = "tenant_id"),
    @Index(name = "idx_inv_count_number", columnList = "count_number"),
    @Index(name = "idx_inv_count_location", columnList = "location_id"),
    @Index(name = "idx_inv_count_status", columnList = "status"),
    @Index(name = "idx_inv_count_date", columnList = "count_date")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class InventoryCount extends TenantAwareEntity {

    @Column(name = "count_number", nullable = false, unique = true, length = 50)
    private String countNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CountStatus status = CountStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "count_type", nullable = false, length = 20)
    @Builder.Default
    private CountType countType = CountType.PARTIAL;

    @Column(name = "count_date", nullable = false)
    private LocalDate countDate;

    @Column(length = 200)
    private String description;

    @Column(length = 1000)
    private String notes;

    @Column(name = "total_items")
    @Builder.Default
    private Integer totalItems = 0;

    @Column(name = "counted_items")
    @Builder.Default
    private Integer countedItems = 0;

    @Column(name = "variance_items")
    @Builder.Default
    private Integer varianceItems = 0;

    @Column(name = "total_variance_value", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal totalVarianceValue = BigDecimal.ZERO;

    @Column(name = "positive_variance_value", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal positiveVarianceValue = BigDecimal.ZERO;

    @Column(name = "negative_variance_value", precision = 18, scale = 4)
    @Builder.Default
    private BigDecimal negativeVarianceValue = BigDecimal.ZERO;

    @Column(name = "freeze_stock")
    @Builder.Default
    private boolean freezeStock = false;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "cancelled_by")
    private Long cancelledBy;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "adjustments_posted")
    @Builder.Default
    private boolean adjustmentsPosted = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "inventoryCount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryCountLine> lines = new ArrayList<>();

    public void addLine(InventoryCountLine line) {
        lines.add(line);
        line.setInventoryCount(this);
        updateSummary();
    }

    public void removeLine(InventoryCountLine line) {
        lines.remove(line);
        line.setInventoryCount(null);
        updateSummary();
    }

    public void updateSummary() {
        this.totalItems = lines.size();
        this.countedItems = (int) lines.stream()
                .filter(InventoryCountLine::isCounted)
                .count();
        this.varianceItems = (int) lines.stream()
                .filter(line -> line.isCounted() && line.hasVariance())
                .count();

        this.positiveVarianceValue = lines.stream()
                .filter(line -> line.isCounted() && line.getVarianceValue().compareTo(BigDecimal.ZERO) > 0)
                .map(InventoryCountLine::getVarianceValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.negativeVarianceValue = lines.stream()
                .filter(line -> line.isCounted() && line.getVarianceValue().compareTo(BigDecimal.ZERO) < 0)
                .map(InventoryCountLine::getVarianceValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .abs();

        this.totalVarianceValue = positiveVarianceValue.subtract(negativeVarianceValue);
    }

    public BigDecimal getCompletionPercentage() {
        if (totalItems == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(countedItems)
                .divide(BigDecimal.valueOf(totalItems), 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    public boolean isComplete() {
        return countedItems.equals(totalItems);
    }
}
