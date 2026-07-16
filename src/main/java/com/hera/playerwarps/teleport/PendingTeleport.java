package com.hera.playerwarps.teleport;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

public final class PendingTeleport {

    private final UUID playerUuid;
    private final long warpId;
    private final String originWorld;
    private final int originBlockX;
    private final int originBlockY;
    private final int originBlockZ;
    private final long createdAtMillis;
    private final BukkitTask task;

    public PendingTeleport(UUID playerUuid, long warpId, String originWorld, int originBlockX, int originBlockY, int originBlockZ,
                           long createdAtMillis, BukkitTask task) {
        this.playerUuid = playerUuid;
        this.warpId = warpId;
        this.originWorld = originWorld;
        this.originBlockX = originBlockX;
        this.originBlockY = originBlockY;
        this.originBlockZ = originBlockZ;
        this.createdAtMillis = createdAtMillis;
        this.task = task;
    }

    public UUID playerUuid() {
        return this.playerUuid;
    }

    public long warpId() {
        return this.warpId;
    }

    public String originWorld() {
        return this.originWorld;
    }

    public int originBlockX() {
        return this.originBlockX;
    }

    public int originBlockY() {
        return this.originBlockY;
    }

    public int originBlockZ() {
        return this.originBlockZ;
    }

    public long createdAtMillis() {
        return this.createdAtMillis;
    }

    public BukkitTask task() {
        return this.task;
    }
}
