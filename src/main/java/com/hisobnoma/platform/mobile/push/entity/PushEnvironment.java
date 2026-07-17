package com.hisobnoma.platform.mobile.push.entity;

/**
 * APNs delivery environment a device token belongs to. Each token MUST be delivered to the
 * matching Apple host — a token minted in one environment is rejected by the other.
 */
public enum PushEnvironment {
    SANDBOX,
    PRODUCTION;

    /** Lenient parse of the wire value ("sandbox"/"production"), defaulting to PRODUCTION. */
    public static PushEnvironment from(String value) {
        if (value == null) {
            return PRODUCTION;
        }
        return "sandbox".equalsIgnoreCase(value.trim()) ? SANDBOX : PRODUCTION;
    }
}
