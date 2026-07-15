package com.hera.playerwarps.warp;

import java.util.UUID;

public final class WarpCreateRequest {

    private final String serverId;
    private final WarpName name;
    private final UUID ownerUuid;
    private final String ownerName;
    private final WarpLocation location;
    private final boolean locked;
    private final boolean whitelistEnabled;
    private final long now;

    public WarpCreateRequest(String serverId, WarpName name, UUID ownerUuid, String ownerName, WarpLocation location, boolean locked,
                             boolean whitelistEnabled, long now) {
        this.serverId = serverId;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.location = location;
        this.locked = locked;
        this.whitelistEnabled = whitelistEnabled;
        this.now = now;
    }

    public String serverId() {
        return this.serverId;
    }

    public WarpName name() {
        return this.name;
    }

    public UUID ownerUuid() {
        return this.ownerUuid;
    }

    public String ownerName() {
        return this.ownerName;
    }

    public WarpLocation location() {
        return this.location;
    }

    public boolean locked() {
        return this.locked;
    }

    public boolean whitelistEnabled() {
        return this.whitelistEnabled;
    }

    public long now() {
        return this.now;
    }
}
