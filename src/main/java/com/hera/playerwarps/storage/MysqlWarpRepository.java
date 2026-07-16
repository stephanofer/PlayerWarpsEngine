package com.hera.playerwarps.storage;

import com.hera.playerwarps.warp.SafeStatus;
import com.hera.playerwarps.warp.Warp;
import com.hera.playerwarps.warp.WarpCreateRequest;
import com.hera.playerwarps.warp.WarpLocation;
import com.hera.playerwarps.warp.WarpName;
import com.hera.playerwarps.warp.WarpRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
                        request.ownerName(), request.location(), null, (short) 0, request.locked(), request.whitelistEnabled(), 0L,
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

    @Override
    public int deleteByOwner(String serverId, UUID ownerUuid) throws SQLException {
        String selectSql = "SELECT id FROM player_warps WHERE server_id = ? AND owner_uuid = ?";
        List<Long> ids = new ArrayList<Long>();
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(selectSql)) {
            statement.setString(1, serverId);
            statement.setString(2, ownerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getLong("id"));
                }
            }
        }
        return deleteByIds(serverId, ids);
    }

    @Override
    public int deleteByIds(String serverId, Collection<Long> ids) throws SQLException {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }

        try (Connection connection = this.database.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteWhitelist = connection.prepareStatement("DELETE FROM player_warp_whitelist WHERE warp_id = ?");
                 PreparedStatement deleteWarp = connection.prepareStatement("DELETE FROM player_warps WHERE server_id = ? AND id = ?")) {
                int removed = 0;
                for (Long id : ids) {
                    if (id == null) {
                        continue;
                    }
                    deleteWhitelist.setLong(1, id.longValue());
                    deleteWhitelist.addBatch();

                    deleteWarp.setString(1, serverId);
                    deleteWarp.setLong(2, id.longValue());
                    deleteWarp.addBatch();
                }
                deleteWhitelist.executeBatch();
                int[] results = deleteWarp.executeBatch();
                for (int result : results) {
                    if (result > 0 || result == Statement.SUCCESS_NO_INFO) {
                        removed++;
                    }
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

    @Override
    public boolean updateLocked(String serverId, long id, boolean locked, long updatedAt) throws SQLException {
        String sql = "UPDATE player_warps SET locked = ?, updated_at = ? WHERE server_id = ? AND id = ?";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, locked);
            statement.setLong(2, updatedAt);
            statement.setString(3, serverId);
            statement.setLong(4, id);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateWhitelistEnabled(String serverId, long id, boolean enabled, long updatedAt) throws SQLException {
        String sql = "UPDATE player_warps SET whitelist_enabled = ?, updated_at = ? WHERE server_id = ? AND id = ?";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setBoolean(1, enabled);
            statement.setLong(2, updatedAt);
            statement.setString(3, serverId);
            statement.setLong(4, id);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateLocation(String serverId, long id, WarpLocation location, SafeStatus safeStatus, long updatedAt) throws SQLException {
        String sql = "UPDATE player_warps SET world = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?, safe_status = ?, updated_at = ? "
                + "WHERE server_id = ? AND id = ?";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, location.world());
            statement.setDouble(2, location.x());
            statement.setDouble(3, location.y());
            statement.setDouble(4, location.z());
            statement.setFloat(5, location.yaw());
            statement.setFloat(6, location.pitch());
            statement.setString(7, safeStatus.name());
            statement.setLong(8, updatedAt);
            statement.setString(9, serverId);
            statement.setLong(10, id);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateName(String serverId, long id, WarpName name, long updatedAt) throws SQLException {
        String sql = "UPDATE player_warps SET name = ?, name_normalized = ?, updated_at = ? WHERE server_id = ? AND id = ?";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name.value());
            statement.setString(2, name.normalized());
            statement.setLong(3, updatedAt);
            statement.setString(4, serverId);
            statement.setLong(5, id);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean updateOwner(String serverId, long id, UUID ownerUuid, String ownerName, long updatedAt) throws SQLException {
        String sql = "UPDATE player_warps SET owner_uuid = ?, owner_name = ?, updated_at = ? WHERE server_id = ? AND id = ?";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, ownerUuid.toString());
            statement.setString(2, ownerName);
            statement.setLong(3, updatedAt);
            statement.setString(4, serverId);
            statement.setLong(5, id);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public void incrementVisits(String serverId, Map<Long, Long> visitDeltas, long visitedAt) throws SQLException {
        if (visitDeltas.isEmpty()) {
            return;
        }

        String sql = "UPDATE player_warps SET visits = visits + ?, last_visited_at = ? WHERE server_id = ? AND id = ?";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Map.Entry<Long, Long> entry : visitDeltas.entrySet()) {
                long delta = entry.getValue() == null ? 0L : entry.getValue();
                if (delta <= 0L) {
                    continue;
                }
                statement.setLong(1, delta);
                statement.setLong(2, visitedAt);
                statement.setString(3, serverId);
                statement.setLong(4, entry.getKey());
                statement.addBatch();
            }
            statement.executeBatch();
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
