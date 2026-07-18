package com.hisobnoma.platform.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads a local {@code .env} file (KEY=VALUE) into the Spring Environment so the credential
 * placeholders in {@code application*.yml} — {@code ${APNS_PRIVATE_KEY}}, {@code ${DB_PASSWORD}},
 * {@code ${JWT_SECRET}}, {@code ${PAYME_KEY}}, {@code ${TELEGRAM_BOT_TOKEN}}, … — resolve from it
 * during local/dev runs without exporting each one by hand.
 *
 * <p>Real OS environment variables and JVM system properties always take precedence over the file,
 * so production (which injects real env vars) behaves exactly as before. The file is optional — if
 * it is absent this is a no-op — and it is gitignored, so real secrets never get committed. Set
 * {@code DOTENV_PATH} to load the file from a non-default location.
 */
public class DotenvEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String PROPERTY_SOURCE_NAME = "dotenv";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String configured = environment.getProperty("DOTENV_PATH");
        Path envPath = Path.of(configured != null && !configured.isBlank() ? configured : ".env");
        if (!Files.isRegularFile(envPath)) {
            return; // optional — nothing to load
        }

        Map<String, Object> values;
        try {
            values = parse(Files.readAllLines(envPath, StandardCharsets.UTF_8));
        } catch (IOException e) {
            // Pre-logging phase — fall back to stderr and continue; env vars may still be set.
            System.err.println("[dotenv] failed to read " + envPath + ": " + e.getMessage());
            return;
        }
        if (values.isEmpty()) {
            return;
        }

        // SystemEnvironmentPropertySource gives the same relaxed binding as OS env vars
        // (UPPER_SNAKE <-> dotted). Placed just after the real OS env source so a real env var
        // always overrides the file.
        MutablePropertySources sources = environment.getPropertySources();
        SystemEnvironmentPropertySource source =
                new SystemEnvironmentPropertySource(PROPERTY_SOURCE_NAME, values);
        String systemEnv = StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME;
        if (sources.contains(systemEnv)) {
            sources.addAfter(systemEnv, source);
        } else {
            sources.addLast(source);
        }
    }

    /**
     * Parses {@code .env} lines. Ignores blank lines and {@code #} comments, tolerates a leading
     * {@code export }, and strips a matching pair of surrounding quotes. Inside double quotes,
     * {@code \n} and {@code \"} are unescaped so a one-line PKCS#8 key (e.g. {@code APNS_PRIVATE_KEY})
     * can carry real newlines.
     */
    static Map<String, Object> parse(List<String> lines) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (String raw : lines) {
            String line = raw.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring("export ".length()).strip();
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue; // no key, or malformed
            }
            String key = line.substring(0, eq).strip();
            String value = unquote(line.substring(eq + 1).strip());
            if (!key.isEmpty()) {
                map.put(key, value);
            }
        }
        return map;
    }

    private static String unquote(String value) {
        if (value.length() >= 2) {
            char q = value.charAt(0);
            if ((q == '"' || q == '\'') && value.charAt(value.length() - 1) == q) {
                String inner = value.substring(1, value.length() - 1);
                return q == '"' ? inner.replace("\\n", "\n").replace("\\\"", "\"") : inner;
            }
        }
        return value;
    }
}
