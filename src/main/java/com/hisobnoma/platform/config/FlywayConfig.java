package com.hisobnoma.platform.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Runs {@code flyway.repair()} before {@code migrate()} on startup.
 *
 * <p>Two migrations ({@code V62}, {@code V63}) were content-edited after their first release (audit
 * columns added). A database that applied the earlier versions holds their old checksums; with
 * {@code validate-on-migrate: true} the app would refuse to boot with a "checksum mismatch". The
 * fake {@code spring.flyway.repair-on-migrate} property that used to be set is not a real Spring/
 * Flyway property and did nothing — this bean is the actual mechanism. {@code repair()} realigns the
 * stored checksums (and clears any failed-migration rows) so {@code migrate()} then validates and
 * applies cleanly.
 */
@Slf4j
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            log.info("Flyway: running repair() to realign checksums before migrate()");
            flyway.repair();
            flyway.migrate();
        };
    }
}
