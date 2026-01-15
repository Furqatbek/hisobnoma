package com.hisobnoma.platform.admin.dto;

import com.hisobnoma.platform.admin.entity.SystemSetting;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSettingDTO {
    private Long id;
    private Long tenantId;
    private String settingKey;
    private String settingValue;
    private String description;
    private String category;
    private SystemSetting.SettingValueType valueType;
    private boolean sensitive;
    private boolean active;
    private Long systemSettingId;
}
