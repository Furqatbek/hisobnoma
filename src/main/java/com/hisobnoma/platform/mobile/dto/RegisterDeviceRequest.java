package com.hisobnoma.platform.mobile.dto;

import com.hisobnoma.platform.mobile.entity.DeviceToken;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for registering a mobile device for push notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDeviceRequest {

    @NotBlank(message = "Device ID is required")
    private String deviceId;

    @NotBlank(message = "FCM token is required")
    private String fcmToken;

    @NotNull(message = "Platform is required")
    private DeviceToken.Platform platform;

    private String deviceName;
    private String deviceModel;
    private String osVersion;
    private String appVersion;
}
