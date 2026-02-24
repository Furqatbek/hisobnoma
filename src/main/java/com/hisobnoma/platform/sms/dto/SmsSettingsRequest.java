package com.hisobnoma.platform.sms.dto;

import lombok.Data;

@Data
public class SmsSettingsRequest {
    private boolean enabled;
    private String apiToken;
    private String senderId;
}
