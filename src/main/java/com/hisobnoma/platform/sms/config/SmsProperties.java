package com.hisobnoma.platform.sms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.sms")
@Getter
@Setter
public class SmsProperties {
    private boolean enabled = false;
    private String apiToken;
    private String baseUrl = "https://devsms.uz/api";
    private String senderId = "4546";
}
