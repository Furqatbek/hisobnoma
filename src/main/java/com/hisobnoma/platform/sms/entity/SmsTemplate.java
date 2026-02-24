package com.hisobnoma.platform.sms.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "sms_templates", indexes = {
    @Index(name = "idx_sms_template_tenant", columnList = "tenant_id"),
    @Index(name = "idx_sms_template_code", columnList = "tenant_id, code", unique = true)
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class SmsTemplate extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 500)
    private String template;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
