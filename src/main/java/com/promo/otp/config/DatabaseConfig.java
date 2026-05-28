package com.promo.otp.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConfig {
    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;
    private static final Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

    static {
        try {
            Class.forName("org.postgresql.Driver");
            initializeDataSource();
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            log.error("PostgreSQL JDBC Driver not found", e);
            throw new RuntimeException("Failed to load PostgreSQL driver", e);
        }
    }

    private static void initializeDataSource() {
        HikariConfig config = new HikariConfig();
        String dbHost = dotenv.get("DB_HOST", "localhost");
        String dbPort = dotenv.get("DB_PORT", "5432");
        String dbName = dotenv.get("DB_NAME", "promo_otp");
        String dbUser = dotenv.get("DB_USER", "postgres");
        String dbPassword = dotenv.get("DB_PASSWORD", "postgres");

        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);
        log.info("Database connection pool initialized");
    }

    private static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Execute schema.sql
            String schemaSql = readSchemaFile();
            for (String statement : schemaSql.split(";")) {
                if (!statement.trim().isEmpty()) {
                    stmt.execute(statement.trim());
                }
            }
            log.info("Database schema initialized successfully");
        } catch (SQLException e) {
            log.error("Failed to initialize database schema", e);
        }
    }

    private static String readSchemaFile() {
        try (var inputStream = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream("db/migration/schema.sql")) {
            if (inputStream == null) {
                log.error("schema.sql not found");
                return "";
            }
            return new String(inputStream.readAllBytes());
        } catch (Exception e) {
            log.error("Failed to read schema.sql", e);
            return "";
        }
    }

    public static Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("DataSource not initialized");
        }
        return dataSource.getConnection();
    }

    public static void closePool() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Database connection pool closed");
        }
    }
}