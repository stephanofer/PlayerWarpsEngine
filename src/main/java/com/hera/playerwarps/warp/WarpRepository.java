package com.hera.playerwarps.warp;

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface WarpRepository {

    List<Warp> findAllByServer(String serverId) throws SQLException;

    Optional<Warp> findById(long id) throws SQLException;

    Warp insert(WarpCreateRequest request) throws SQLException;

    boolean deleteById(String serverId, long id) throws SQLException;

    int deleteByOwner(String serverId, UUID ownerUuid) throws SQLException;

    int deleteByIds(String serverId, Collection<Long> ids) throws SQLException;

    boolean updateDescription(String serverId, long id, String description, long updatedAt) throws SQLException;

    boolean updateLocked(String serverId, long id, boolean locked, long updatedAt) throws SQLException;

    boolean updateWhitelistEnabled(String serverId, long id, boolean enabled, long updatedAt) throws SQLException;

    boolean updateLocation(String serverId, long id, WarpLocation location, SafeStatus safeStatus, long updatedAt) throws SQLException;

    boolean updateName(String serverId, long id, WarpName name, long updatedAt) throws SQLException;

    boolean updateOwner(String serverId, long id, UUID ownerUuid, String ownerName, long updatedAt) throws SQLException;

    void incrementVisits(String serverId, Map<Long, Long> visitDeltas, long visitedAt) throws SQLException;
}
