package com.hisobnoma.platform.inventory.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Category entity for hierarchical product categorization.
 * Supports parent-child relationships for nested categories.
 */
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_categories_tenant", columnList = "tenant_id"),
    @Index(name = "idx_categories_parent", columnList = "parent_id"),
    @Index(name = "idx_categories_code", columnList = "code")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Category extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 500)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Category> children = new ArrayList<>();

    @Column(nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Integer level = 0;

    @Column(length = 500)
    private String path;

    public void addChild(Category child) {
        children.add(child);
        child.setParent(this);
        child.setLevel(this.level + 1);
        child.updatePath();
    }

    public void removeChild(Category child) {
        children.remove(child);
        child.setParent(null);
    }

    public void updatePath() {
        if (parent == null) {
            this.path = "/" + this.getId();
        } else {
            this.path = parent.getPath() + "/" + this.getId();
        }
    }

    public boolean isRoot() {
        return parent == null;
    }

    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }
}
