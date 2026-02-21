package com.hisobnoma.platform.mobile.dto;

import com.hisobnoma.platform.mobile.entity.MobileAlert;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Alert preference DTO.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertPreferenceDTO {

    private Long id;
    private MobileAlert.AlertType alertType;
    private boolean pushEnabled;
    private boolean inAppEnabled;
    private boolean emailEnabled;
    private boolean smsEnabled;
    private boolean telegramEnabled;
    private Integer thresholdValue;
    private Integer quietHoursStart;
    private Integer quietHoursEnd;
}
