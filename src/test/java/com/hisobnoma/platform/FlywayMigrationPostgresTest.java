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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

    /**
     * Every table backing a {@code BaseEntity} subclass must carry the columns
     * Hibernate emits on every INSERT/UPDATE — {@code id, created_at,
     * updated_at, version} (plus {@code created_by, updated_by} for
     * {@code AuditableEntity}, and {@code tenant_id} for
     * {@code TenantAwareEntity}). The migration applies cleanly without them,
     * so the gap only surfaces at runtime on Postgres — exactly the bug class
     * that hit web_loyalty_transactions / web_device_tokens / web_wishlist_items.
     */
    @Test
    void entityTables_haveInheritedAuditColumns() throws Exception {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        // BaseEntity subclasses: id, created_at, updated_at, version
        List<String> baseTables = List.of(
                "web_loyalty_transactions", "web_device_tokens", "web_notifications");
        // TenantAwareEntity subclasses: + created_by, updated_by, tenant_id
        List<String> tenantTables = List.of(
                "web_customers", "web_orders", "web_order_lines", "web_catalog_items",
                "web_campaigns", "web_wishlist_items", "web_otp_codes", "web_payments",
                "distribution_agents", "distribution_territories",
                "distribution_orders", "distribution_order_lines",
                "distribution_van_loadouts", "distribution_van_loadout_lines",
                "distribution_routes", "distribution_route_stops", "distribution_visits",
                "distribution_agent_targets");

        Set<String> violations = new TreeSet<>();
        try (Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {

            for (String table : baseTables) {
                requireColumns(conn, table,
                        List.of("id", "created_at", "updated_at", "version"), violations);
            }
            for (String table : tenantTables) {
                requireColumns(conn, table,
                        List.of("id", "created_at", "updated_at", "version",
                                "created_by", "updated_by", "tenant_id"), violations);
            }
        }

        assertTrue(violations.isEmpty(),
                "Entity-backed tables missing inherited columns Hibernate writes on every "
                        + "INSERT/UPDATE (add them to the CREATE TABLE):\n  "
                        + String.join("\n  ", violations));
    }

    private void requireColumns(Connection conn, String table,
                                List<String> columns, Set<String> violations) throws Exception {
        for (String column : columns) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM information_schema.columns "
                            + "WHERE table_schema = 'public' AND table_name = ? AND column_name = ?")) {
                ps.setString(1, table);
                ps.setString(2, column);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        violations.add(table + "." + column);
                    }
                }
            }
        }
    }
}
