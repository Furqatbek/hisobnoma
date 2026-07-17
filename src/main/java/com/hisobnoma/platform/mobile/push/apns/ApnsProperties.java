package com.hisobnoma.platform.mobile.push.apns;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * APNs credentials/config. Disabled by default — with no {@code .p8} key, team id and key id
 * the push subsystem is a logged no-op, so it never blocks startup or tests. Flip on in prod
 * by setting the env vars.
 */
@Component
@ConfigurationProperties(prefix = "hisobnoma.push.apns")
@Getter
@Setter
public class ApnsProperties {

    private boolean enabled = false;

    /** Apple Developer Team ID (10 chars) — the JWT {@code iss}. */
    private String teamId;

    /** APNs Auth Key ID (10 chars) — the JWT header {@code kid}. */
    private String keyId;

    /** App bundle id → APNs {@code apns-topic}. */
    private String bundleId = "com.hisobnoma.admin";

    /** Contents of the {@code .p8} auth key (PKCS#8 PEM). */
    private String privateKey;

    public boolean isConfigured() {
        return enabled
                && notBlank(teamId) && notBlank(keyId) && notBlank(bundleId) && notBlank(privateKey);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
