package com.hisobnoma.platform.pos.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import com.hisobnoma.platform.pos.enums.PriceListType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * PriceList entity.
 * Represents a collection of prices for products, can be standard or customer-specific.
 */
@Entity
@Table(name = "price_lists", indexes = {
    @Index(name = "idx_price_lists_tenant", columnList = "tenant_id"),
    @Index(name = "idx_price_lists_code", columnList = "code"),
    @Index(name = "idx_price_lists_type", columnList = "type"),
    @Index(name = "idx_price_lists_active", columnList = "is_active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class PriceList extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PriceListType type = PriceListType.STANDARD;

    @Column(length = 3)
    @Builder.Default
    private String currency = "UZS";

    /**
     * Priority for overlapping price lists (higher = takes precedence)
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer priority = 0;

    /**
     * Percentage markup/markdown from base price (e.g., -10 = 10% discount)
     */
    @Column(name = "default_markup_percent", precision = 5, scale = 2)
    private BigDecimal defaultMarkupPercent;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;

    /**
     * If true, this is the default price list for new customers
     */
    @Column(name = "is_default")
    @Builder.Default
    private boolean defaultPriceList = false;

    /**
     * Location-specific price list (null = all locations)
     */
    @Column(name = "location_id")
    private Long locationId;

    @Column(length = 1000)
    private String notes;

    @OneToMany(mappedBy = "priceList", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PriceListItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "priceList", cascade = CascadeType.ALL)
    @Builder.Default
    private List<CustomerPriceList> customerAssignments = new ArrayList<>();

    public boolean isEffective(LocalDate date) {
        if (!active) return false;
        if (startDate != null && date.isBefore(startDate)) return false;
        if (endDate != null && date.isAfter(endDate)) return false;
        return true;
    }

    public void addItem(PriceListItem item) {
        items.add(item);
        item.setPriceList(this);
    }

    public void removeItem(PriceListItem item) {
        items.remove(item);
        item.setPriceList(null);
    }
}
