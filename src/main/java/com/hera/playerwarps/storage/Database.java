package com.hera.playerwarps.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public final class Database {

    private final DatabaseSettings settings;
    private HikariDataSource dataSource;

    public Database(DatabaseSettings settings) {
        this.settings = settings;
    }

    public void connect() throws SQLException {
        HikariConfig config = new HikariConfig();
        config.setPoolName("PlayerWarpsEngine-Pool");
        config.setJdbcUrl(this.settings.jdbcUrl());
        config.setUsername(this.settings.username());
        config.setPassword(this.settings.password());
        config.setMaximumPoolSize(this.settings.maximumPoolSize());
        config.setMinimumIdle(this.settings.minimumIdle());
        config.setConnectionTimeout(this.settings.connectionTimeoutMs());
        config.setMaxLifetime(this.settings.maxLifetimeMs());
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource(config);
        testConnection();
    }

    public Connection getConnection() throws SQLException {
        if (this.dataSource == null) {
            throw new SQLException("Database pool is not initialized");
        }
        return this.dataSource.getConnection();
    }

    public void testConnection() throws SQLException {
        try (Connection connection = getConnection()) {
            if (!connection.isValid(3)) {
                throw new SQLException("MySQL connection validation failed");
            }
        }
    }

    public void close() {
        if (this.dataSource != null) {
            this.dataSource.close();
            this.dataSource = null;
        }
    }
}
