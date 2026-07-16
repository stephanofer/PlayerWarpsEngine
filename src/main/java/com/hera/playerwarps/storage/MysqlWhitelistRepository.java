package com.hera.playerwarps.storage;

import com.hera.playerwarps.warp.WhitelistEntry;
import com.hera.playerwarps.warp.WhitelistRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MysqlWhitelistRepository implements WhitelistRepository {

    private final Database database;

    public MysqlWhitelistRepository(Database database) {
        this.database = database;
    }

    @Override
    public Map<Long, List<WhitelistEntry>> findAllByServer(String serverId) throws SQLException {
        String sql = "SELECT ww.warp_id, ww.player_uuid, ww.player_name, ww.created_at "
                + "FROM player_warp_whitelist ww "
                + "INNER JOIN player_warps w ON w.id = ww.warp_id "
                + "WHERE w.server_id = ?";
        Map<Long, List<WhitelistEntry>> entries = new LinkedHashMap<Long, List<WhitelistEntry>>();

        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, serverId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    long warpId = resultSet.getLong("warp_id");
                    List<WhitelistEntry> warpEntries = entries.get(warpId);
                    if (warpEntries == null) {
                        warpEntries = new ArrayList<WhitelistEntry>();
                        entries.put(warpId, warpEntries);
                    }
                    warpEntries.add(new WhitelistEntry(
                            warpId,
                            UUID.fromString(resultSet.getString("player_uuid")),
                            resultSet.getString("player_name"),
                            resultSet.getLong("created_at")
                    ));
                }
            }
        }

        return entries;
    }

    @Override
    public List<WhitelistEntry> findByWarpId(long warpId) throws SQLException {
        String sql = "SELECT warp_id, player_uuid, player_name, created_at FROM player_warp_whitelist WHERE warp_id = ?";
        List<WhitelistEntry> entries = new ArrayList<WhitelistEntry>();

        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warpId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(new WhitelistEntry(
                            resultSet.getLong("warp_id"),
                            UUID.fromString(resultSet.getString("player_uuid")),
                            resultSet.getString("player_name"),
                            resultSet.getLong("created_at")
                    ));
                }
            }
        }

        return entries;
    }

    @Override
    public boolean add(long warpId, UUID playerUuid, String playerName, long createdAt) throws SQLException {
        String sql = "INSERT IGNORE INTO player_warp_whitelist (warp_id, player_uuid, player_name, created_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warpId);
            statement.setString(2, playerUuid.toString());
            statement.setString(3, playerName);
            statement.setLong(4, createdAt);
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public boolean remove(long warpId, UUID playerUuid) throws SQLException {
        String sql = "DELETE FROM player_warp_whitelist WHERE warp_id = ? AND player_uuid = ?";
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, warpId);
            statement.setString(2, playerUuid.toString());
            return statement.executeUpdate() > 0;
        }
    }

    @Override
    public int deleteByWarpId(long warpId) throws SQLException {
        try (Connection connection = this.database.getConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM player_warp_whitelist WHERE warp_id = ?")) {
            statement.setLong(1, warpId);
            return statement.executeUpdate();
        }
    }
}
