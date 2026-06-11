package com.hisobnoma.platform.web.entity;

import com.hisobnoma.platform.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "web_notifications",
        indexes = {
                @Index(name = "idx_web_notifications_customer", columnList = "tenant_id, web_customer_id, is_read"),
                @Index(name = "idx_web_notifications_created", columnList = "tenant_id, web_customer_id, created_at DESC")
        })
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class WebNotification extends BaseEntity {

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "web_customer_id", nullable = false)
    private Long webCustomerId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 1000)
    private String body;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String type = "GENERAL";

    @Column(name = "reference_type", length = 30)
    private String referenceType;

    @Column(name = "reference_id", length = 50)
    private String referenceId;

    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean read = false;
}
