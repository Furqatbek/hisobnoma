package com.hisobnoma.platform.mobile.dto;

import com.hisobnoma.platform.mobile.entity.DeviceToken;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Device token DTO for push notification registration.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeviceTokenDTO {

    private Long id;
    private String deviceId;
    private String fcmToken;
    private DeviceToken.Platform platform;
    private String deviceName;
    private String deviceModel;
    private String osVersion;
    private String appVersion;
    private Instant lastActiveAt;
    private boolean active;
}
