package com.hisobnoma.platform.telegram.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.telegram")
@Getter
@Setter
public class TelegramProperties {
    private boolean enabled = false;
    private String botToken;
    private String botUsername = "hisobnoma_bot";
}
