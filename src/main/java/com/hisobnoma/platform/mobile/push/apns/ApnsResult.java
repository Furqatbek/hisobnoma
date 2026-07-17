package com.hisobnoma.platform.mobile.push.apns;

/**
 * Outcome of one APNs delivery attempt.
 *
 * @param status    APNs HTTP status (0 = not sent / disabled)
 * @param reason    APNs {@code reason} field on failure, or a local marker
 * @param tokenDead true when APNs says the token is permanently invalid (410, BadDeviceToken,
 *                  Unregistered) and the caller should delete it
 */
public record ApnsResult(int status, String reason, boolean tokenDead) {

    public boolean isSuccess() {
        return status == 200;
    }

    public static ApnsResult ok() {
        return new ApnsResult(200, null, false);
    }

    public static ApnsResult dead(int status, String reason) {
        return new ApnsResult(status, reason, true);
    }

    public static ApnsResult failed(int status, String reason) {
        return new ApnsResult(status, reason, false);
    }

    public static ApnsResult skipped() {
        return new ApnsResult(0, "apns-not-configured", false);
    }
}
