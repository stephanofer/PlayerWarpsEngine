package com.hera.playerwarps.storage;

import com.hera.playerwarps.warp.SafeStatus;
import com.hera.playerwarps.warp.Warp;
import com.hera.playerwarps.warp.WarpCreateRequest;
import com.hera.playerwarps.warp.WarpLocation;
import com.hera.playerwarps.warp.WarpRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class MysqlWarpRepository implements WarpRepository {

    private final Database database;

    public MysqlWarpRepository(Database database) {
        this.database = database;
    }

    @Override
    public List<Warp> findAllByServer(String serverId) throws SQLException {
        String sql = "SELECT * FROM player_warps WHERE server_id = ?";
        List<Warp> warps = new ArrayList<Warp>();

        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    warps.add(mapWarp(resultSet));
                }
            }
        }

        return warps;
    }

    @Override
    public Optional<Warp> findById(long id) throws SQLException {
        String sql = "SELECT * FROM player_warps WHERE id = ?";

        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapWarp(resultSet));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public Warp insert(WarpCreateRequest request) throws SQLException {
        String sql = "INSERT INTO player_warps (server_id, name, name_normalized, owner_uuid, owner_name, world, x, y, z, yaw, pitch, "
                + "locked, whitelist_enabled, visits, safe_status, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 'UNKNOWN', ?, ?)";

        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, request.serverId());
            statement.setString(2, request.name().value());
            statement.setString(3, request.name().normalized());
            statement.setString(4, request.ownerUuid().toString());
            statement.setString(5, request.ownerName());
            statement.setString(6, request.location().world());
            statement.setDouble(7, request.location().x());
            statement.setDouble(8, request.location().y());
            statement.setDouble(9, request.location().z());
            statement.setFloat(10, request.location().yaw());
            statement.setFloat(11, request.location().pitch());
            statement.setBoolean(12, request.locked());
            statement.setBoolean(13, request.whitelistEnabled());
            statement.setLong(14, request.now());
            statement.setLong(15, request.now());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Creating warp did not return a generated id");
                }

                return new Warp(keys.getLong(1), request.serverId(), request.name().value(), request.name().normalized(), request.ownerUuid(),
                        request.ownerName(), request.location(), null, null, (short) 0, request.locked(), request.whitelistEnabled(), 0L,
                        SafeStatus.UNKNOWN, request.now(), request.now(), null);
            }
        }
    }

    @Override
    public boolean deleteById(String serverId, long id) throws SQLException {
        try (Connection connection = this.database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteWarp = connection.prepareStatement("DELETE FROM player_warps WHERE server_id = ? AND id = ?");
                 PreparedStatement deleteWhitelist = connection.prepareStatement("DELETE FROM player_warp_whitelist WHERE warp_id = ?")) {
                deleteWarp.setString(1, serverId);
                deleteWarp.setLong(2, id);
                boolean removed = deleteWarp.executeUpdate() > 0;
                if (removed) {
                    deleteWhitelist.setLong(1, id);
                    deleteWhitelist.executeUpdate();
                }
                connection.commit();
                return removed;
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    private static Warp mapWarp(ResultSet resultSet) throws SQLException {
        Long lastVisitedAt = resultSet.getObject("last_visited_at") == null ? null : resultSet.getLong("last_visited_at");
        return new Warp(
                resultSet.getLong("id"),
                resultSet.getString("server_id"),
                resultSet.getString("name"),
                resultSet.getString("name_normalized"),
                UUID.fromString(resultSet.getString("owner_uuid")),
                resultSet.getString("owner_name"),
                new WarpLocation(
                        resultSet.getString("world"),
                        resultSet.getDouble("x"),
                        resultSet.getDouble("y"),
                        resultSet.getDouble("z"),
                        resultSet.getFloat("yaw"),
                        resultSet.getFloat("pitch")
                ),
                resultSet.getString("description"),
                resultSet.getString("icon_material"),
                resultSet.getShort("icon_data"),
                resultSet.getBoolean("locked"),
                resultSet.getBoolean("whitelist_enabled"),
                resultSet.getLong("visits"),
                SafeStatus.fromDatabase(resultSet.getString("safe_status")),
                resultSet.getLong("created_at"),
                resultSet.getLong("updated_at"),
                lastVisitedAt
        );
    }
}
