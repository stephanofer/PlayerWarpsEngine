package com.hera.playerwarps.warp;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface WhitelistRepository {

    Map<Long, List<WhitelistEntry>> findAllByServer(String serverId) throws SQLException;
}
