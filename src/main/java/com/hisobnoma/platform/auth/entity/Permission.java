package com.hisobnoma.platform.auth.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "permissions", indexes = {
    @Index(name = "idx_permissions_code", columnList = "code"),
    @Index(name = "idx_permissions_module", columnList = "module")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Permission extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Module module;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Action action;

    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    public enum Module {
        INVENTORY,
        FINANCE,
        POS,
        ADMIN,
        REPORTS,
        WEB,
        MOBILE,
        HR,
        DELIVERY
    }

    public enum Action {
        CREATE,
        READ,
        UPDATE,
        DELETE,
        EXPORT,
        APPROVE,
        MANAGE,
        VIEW,
        GENERATE
    }
}
