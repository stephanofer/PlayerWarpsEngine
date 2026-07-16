package com.hera.playerwarps.config;

import dev.dejvokep.boostedyaml.YamlDocument;

import java.util.Locale;

public final class TeleportSettings {

    private final int delaySeconds;
    private final int cooldownSeconds;
    private final boolean cancelOnMove;
    private final boolean cancelOnDamage;
    private final boolean preloadChunk;
    private final boolean generateMissingChunks;
    private final int maxTeleportsPerTick;
    private final UnsafePolicy unsafePolicy;
    private final String bypassDelayPermission;

    private TeleportSettings(int delaySeconds, int cooldownSeconds, boolean cancelOnMove, boolean cancelOnDamage, boolean preloadChunk,
                             boolean generateMissingChunks, int maxTeleportsPerTick, UnsafePolicy unsafePolicy, String bypassDelayPermission) {
        this.delaySeconds = delaySeconds;
        this.cooldownSeconds = cooldownSeconds;
        this.cancelOnMove = cancelOnMove;
        this.cancelOnDamage = cancelOnDamage;
        this.preloadChunk = preloadChunk;
        this.generateMissingChunks = generateMissingChunks;
        this.maxTeleportsPerTick = maxTeleportsPerTick;
        this.unsafePolicy = unsafePolicy;
        this.bypassDelayPermission = bypassDelayPermission;
    }

    public static TeleportSettings from(YamlDocument config) {
        int delaySeconds = Math.max(0, config.getInt("teleport.delay-seconds", 3));
        int cooldownSeconds = Math.max(0, config.getInt("teleport.cooldown-seconds", 2));
        boolean cancelOnMove = config.getBoolean("teleport.cancel-on-move", true);
        boolean cancelOnDamage = config.getBoolean("teleport.cancel-on-damage", true);
        boolean preloadChunk = config.getBoolean("teleport.preload-chunk", true);
        boolean generateMissingChunks = config.getBoolean("teleport.generate-missing-chunks", false);
        int maxTeleportsPerTick = Math.max(1, config.getInt("teleport.max-teleports-per-tick", 3));
        String bypassDelayPermission = config.getString("teleport.bypass-delay-permission", "pwarp.delay.bypass").trim();

        String unsafePolicyName = config.getString("teleport.unsafe-policy", "BLOCK").trim().toUpperCase(Locale.ENGLISH);
        UnsafePolicy unsafePolicy;
        try {
            unsafePolicy = UnsafePolicy.valueOf(unsafePolicyName);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("teleport.unsafe-policy must be BLOCK or WARN");
        }

        return new TeleportSettings(delaySeconds, cooldownSeconds, cancelOnMove, cancelOnDamage, preloadChunk, generateMissingChunks,
                maxTeleportsPerTick, unsafePolicy, bypassDelayPermission);
    }

    public int delaySeconds() {
        return this.delaySeconds;
    }

    public int cooldownSeconds() {
        return this.cooldownSeconds;
    }

    public boolean cancelOnMove() {
        return this.cancelOnMove;
    }

    public boolean cancelOnDamage() {
        return this.cancelOnDamage;
    }

    public boolean preloadChunk() {
        return this.preloadChunk;
    }

    public boolean generateMissingChunks() {
        return this.generateMissingChunks;
    }

    public int maxTeleportsPerTick() {
        return this.maxTeleportsPerTick;
    }

    public UnsafePolicy unsafePolicy() {
        return this.unsafePolicy;
    }

    public String bypassDelayPermission() {
        return this.bypassDelayPermission;
    }

    public enum UnsafePolicy {
        BLOCK,
        WARN
    }
}
