package com.hisobnoma.platform.admin.entity;

import com.hisobnoma.platform.common.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * Tenant-specific settings that override system defaults.
 * Each tenant can customize their own configuration.
 */
@Entity
@Table(name = "tenant_settings", indexes = {
    @Index(name = "idx_tenant_settings_tenant", columnList = "tenant_id"),
    @Index(name = "idx_tenant_settings_key", columnList = "setting_key"),
    @Index(name = "idx_tenant_settings_category", columnList = "category")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uk_tenant_settings_tenant_key", columnNames = {"tenant_id", "setting_key"})
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSetting extends TenantAwareEntity {

    @Column(name = "setting_key", nullable = false, length = 100)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    @Builder.Default
    private SystemSetting.SettingValueType valueType = SystemSetting.SettingValueType.STRING;

    @Column(name = "is_sensitive", nullable = false)
    @Builder.Default
    private boolean sensitive = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "system_setting_id")
    private SystemSetting systemSetting;

    /**
     * Gets the value as boolean.
     */
    public boolean getBooleanValue() {
        return settingValue != null &&
               ("true".equalsIgnoreCase(settingValue) || "1".equals(settingValue) || "yes".equalsIgnoreCase(settingValue));
    }

    /**
     * Gets the value as integer.
     */
    public Integer getIntegerValue() {
        if (settingValue == null) return null;
        try {
            return Integer.parseInt(settingValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets the value as long.
     */
    public Long getLongValue() {
        if (settingValue == null) return null;
        try {
            return Long.parseLong(settingValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
