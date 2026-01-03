package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Location entity representing inventory storage locations.
 * Supports hierarchical structure with parent-child relationships.
 */
@Entity
@Table(name = "locations", indexes = {
    @Index(name = "idx_locations_tenant", columnList = "tenant_id"),
    @Index(name = "idx_locations_code", columnList = "code"),
    @Index(name = "idx_locations_type", columnList = "location_type"),
    @Index(name = "idx_locations_parent", columnList = "parent_location_id"),
    @Index(name = "idx_locations_active", columnList = "active")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Location extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", nullable = false, length = 20)
    private LocationType locationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_location_id")
    private Location parentLocation;

    @OneToMany(mappedBy = "parentLocation", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Location> childLocations = new ArrayList<>();

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 100)
    private String country;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String email;

    @Column(name = "manager_name", length = 200)
    private String managerName;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean isDefault = false;

    @Column(name = "allow_negative_stock", nullable = false)
    @Builder.Default
    private boolean allowNegativeStock = false;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * Gets the full path of the location (e.g., "Warehouse A > Zone 1 > Bin A1")
     */
    public String getFullPath() {
        if (parentLocation == null) {
            return name;
        }
        return parentLocation.getFullPath() + " > " + name;
    }

    /**
     * Gets the level of the location in the hierarchy (0 = root)
     */
    public int getLevel() {
        if (parentLocation == null) {
            return 0;
        }
        return parentLocation.getLevel() + 1;
    }
}
