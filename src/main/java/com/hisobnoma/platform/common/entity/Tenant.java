package com.hisobnoma.platform.common.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "tenants", indexes = {
    @Index(name = "idx_tenants_code", columnList = "code")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class Tenant extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 255)
    private String description;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 100)
    private String contactEmail;

    @Column(length = 20)
    private String contactPhone;

    @Column(length = 255)
    private String address;

    @Column(length = 100)
    private String timezone;

    @Column(length = 10)
    private String currency;

    @Column(length = 10)
    private String locale;

    @Column(columnDefinition = "TEXT")
    private String settings;

    private Instant subscriptionExpiresAt;

    @Builder.Default
    @Column(nullable = false)
    private int maxUsers = 10;

    @Builder.Default
    @Column(nullable = false)
    private int maxLocations = 5;
}
