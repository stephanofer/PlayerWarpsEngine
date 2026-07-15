package com.hera.playerwarps.warp;

import java.util.UUID;

public final class WhitelistEntry {

    private final long warpId;
    private final UUID playerUuid;
    private final String playerName;
    private final long createdAt;

    public WhitelistEntry(long warpId, UUID playerUuid, String playerName, long createdAt) {
        this.warpId = warpId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.createdAt = createdAt;
    }

    public long warpId() {
        return this.warpId;
    }

    public UUID playerUuid() {
        return this.playerUuid;
    }

    public String playerName() {
        return this.playerName;
    }

    public long createdAt() {
        return this.createdAt;
    }
}
