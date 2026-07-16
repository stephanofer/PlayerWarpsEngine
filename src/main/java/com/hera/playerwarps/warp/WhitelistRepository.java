package com.hera.playerwarps.warp;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface WhitelistRepository {

    Map<Long, List<WhitelistEntry>> findAllByServer(String serverId) throws SQLException;

    List<WhitelistEntry> findByWarpId(long warpId) throws SQLException;

    boolean add(long warpId, UUID playerUuid, String playerName, long createdAt) throws SQLException;

    boolean remove(long warpId, UUID playerUuid) throws SQLException;

    int deleteByWarpId(long warpId) throws SQLException;
}
