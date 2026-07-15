package com.hera.playerwarps.storage;

import dev.dejvokep.boostedyaml.YamlDocument;

import java.util.Objects;

public final class DatabaseSettings {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private final int maximumPoolSize;
    private final int minimumIdle;
    private final long connectionTimeoutMs;
    private final long maxLifetimeMs;

    private DatabaseSettings(String host, int port, String database, String username, String password, int maximumPoolSize, int minimumIdle,
                             long connectionTimeoutMs, long maxLifetimeMs) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
        this.maximumPoolSize = maximumPoolSize;
        this.minimumIdle = minimumIdle;
        this.connectionTimeoutMs = connectionTimeoutMs;
        this.maxLifetimeMs = maxLifetimeMs;
    }

    public static DatabaseSettings from(YamlDocument storage) {
        String host = required(storage.getString("mysql.host", "localhost"), "mysql.host");
        int port = storage.getInt("mysql.port", 3306);
        String database = required(storage.getString("mysql.database", "hera_playerwarps"), "mysql.database");
        String username = required(storage.getString("mysql.username", "root"), "mysql.username");
        String password = storage.getString("mysql.password", "");
        int maximumPoolSize = storage.getInt("mysql.pool.maximum-pool-size", 10);
        int minimumIdle = storage.getInt("mysql.pool.minimum-idle", 2);
        long connectionTimeoutMs = storage.getLong("mysql.pool.connection-timeout-ms", 5000L);
        long maxLifetimeMs = storage.getLong("mysql.pool.max-lifetime-ms", 1800000L);

        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("mysql.port must be between 1 and 65535");
        }
        if (maximumPoolSize <= 0) {
            throw new IllegalArgumentException("mysql.pool.maximum-pool-size must be positive");
        }
        if (minimumIdle < 0 || minimumIdle > maximumPoolSize) {
            throw new IllegalArgumentException("mysql.pool.minimum-idle must be between 0 and maximum-pool-size");
        }
        if (connectionTimeoutMs <= 0) {
            throw new IllegalArgumentException("mysql.pool.connection-timeout-ms must be positive");
        }
        if (maxLifetimeMs <= 0) {
            throw new IllegalArgumentException("mysql.pool.max-lifetime-ms must be positive");
        }

        return new DatabaseSettings(host, port, database, username, password, maximumPoolSize, minimumIdle, connectionTimeoutMs, maxLifetimeMs);
    }

    public String jdbcUrl() {
        return "jdbc:mysql://" + this.host + ":" + this.port + "/" + this.database + "?useSSL=false&useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    }

    public String host() {
        return this.host;
    }

    public int port() {
        return this.port;
    }

    public String database() {
        return this.database;
    }

    public String username() {
        return this.username;
    }

    public String password() {
        return this.password;
    }

    public int maximumPoolSize() {
        return this.maximumPoolSize;
    }

    public int minimumIdle() {
        return this.minimumIdle;
    }

    public long connectionTimeoutMs() {
        return this.connectionTimeoutMs;
    }

    public long maxLifetimeMs() {
        return this.maxLifetimeMs;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof DatabaseSettings)) {
            return false;
        }
        DatabaseSettings that = (DatabaseSettings) object;
        return this.port == that.port
                && this.maximumPoolSize == that.maximumPoolSize
                && this.minimumIdle == that.minimumIdle
                && this.connectionTimeoutMs == that.connectionTimeoutMs
                && this.maxLifetimeMs == that.maxLifetimeMs
                && Objects.equals(this.host, that.host)
                && Objects.equals(this.database, that.database)
                && Objects.equals(this.username, that.username)
                && Objects.equals(this.password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.host, this.port, this.database, this.username, this.password, this.maximumPoolSize, this.minimumIdle,
                this.connectionTimeoutMs, this.maxLifetimeMs);
    }

    private static String required(String value, String path) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(path + " cannot be empty");
        }
        return trimmed;
    }
}
