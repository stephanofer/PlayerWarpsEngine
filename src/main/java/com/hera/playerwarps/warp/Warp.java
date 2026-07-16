package com.hera.playerwarps.warp;

import java.util.UUID;

public final class Warp {

    private final long id;
    private final String serverId;
    private final String name;
    private final String nameNormalized;
    private final UUID ownerUuid;
    private final String ownerName;
    private final WarpLocation location;
    private final String description;
    private final String iconMaterial;
    private final short iconData;
    private final boolean locked;
    private final boolean whitelistEnabled;
    private final long visits;
    private final SafeStatus safeStatus;
    private final long createdAt;
    private final long updatedAt;
    private final Long lastVisitedAt;

    public Warp(long id, String serverId, String name, String nameNormalized, UUID ownerUuid, String ownerName, WarpLocation location,
                String description, String iconMaterial, short iconData, boolean locked, boolean whitelistEnabled, long visits,
                SafeStatus safeStatus, long createdAt, long updatedAt, Long lastVisitedAt) {
        this.id = id;
        this.serverId = serverId;
        this.name = name;
        this.nameNormalized = nameNormalized;
        this.ownerUuid = ownerUuid;
        this.ownerName = ownerName;
        this.location = location;
        this.description = description;
        this.iconMaterial = iconMaterial;
        this.iconData = iconData;
        this.locked = locked;
        this.whitelistEnabled = whitelistEnabled;
        this.visits = visits;
        this.safeStatus = safeStatus;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastVisitedAt = lastVisitedAt;
    }

    public long id() {
        return this.id;
    }

    public String serverId() {
        return this.serverId;
    }

    public String name() {
        return this.name;
    }

    public String nameNormalized() {
        return this.nameNormalized;
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

    public String description() {
        return this.description;
    }

    public String iconMaterial() {
        return this.iconMaterial;
    }

    public short iconData() {
        return this.iconData;
    }

    public boolean locked() {
        return this.locked;
    }

    public boolean whitelistEnabled() {
        return this.whitelistEnabled;
    }

    public long visits() {
        return this.visits;
    }

    public SafeStatus safeStatus() {
        return this.safeStatus;
    }

    public long createdAt() {
        return this.createdAt;
    }

    public long updatedAt() {
        return this.updatedAt;
    }

    public Long lastVisitedAt() {
        return this.lastVisitedAt;
    }

    public Warp withDescription(String description, long updatedAt) {
        return new Warp(this.id, this.serverId, this.name, this.nameNormalized, this.ownerUuid, this.ownerName, this.location,
                description, this.iconMaterial, this.iconData, this.locked, this.whitelistEnabled, this.visits, this.safeStatus,
                this.createdAt, updatedAt, this.lastVisitedAt);
    }

    public Warp withLocked(boolean locked, long updatedAt) {
        return new Warp(this.id, this.serverId, this.name, this.nameNormalized, this.ownerUuid, this.ownerName, this.location,
                this.description, this.iconMaterial, this.iconData, locked, this.whitelistEnabled, this.visits, this.safeStatus,
                this.createdAt, updatedAt, this.lastVisitedAt);
    }

    public Warp withWhitelistEnabled(boolean whitelistEnabled, long updatedAt) {
        return new Warp(this.id, this.serverId, this.name, this.nameNormalized, this.ownerUuid, this.ownerName, this.location,
                this.description, this.iconMaterial, this.iconData, this.locked, whitelistEnabled, this.visits, this.safeStatus,
                this.createdAt, updatedAt, this.lastVisitedAt);
    }

    public Warp withLocation(WarpLocation location, SafeStatus safeStatus, long updatedAt) {
        return new Warp(this.id, this.serverId, this.name, this.nameNormalized, this.ownerUuid, this.ownerName, location,
                this.description, this.iconMaterial, this.iconData, this.locked, this.whitelistEnabled, this.visits, safeStatus,
                this.createdAt, updatedAt, this.lastVisitedAt);
    }

    public Warp withName(WarpName name, long updatedAt) {
        return new Warp(this.id, this.serverId, name.value(), name.normalized(), this.ownerUuid, this.ownerName, this.location,
                this.description, this.iconMaterial, this.iconData, this.locked, this.whitelistEnabled, this.visits, this.safeStatus,
                this.createdAt, updatedAt, this.lastVisitedAt);
    }

    public Warp withOwner(UUID ownerUuid, String ownerName, long updatedAt) {
        return new Warp(this.id, this.serverId, this.name, this.nameNormalized, ownerUuid, ownerName, this.location,
                this.description, this.iconMaterial, this.iconData, this.locked, this.whitelistEnabled, this.visits, this.safeStatus,
                this.createdAt, updatedAt, this.lastVisitedAt);
    }
}
