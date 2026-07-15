package com.hera.playerwarps.warp;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface WarpRepository {

    List<Warp> findAllByServer(String serverId) throws SQLException;

    Optional<Warp> findById(long id) throws SQLException;

    Warp insert(WarpCreateRequest request) throws SQLException;

    boolean deleteById(String serverId, long id) throws SQLException;
}
