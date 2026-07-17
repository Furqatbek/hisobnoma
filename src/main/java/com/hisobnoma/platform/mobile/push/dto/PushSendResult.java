package com.hisobnoma.platform.mobile.push.dto;

/**
 * Summary of a broadcast: how many tokens were targeted, delivered, failed, and pruned as dead.
 * {@code apnsConfigured=false} means APNs credentials are not set — nothing was actually sent.
 */
public record PushSendResult(
        int recipients,
        int sent,
        int failed,
        int pruned,
        boolean apnsConfigured) {
}
