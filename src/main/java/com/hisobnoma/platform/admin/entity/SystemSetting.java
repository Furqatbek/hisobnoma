package com.hisobnoma.platform.admin.entity;

import com.hisobnoma.platform.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * System-wide settings that apply globally across all tenants.
 * These are administrative settings managed by super admins.
 */
@Entity
@Table(name = "system_settings", indexes = {
    @Index(name = "idx_system_settings_key", columnList = "setting_key", unique = true),
    @Index(name = "idx_system_settings_category", columnList = "category")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class SystemSetting extends AuditableEntity {

    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    @Builder.Default
    private SettingValueType valueType = SettingValueType.STRING;

    @Column(name = "is_sensitive", nullable = false)
    @Builder.Default
    private boolean sensitive = false;

    @Column(name = "is_readonly", nullable = false)
    @Builder.Default
    private boolean readonly = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "validation_regex", length = 500)
    private String validationRegex;

    @Column(name = "min_value")
    private Long minValue;

    @Column(name = "max_value")
    private Long maxValue;

    @Column(name = "allowed_values", columnDefinition = "TEXT")
    private String allowedValues;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    public enum SettingValueType {
        STRING,
        INTEGER,
        DECIMAL,
        BOOLEAN,
        JSON,
        ENUM,
        DATE,
        DATETIME
    }

    /**
     * Gets the effective value (setting value if set, otherwise default).
     */
    public String getEffectiveValue() {
        return settingValue != null ? settingValue : defaultValue;
    }

    /**
     * Gets the value as boolean.
     */
    public boolean getBooleanValue() {
        String value = getEffectiveValue();
        return value != null && ("true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value));
    }

    /**
     * Gets the value as integer.
     */
    public Integer getIntegerValue() {
        String value = getEffectiveValue();
        if (value == null) return null;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Gets the value as long.
     */
    public Long getLongValue() {
        String value = getEffectiveValue();
        if (value == null) return null;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
