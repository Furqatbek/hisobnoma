package com.hisobnoma.platform.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.StandardEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DotenvEnvironmentPostProcessorTest {

    private Map<String, Object> parse(String... lines) {
        return DotenvEnvironmentPostProcessor.parse(List.of(lines));
    }

    @Test
    void parse_readsPlainKeyValuePairs() {
        Map<String, Object> env = parse("APNS_ENABLED=true", "APNS_TEAM_ID=ABC123");
        assertEquals("true", env.get("APNS_ENABLED"));
        assertEquals("ABC123", env.get("APNS_TEAM_ID"));
    }

    @Test
    void parse_ignoresBlankLinesAndComments() {
        Map<String, Object> env = parse("", "   ", "# a comment", "DB_HOST=localhost");
        assertEquals(1, env.size());
        assertEquals("localhost", env.get("DB_HOST"));
    }

    @Test
    void parse_toleratesExportPrefixAndSurroundingWhitespace() {
        Map<String, Object> env = parse("export JWT_SECRET = shhh ");
        assertEquals("shhh", env.get("JWT_SECRET"));
    }

    @Test
    void parse_stripsMatchingQuotes() {
        Map<String, Object> env = parse("A='single'", "B=\"double\"", "C=\"un'matched");
        assertEquals("single", env.get("A"));
        assertEquals("double", env.get("B"));
        assertEquals("\"un'matched", env.get("C"), "no matching close quote — left as-is");
    }

    @Test
    void parse_unescapesNewlinesInDoubleQuotedValues() {
        // A one-line PKCS#8 key: \n inside double quotes becomes a real newline.
        Map<String, Object> env = parse(
                "APNS_PRIVATE_KEY=\"-----BEGIN PRIVATE KEY-----\\nMIGT\\n-----END PRIVATE KEY-----\"");
        String key = (String) env.get("APNS_PRIVATE_KEY");
        assertTrue(key.startsWith("-----BEGIN PRIVATE KEY-----\n"));
        assertTrue(key.endsWith("\n-----END PRIVATE KEY-----"));
        assertEquals(3, key.lines().count());
    }

    @Test
    void parse_keepsEqualsSignsInsideTheValue() {
        Map<String, Object> env = parse("URL=https://x.test/cb?a=1&b=2");
        assertEquals("https://x.test/cb?a=1&b=2", env.get("URL"));
    }

    @Test
    void parse_skipsMalformedLinesWithoutKey() {
        Map<String, Object> env = parse("=novalue", "NOEQUALS", "OK=1");
        assertEquals(1, env.size());
        assertEquals("1", env.get("OK"));
    }

    // ---- end-to-end against a Spring Environment ----

    @Test
    void postProcess_loadsFileValuesButLetsRealEnvWin() throws Exception {
        Path envFile = Files.createTempFile("dotenv-test", ".env");
        Files.writeString(envFile, "APNS_TEAM_ID=FROM_FILE\nDB_PASSWORD=file-secret\n");
        String overridden = "APNS_TEAM_ID"; // also set as a JVM system property below
        System.setProperty("DOTENV_PATH", envFile.toString());
        System.setProperty(overridden, "FROM_SYSTEM_PROP");
        try {
            StandardEnvironment env = new StandardEnvironment();
            new DotenvEnvironmentPostProcessor().postProcessEnvironment(env, null);

            assertTrue(env.getPropertySources().contains("dotenv"));
            // Value present only in the file resolves.
            assertEquals("file-secret", env.getProperty("DB_PASSWORD"));
            // A real system property outranks the file (dotenv precedence is below OS env/props).
            assertEquals("FROM_SYSTEM_PROP", env.getProperty(overridden));
        } finally {
            System.clearProperty("DOTENV_PATH");
            System.clearProperty(overridden);
            Files.deleteIfExists(envFile);
        }
    }

    @Test
    void postProcess_missingFileIsANoOp() {
        System.setProperty("DOTENV_PATH", "/no/such/.env-should-not-exist");
        try {
            StandardEnvironment env = new StandardEnvironment();
            new DotenvEnvironmentPostProcessor().postProcessEnvironment(env, null);
            assertFalse(env.getPropertySources().contains("dotenv"));
        } finally {
            System.clearProperty("DOTENV_PATH");
        }
    }
}
