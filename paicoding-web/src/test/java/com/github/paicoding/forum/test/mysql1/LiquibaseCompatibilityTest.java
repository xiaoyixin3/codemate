package com.github.paicoding.forum.test.mysql1;

import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the two database paths that must remain safe for every release:
 * bootstrapping a completely empty database and upgrading the last pre-Agent schema.
 */
class LiquibaseCompatibilityTest {
    private static final String CURRENT_CHANGELOG = "liquibase/master.xml";
    private static final String LEGACY_CHANGELOG = "liquibase/legacy-before-codemate-agent.xml";
    private static final String DATABASE_PREFIX = "codemate_liquibase_it_";

    private final List<String> temporaryDatabases = new ArrayList<>();

    @Test
    void migratesEmptyDatabaseAndRemainsIdempotent() throws Exception {
        String databaseName = createTemporaryDatabase("empty");

        update(databaseName, CURRENT_CHANGELOG);
        assertCurrentSchema(databaseName);
        int appliedChangeCount = changeLogCount(databaseName);

        update(databaseName, CURRENT_CHANGELOG);
        assertEquals(appliedChangeCount, changeLogCount(databaseName),
                "A second migration must not execute additional change sets");
        assertCurrentSchema(databaseName);
    }

    @Test
    void upgradesPreAgentDatabaseAndRemainsIdempotent() throws Exception {
        String databaseName = createTemporaryDatabase("legacy");

        update(databaseName, LEGACY_CHANGELOG);
        assertTrue(tableExists(databaseName, "user_info"));
        assertFalse(tableExists(databaseName, "ai_agent_run"));
        int legacyChangeCount = changeLogCount(databaseName);

        update(databaseName, CURRENT_CHANGELOG);
        assertTrue(changeLogCount(databaseName) > legacyChangeCount);
        assertCurrentSchema(databaseName);
        int upgradedChangeCount = changeLogCount(databaseName);

        update(databaseName, CURRENT_CHANGELOG);
        assertEquals(upgradedChangeCount, changeLogCount(databaseName),
                "An upgraded database must remain stable on the next startup");
        assertCurrentSchema(databaseName);
    }

    @AfterEach
    void dropTemporaryDatabases() throws SQLException {
        for (String databaseName : temporaryDatabases) {
            if (!databaseName.matches(DATABASE_PREFIX + "[a-z]+_[0-9a-f]{32}")) {
                throw new IllegalStateException("Refusing to drop unexpected database: " + databaseName);
            }
            try (Connection connection = serverConnection(); Statement statement = connection.createStatement()) {
                statement.execute("DROP DATABASE IF EXISTS `" + databaseName + "`");
            }
        }
        temporaryDatabases.clear();
    }

    private String createTemporaryDatabase(String scenario) throws SQLException {
        String databaseName = DATABASE_PREFIX + scenario + "_" + UUID.randomUUID().toString().replace("-", "");
        if (!databaseName.matches(DATABASE_PREFIX + "[a-z]+_[0-9a-f]{32}")) {
            throw new IllegalStateException("Unsafe temporary database name: " + databaseName);
        }
        try (Connection connection = serverConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
        temporaryDatabases.add(databaseName);
        return databaseName;
    }

    private void update(String databaseName, String changeLog) throws Exception {
        try (Connection connection = databaseConnection(databaseName)) {
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(connection));
            Liquibase liquibase = new Liquibase(changeLog, new ClassLoaderResourceAccessor(), database);
            liquibase.update(new Contexts(), new LabelExpression());
        }
    }

    private void assertCurrentSchema(String databaseName) throws SQLException {
        List<String> requiredTables = Arrays.asList(
                "user_info",
                "article",
                "ai_task_plan",
                "ai_agent_run",
                "ai_agent_step",
                "ai_agent_evidence",
                "ai_bug_diagnosis",
                "ai_task_write_audit",
                "ai_knowledge_chunk",
                "ai_chat_memory",
                "ai_user_technical_memory"
        );
        for (String table : requiredTables) {
            assertTrue(tableExists(databaseName, table), "Missing migrated table: " + table);
        }
        assertTrue(columnExists(databaseName, "ai_knowledge_chunk", "heading"));
        assertTrue(columnExists(databaseName, "ai_knowledge_chunk", "embedding_dimension"));
        assertTrue(tableExists(databaseName, "databasechangelog"));
        assertTrue(tableExists(databaseName, "databasechangeloglock"));
    }

    private boolean tableExists(String databaseName, String tableName) throws SQLException {
        return informationSchemaExists(
                "SELECT 1 FROM information_schema.tables WHERE table_schema='" + databaseName
                        + "' AND lower(table_name)=lower('" + tableName + "') LIMIT 1");
    }

    private boolean columnExists(String databaseName, String tableName, String columnName) throws SQLException {
        return informationSchemaExists(
                "SELECT 1 FROM information_schema.columns WHERE table_schema='" + databaseName
                        + "' AND lower(table_name)=lower('" + tableName + "')"
                        + " AND lower(column_name)=lower('" + columnName + "') LIMIT 1");
    }

    private boolean informationSchemaExists(String sql) throws SQLException {
        try (Connection connection = serverConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            return resultSet.next();
        }
    }

    private int changeLogCount(String databaseName) throws SQLException {
        try (Connection connection = databaseConnection(databaseName);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM DATABASECHANGELOG")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private Connection serverConnection() throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/?useUnicode=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai",
                databaseUsername(), databasePassword());
    }

    private Connection databaseConnection(String databaseName) throws SQLException {
        return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/" + databaseName
                        + "?useUnicode=true&allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai",
                databaseUsername(), databasePassword());
    }

    private String databaseUsername() {
        return System.getenv().getOrDefault("DB_USERNAME", "root");
    }

    private String databasePassword() {
        return System.getenv().getOrDefault("DB_PASSWORD", "");
    }
}
