package com.hera.playerwarps.storage;

import com.hera.playerwarps.warp.LimitBonusRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MysqlLimitBonusRepository implements LimitBonusRepository {

    private final Database database;

    public MysqlLimitBonusRepository(Database database) {
        this.database = database;
    }

    @Override
    public Map<UUID, Integer> findAllByServer(String serverId) throws SQLException {
        String sql = "SELECT player_uuid, extra_limit FROM player_warp_bonuses WHERE server_id = ?";
        Map<UUID, Integer> bonuses = new LinkedHashMap<UUID, Integer>();

        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    bonuses.put(UUID.fromString(resultSet.getString("player_uuid")), Math.max(0, resultSet.getInt("extra_limit")));
                }
            }
        }

        return bonuses;
    }

    @Override
    public int findBonus(String serverId, UUID playerUuid) throws SQLException {
        String sql = "SELECT extra_limit FROM player_warp_bonuses WHERE server_id = ? AND player_uuid = ?";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serverId);
            statement.setString(2, playerUuid.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Math.max(0, resultSet.getInt("extra_limit"));
                }
            }
        }
        return 0;
    }

    @Override
    public void setBonus(String serverId, UUID playerUuid, int amount) throws SQLException {
        String sql = "INSERT INTO player_warp_bonuses (server_id, player_uuid, extra_limit, updated_at) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE extra_limit = VALUES(extra_limit), updated_at = VALUES(updated_at)";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serverId);
            statement.setString(2, playerUuid.toString());
            statement.setInt(3, Math.max(0, amount));
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        }
    }
}
