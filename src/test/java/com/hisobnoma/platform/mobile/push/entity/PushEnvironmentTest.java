package com.hisobnoma.platform.mobile.push.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PushEnvironmentTest {

    @Test
    void from_parsesSandboxCaseAndWhitespaceInsensitively() {
        assertEquals(PushEnvironment.SANDBOX, PushEnvironment.from("sandbox"));
        assertEquals(PushEnvironment.SANDBOX, PushEnvironment.from("  SANDBOX  "));
        assertEquals(PushEnvironment.SANDBOX, PushEnvironment.from("Sandbox"));
    }

    @Test
    void from_defaultsToProductionForNullBlankOrUnknown() {
        assertEquals(PushEnvironment.PRODUCTION, PushEnvironment.from(null));
        assertEquals(PushEnvironment.PRODUCTION, PushEnvironment.from("production"));
        assertEquals(PushEnvironment.PRODUCTION, PushEnvironment.from("prod"));
        assertEquals(PushEnvironment.PRODUCTION, PushEnvironment.from("anything-else"));
    }
}
