package com.hisobnoma.platform.mobile.push.apns;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hisobnoma.platform.mobile.push.entity.PushEnvironment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the APNs JSON body shape (the aps envelope Apple requires plus the app's routing keys)
 * and the disabled-by-default guard that stops any network call when credentials are absent.
 */
class ApnsHttpClientTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private ApnsHttpClient client(ApnsProperties props) {
        return new ApnsHttpClient(props, new ApnsJwtProvider(props), mapper);
    }

    @Test
    void body_buildsApsEnvelopeWithAlertSoundAndRoutingKeys() throws Exception {
        ApnsHttpClient c = client(new ApnsProperties());
        ApnsPayload payload = new ApnsPayload("Order shipped", "Your order is on the way", 3,
                "new_order", 555L, "/orders/555");

        JsonNode root = mapper.readTree(c.body(payload));

        assertEquals("Order shipped", root.path("aps").path("alert").path("title").asText());
        assertEquals("Your order is on the way", root.path("aps").path("alert").path("body").asText());
        assertEquals("default", root.path("aps").path("sound").asText());
        assertEquals(3, root.path("aps").path("badge").asInt());
        assertEquals("new_order", root.path("type").asText());
        assertEquals(555L, root.path("id").asLong());
        assertEquals("/orders/555", root.path("route").asText());
    }

    @Test
    void body_omitsBadgeAndRoutingKeysWhenNull() throws Exception {
        ApnsHttpClient c = client(new ApnsProperties());
        ApnsPayload payload = new ApnsPayload("t", "b", null, null, null, null);

        JsonNode root = mapper.readTree(c.body(payload));

        assertFalse(root.path("aps").has("badge"), "badge omitted when null");
        assertFalse(root.has("type"));
        assertFalse(root.has("id"));
        assertFalse(root.has("route"));
        // alert + sound always present
        assertTrue(root.path("aps").has("alert"));
        assertEquals("default", root.path("aps").path("sound").asText());
    }

    @Test
    void classify_success() {
        ApnsResult r = client(new ApnsProperties()).classify(200, null);
        assertTrue(r.isSuccess());
        assertFalse(r.tokenDead());
    }

    @Test
    void classify_deadReasonsAndGoneStatusPruneTheToken() {
        ApnsHttpClient c = client(new ApnsProperties());
        assertTrue(c.classify(410, "Unregistered").tokenDead());
        assertTrue(c.classify(400, "BadDeviceToken").tokenDead());
        assertTrue(c.classify(410, null).tokenDead(), "410 alone is terminal even with no reason");
    }

    @Test
    void classify_deviceTokenNotForTopicIsNotDead() {
        // A wrong bundle id is a provider config error — must NOT prune every token in the tenant.
        ApnsResult r = client(new ApnsProperties()).classify(400, "DeviceTokenNotForTopic");
        assertFalse(r.tokenDead());
        assertFalse(r.isSuccess());
    }

    @Test
    void classify_nullReasonOnTransientErrorDoesNotThrowAndIsRetryable() {
        // reasonOf() returns null for a blank/unparseable APNs error body (e.g. a 503 edge error).
        ApnsResult r = client(new ApnsProperties()).classify(503, null);
        assertFalse(r.isSuccess());
        assertFalse(r.tokenDead(), "transient failure keeps the token");
        assertEquals(503, r.status(), "real status preserved, not lost to an NPE");
    }

    @Test
    void send_whenNotConfigured_returnsSkippedWithoutNetwork() {
        ApnsProperties props = new ApnsProperties(); // enabled=false
        ApnsHttpClient c = client(props);

        ApnsResult result = c.send("any-token", PushEnvironment.PRODUCTION, new ApnsPayload("t", "b", null, null, null, null));

        assertFalse(result.isSuccess());
        assertFalse(result.tokenDead());
        assertEquals("apns-not-configured", result.reason());
    }
}
