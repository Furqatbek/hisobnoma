package com.hisobnoma.platform.admin.dto;

import com.hisobnoma.platform.admin.entity.SystemSetting;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingDTO {
    private Long id;
    private String settingKey;
    private String settingValue;
    private String defaultValue;
    private String description;
    private String category;
    private SystemSetting.SettingValueType valueType;
    private boolean sensitive;
    private boolean readonly;
    private boolean active;
    private String validationRegex;
    private Long minValue;
    private Long maxValue;
    private String allowedValues;
    private Integer sortOrder;
}
