package com.hisobnoma.platform.mobile.push.apns;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.mobile.push.entity.PushEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Real APNs delivery over HTTP/2 (required by Apple) using the JDK HttpClient. Routes each send to
 * the sandbox or production host per the token's {@link PushEnvironment}. When APNs reports the
 * token is permanently invalid, returns a {@code tokenDead} result so the caller can prune it.
 */
@Slf4j
@Component
public class ApnsHttpClient implements ApnsClient {

    private static final String PRODUCTION_HOST = "api.push.apple.com";
    private static final String SANDBOX_HOST = "api.sandbox.push.apple.com";
    /**
     * APNs reasons meaning THIS token is gone for good (safe to delete). Deliberately excludes
     * {@code DeviceTokenNotForTopic}: that means the apns-topic/bundle id doesn't match the token —
     * a provider-side config error that would otherwise wipe every token in a tenant on the first
     * misconfigured broadcast, not a per-token death.
     */
    private static final Set<String> DEAD_REASONS = Set.of("BadDeviceToken", "Unregistered");

    private final ApnsProperties properties;
    private final ApnsJwtProvider jwtProvider;
    private final ObjectMapper objectMapper;
    private volatile HttpClient httpClient;

    public ApnsHttpClient(ApnsProperties properties, ApnsJwtProvider jwtProvider, ObjectMapper objectMapper) {
        this.properties = properties;
        this.jwtProvider = jwtProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public ApnsResult send(String deviceToken, PushEnvironment environment, ApnsPayload payload) {
        if (!isConfigured()) {
            return ApnsResult.skipped();
        }
        String jwt = jwtProvider.currentToken();
        if (jwt == null) {
            return ApnsResult.failed(0, "apns-key-load-failed");
        }
        String host = environment == PushEnvironment.SANDBOX ? SANDBOX_HOST : PRODUCTION_HOST;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://" + host + "/3/device/" + deviceToken))
                    .timeout(Duration.ofSeconds(10))
                    .header("authorization", "bearer " + jwt)
                    .header("apns-topic", properties.getBundleId())
                    .header("apns-push-type", "alert")
                    .header("apns-priority", "10")
                    .POST(HttpRequest.BodyPublishers.ofString(body(payload)))
                    .build();

            HttpResponse<String> response = client().send(request, HttpResponse.BodyHandlers.ofString());
            ApnsResult result = classify(response.statusCode(), reasonOf(response.body()));
            if (!result.isSuccess() && !result.tokenDead()) {
                log.warn("APNs send failed: status={} reason={}", result.status(), result.reason());
            }
            return result;
        } catch (Exception e) {
            log.warn("APNs send error to {}: {}", host, e.getMessage());
            return ApnsResult.failed(0, e.getClass().getSimpleName());
        }
    }

    /**
     * Maps an APNs HTTP status + reason to a result. A token is pruned only on 410 or a per-token
     * dead reason ({@link #DEAD_REASONS}); everything else is a retryable failure. Null-safe on
     * reason (a blank/unparseable error body yields a null reason).
     */
    ApnsResult classify(int status, String reason) {
        if (status == 200) {
            return ApnsResult.ok();
        }
        if (status == 410 || (reason != null && DEAD_REASONS.contains(reason))) {
            return ApnsResult.dead(status, reason);
        }
        return ApnsResult.failed(status, reason);
    }

    String body(ApnsPayload p) throws Exception {
        Map<String, Object> alert = new LinkedHashMap<>();
        alert.put("title", p.title());
        alert.put("body", p.body());
        Map<String, Object> aps = new LinkedHashMap<>();
        aps.put("alert", alert);
        aps.put("sound", "default");
        if (p.badge() != null) {
            aps.put("badge", p.badge());
        }
        Map<String, Object> root = new HashMap<>();
        root.put("aps", aps);
        if (p.type() != null) {
            root.put("type", p.type());
        }
        if (p.id() != null) {
            root.put("id", p.id());
        }
        if (p.route() != null) {
            root.put("route", p.route());
        }
        return objectMapper.writeValueAsString(root);
    }

    private String reasonOf(String responseBody) {
        try {
            return responseBody == null || responseBody.isBlank()
                    ? null : objectMapper.readTree(responseBody).path("reason").asText(null);
        } catch (Exception e) {
            return null;
        }
    }

    private HttpClient client() {
        HttpClient c = httpClient;
        if (c == null) {
            synchronized (this) {
                if (httpClient == null) {
                    httpClient = HttpClient.newBuilder()
                            .version(HttpClient.Version.HTTP_2)
                            .connectTimeout(Duration.ofSeconds(10))
                            .build();
                }
                c = httpClient;
            }
        }
        return c;
    }
}
