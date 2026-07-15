package com.hera.playerwarps.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaMigrator {

    private static final int INITIAL_SCHEMA_VERSION = 1;

    private final Database database;

    public SchemaMigrator(Database database) {
        this.database = database;
    }

    public void migrate() throws SQLException {
        createMigrationTable();
        if (isMigrationApplied(INITIAL_SCHEMA_VERSION)) {
            return;
        }

        applyInitialSchema();
        markApplied(INITIAL_SCHEMA_VERSION, "initial_schema");
    }

    private void createMigrationTable() throws SQLException {
        execute("CREATE TABLE IF NOT EXISTS player_warps_schema_migrations ("
                + "version INT NOT NULL PRIMARY KEY,"
                + "description VARCHAR(128) NOT NULL,"
                + "applied_at BIGINT NOT NULL"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private boolean isMigrationApplied(int version) throws SQLException {
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM player_warps_schema_migrations WHERE version = ?")) {
            statement.setInt(1, version);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private void applyInitialSchema() throws SQLException {
        execute("CREATE TABLE IF NOT EXISTS player_warps ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "server_id VARCHAR(64) NOT NULL,"
                + "name VARCHAR(32) NOT NULL,"
                + "name_normalized VARCHAR(32) NOT NULL,"
                + "owner_uuid CHAR(36) NOT NULL,"
                + "owner_name VARCHAR(16) NOT NULL,"
                + "world VARCHAR(64) NOT NULL,"
                + "x DOUBLE NOT NULL,"
                + "y DOUBLE NOT NULL,"
                + "z DOUBLE NOT NULL,"
                + "yaw FLOAT NOT NULL,"
                + "pitch FLOAT NOT NULL,"
                + "description VARCHAR(160) NULL,"
                + "icon_material VARCHAR(64) NULL,"
                + "icon_data SMALLINT NOT NULL DEFAULT 0,"
                + "locked BOOLEAN NOT NULL DEFAULT FALSE,"
                + "whitelist_enabled BOOLEAN NOT NULL DEFAULT FALSE,"
                + "visits BIGINT NOT NULL DEFAULT 0,"
                + "safe_status VARCHAR(16) NOT NULL DEFAULT 'UNKNOWN',"
                + "created_at BIGINT NOT NULL,"
                + "updated_at BIGINT NOT NULL,"
                + "last_visited_at BIGINT NULL,"
                + "UNIQUE KEY uq_warp_name (server_id, name_normalized),"
                + "KEY idx_owner (server_id, owner_uuid),"
                + "KEY idx_visits (server_id, visits),"
                + "KEY idx_updated (server_id, updated_at)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        execute("CREATE TABLE IF NOT EXISTS player_warp_whitelist ("
                + "warp_id BIGINT NOT NULL,"
                + "player_uuid CHAR(36) NOT NULL,"
                + "player_name VARCHAR(16) NOT NULL,"
                + "created_at BIGINT NOT NULL,"
                + "PRIMARY KEY (warp_id, player_uuid)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        execute("CREATE TABLE IF NOT EXISTS player_warp_bonuses ("
                + "server_id VARCHAR(64) NOT NULL,"
                + "player_uuid CHAR(36) NOT NULL,"
                + "extra_limit INT NOT NULL,"
                + "updated_at BIGINT NOT NULL,"
                + "PRIMARY KEY (server_id, player_uuid)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void markApplied(int version, String description) throws SQLException {
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement("INSERT INTO player_warps_schema_migrations (version, description, applied_at) VALUES (?, ?, ?)")) {
            statement.setInt(1, version);
            statement.setString(2, description);
            statement.setLong(3, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }

    private void execute(String sql) throws SQLException {
        try (Connection connection = this.database.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
