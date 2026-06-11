package com.hisobnoma.platform;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs every Flyway migration against a real PostgreSQL instance.
 *
 * The regular suite uses H2 with Hibernate ddl-auto and Flyway disabled,
 * so without this test the migration scripts are never executed anywhere
 * before a production deploy. Skipped automatically when Docker is not
 * available (local sandboxes); runs in CI.
 */
@Testcontainers(disabledWithoutDocker = true)
class FlywayMigrationPostgresTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("hisobnoma_migration_test")
                    .withUsername("test")
                    .withPassword("test");

    @Test
    void allMigrations_applyCleanly_onPostgres() {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load();

        var result = flyway.migrate();

        assertTrue(result.success, "Flyway migration failed");
        assertTrue(result.migrationsExecuted >= 60,
                "Expected the full migration chain to run, executed: " + result.migrationsExecuted);

        for (MigrationInfo info : flyway.info().applied()) {
            assertEquals(MigrationState.SUCCESS, info.getState(),
                    "Migration " + info.getVersion() + " (" + info.getDescription() + ") is not SUCCESS");
        }
    }

    @Test
    void migratedSchema_containsCoreTables() throws Exception {
        // Depends on the migrate above only logically; Flyway skips
        // already-applied migrations, so re-running is cheap and keeps
        // the tests order-independent.
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement stmt = conn.createStatement()) {

            for (String table : new String[]{
                    "tenants", "users", "roles", "products", "stock",
                    "pos_transactions", "ar_invoices", "journal_entries",
                    "web_orders", "web_customers", "web_catalog_items",
                    "web_loyalty_transactions", "web_wishlist_items", "tenant_settings"}) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT to_regclass('public." + table + "')")) {
                    assertTrue(rs.next());
                    assertNotNull(rs.getString(1), "Expected table missing after migration: " + table);
                }
            }
        }
    }
}
