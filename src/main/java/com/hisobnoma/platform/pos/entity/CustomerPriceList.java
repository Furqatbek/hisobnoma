package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * CustomerPriceList entity.
 * Maps customers to specific price lists.
 */
@Entity
@Table(name = "customer_price_lists", indexes = {
    @Index(name = "idx_customer_price_lists_customer", columnList = "customer_id"),
    @Index(name = "idx_customer_price_lists_price_list", columnList = "price_list_id")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_customer_price_list", columnNames = {"customer_id", "price_list_id"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CustomerPriceList extends BaseEntity {

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "price_list_id", nullable = false)
    private PriceList priceList;

    /**
     * Priority when customer has multiple price lists
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    public boolean isEffective(LocalDate date) {
        if (!active) return false;
        if (startDate != null && date.isBefore(startDate)) return false;
        if (endDate != null && date.isAfter(endDate)) return false;
        return true;
    }
}
