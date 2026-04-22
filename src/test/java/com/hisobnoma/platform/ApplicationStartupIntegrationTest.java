package com.hisobnoma.platform;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ApplicationStartupIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private DataSource dataSource;

    @Test
    void applicationContext_loadsWithoutErrors() {
        // Then — just reaching this assertion means the full context loaded
        assertNotNull(applicationContext);
        // Verify a few key beans are present
        assertNotNull(applicationContext.getBean("auditLogService"));
        assertNotNull(applicationContext.getBean("authService"));
        assertNotNull(applicationContext.getBean("userService"));
    }

    @Test
    void dataSource_connectsToTestDatabase() throws SQLException {
        // When
        try (Connection connection = dataSource.getConnection()) {
            // Then
            assertNotNull(connection);
            assertTrue(connection.isValid(2));

            DatabaseMetaData metadata = connection.getMetaData();
            assertNotNull(metadata);
            // In test profile we use H2 (not Testcontainers Postgres as the plan
            // suggests); verify the URL reflects that
            String url = metadata.getURL();
            assertNotNull(url);
            assertTrue(url.contains("h2") || url.contains("postgresql"),
                    "Expected H2 or PostgreSQL URL, got: " + url);
        }
    }

    @Test
    void schemaInitialization_tablesExistForCoreEntities() throws SQLException {
        // Flyway is disabled in test profile; Hibernate DDL create-drop handles schema.
        // Verify the expected core tables exist after context startup.
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metadata = connection.getMetaData();

            // Check for a few core tables that should exist after Hibernate DDL
            assertTrue(tableExists(metadata, "users"), "users table should exist");
            assertTrue(tableExists(metadata, "roles"), "roles table should exist");
            assertTrue(tableExists(metadata, "permissions"), "permissions table should exist");
            assertTrue(tableExists(metadata, "audit_logs"), "audit_logs table should exist");
        }
    }

    private boolean tableExists(DatabaseMetaData metadata, String tableName) throws SQLException {
        // H2 stores table names in uppercase by default; check both cases
        try (var rs = metadata.getTables(null, null, tableName.toUpperCase(), null)) {
            if (rs.next()) return true;
        }
        try (var rs = metadata.getTables(null, null, tableName, null)) {
            return rs.next();
        }
    }
}
