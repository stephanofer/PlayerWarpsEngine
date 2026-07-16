package com.hera.playerwarps.placeholder;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.warp.VisitBuffer;
import com.hera.playerwarps.warp.Warp;
import com.hera.playerwarps.warp.WarpCache;
import com.hera.playerwarps.warp.WarpLimitService;
import com.hera.playerwarps.warp.WarpName;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public final class PlayerWarpsExpansion extends PlaceholderExpansion {

    private final PlayerWarpsPlugin plugin;
    private final ConfigManager configManager;
    private final WarpCache warpCache;
    private final WarpLimitService warpLimitService;
    private final VisitBuffer visitBuffer;

    public PlayerWarpsExpansion(PlayerWarpsPlugin plugin, ConfigManager configManager, WarpCache warpCache,
                                WarpLimitService warpLimitService, VisitBuffer visitBuffer) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.warpCache = warpCache;
        this.warpLimitService = warpLimitService;
        this.visitBuffer = visitBuffer;
    }

    @Override
    public String getIdentifier() {
        return "playerwarps";
    }

    @Override
    public String getAuthor() {
        return "HERA Network";
    }

    @Override
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        if (params == null) {
            return null;
        }

        if (params.equalsIgnoreCase("total")) {
            return String.valueOf(this.warpCache.totalWarps());
        }

        if (player == null && (params.equalsIgnoreCase("amount") || params.equalsIgnoreCase("limit") || params.equalsIgnoreCase("remaining"))) {
            return fallback();
        }

        if (params.equalsIgnoreCase("amount")) {
            return String.valueOf(this.warpCache.countByOwner(player.getUniqueId()));
        }
        if (params.equalsIgnoreCase("limit")) {
            return limit(player);
        }
        if (params.equalsIgnoreCase("remaining")) {
            String limit = limit(player);
            if (limit.equals(fallback())) {
                return fallback();
            }
            int remaining = Math.max(0, Integer.parseInt(limit) - this.warpCache.countByOwner(player.getUniqueId()));
            return String.valueOf(remaining);
        }

        if (params.startsWith("visits_")) {
            Warp warp = warp(params.substring("visits_".length()));
            return warp == null ? fallback() : String.valueOf(this.visitBuffer.effectiveVisits(warp));
        }
        if (params.startsWith("owner_")) {
            Warp warp = warp(params.substring("owner_".length()));
            return warp == null ? fallback() : warp.ownerName();
        }
        if (params.startsWith("locked_")) {
            Warp warp = warp(params.substring("locked_".length()));
            if (warp == null) {
                return fallback();
            }
            return warp.locked() ? this.configManager.settings().placeholderSettings().locked()
                    : this.configManager.settings().placeholderSettings().unlocked();
        }

        return null;
    }

    private String limit(OfflinePlayer offlinePlayer) {
        UUID uuid = offlinePlayer.getUniqueId();
        Integer cached = this.warpLimitService.cachedValue(uuid);
        if (cached != null) {
            return String.valueOf(cached.intValue());
        }

        Player online = offlinePlayer.getPlayer();
        if (online != null && online.isOnline() && Bukkit.isPrimaryThread()) {
            return String.valueOf(this.warpLimitService.cached(online));
        }

        if (online == null || !online.isOnline()) {
            return String.valueOf(this.warpLimitService.calculateOffline(uuid));
        }

        return fallback();
    }

    private Warp warp(String rawName) {
        if (rawName == null || rawName.trim().isEmpty()) {
            return null;
        }
        return this.warpCache.getByName(WarpName.normalize(rawName)).orElse(null);
    }

    private String fallback() {
        return this.configManager.settings().placeholderSettings().fallback();
    }
}
