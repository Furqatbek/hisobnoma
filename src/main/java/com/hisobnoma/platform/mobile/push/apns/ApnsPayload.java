package com.hisobnoma.platform.mobile.push.apns;

/**
 * The app-facing notification content + routing keys. Serialized into the APNs body as
 * {@code {"aps":{"alert":{title,body},"sound":"default","badge":n}, "type":..., "id":..., "route":...}}.
 * {@code type}/{@code id}/{@code route} are language-neutral (the app routes on them); {@code title}/
 * {@code body} are already localized by the caller.
 */
public record ApnsPayload(
        String title,
        String body,
        Integer badge,
        String type,
        Long id,
        String route) {
}
