package com.hera.playerwarps.warp;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WarpWarmupService {

    private final WarpRepository warpRepository;
    private final WhitelistRepository whitelistRepository;
    private final LimitBonusRepository limitBonusRepository;
    private final WarpCache warpCache;

    public WarpWarmupService(WarpRepository warpRepository, WhitelistRepository whitelistRepository, LimitBonusRepository limitBonusRepository,
                             WarpCache warpCache) {
        this.warpRepository = warpRepository;
        this.whitelistRepository = whitelistRepository;
        this.limitBonusRepository = limitBonusRepository;
        this.warpCache = warpCache;
    }

    public WarmupResult warmup(String serverId) throws SQLException {
        List<Warp> warps = this.warpRepository.findAllByServer(serverId);
        Map<Long, List<WhitelistEntry>> whitelistEntries = this.whitelistRepository.findAllByServer(serverId);
        Map<UUID, Integer> bonuses = this.limitBonusRepository.findAllByServer(serverId);

        this.warpCache.replaceAll(warps, whitelistEntries, bonuses);
        return new WarmupResult(warps.size(), whitelistEntries.size(), bonuses.size());
    }

    public static final class WarmupResult {

        private final int warps;
        private final int whitelistWarps;
        private final int bonuses;

        private WarmupResult(int warps, int whitelistWarps, int bonuses) {
            this.warps = warps;
            this.whitelistWarps = whitelistWarps;
            this.bonuses = bonuses;
        }

        public int warps() {
            return this.warps;
        }

        public int whitelistWarps() {
            return this.whitelistWarps;
        }

        public int bonuses() {
            return this.bonuses;
        }
    }
}
