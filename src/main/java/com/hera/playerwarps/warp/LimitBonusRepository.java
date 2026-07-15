package com.hera.playerwarps.warp;

import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

public interface LimitBonusRepository {

    Map<UUID, Integer> findAllByServer(String serverId) throws SQLException;

    int findBonus(String serverId, UUID playerUuid) throws SQLException;

    void setBonus(String serverId, UUID playerUuid, int amount) throws SQLException;
}
