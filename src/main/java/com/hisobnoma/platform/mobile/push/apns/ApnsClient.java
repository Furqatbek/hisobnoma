package com.hisobnoma.platform.mobile.push.apns;

import com.hisobnoma.platform.mobile.push.entity.PushEnvironment;

/**
 * Sends a single notification to one device token via APNs. Implementations must route to the
 * host matching the token's {@link PushEnvironment} (sandbox vs production).
 */
public interface ApnsClient {

    boolean isConfigured();

    ApnsResult send(String deviceToken, PushEnvironment environment, ApnsPayload payload);
}
