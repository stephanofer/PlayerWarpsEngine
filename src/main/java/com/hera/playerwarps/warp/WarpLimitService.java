package com.hera.playerwarps.warp;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.config.LimitSettings;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public final class WarpLimitService {

    private final ConfigManager configManager;
    private final WarpCache warpCache;
    private final Cache<UUID, Integer> playerLimitCache;

    public WarpLimitService(ConfigManager configManager, WarpCache warpCache) {
        this.configManager = configManager;
        this.warpCache = warpCache;
        this.playerLimitCache = Caffeine.newBuilder()
                .maximumSize(10000L)
                .expireAfterWrite(30L, TimeUnit.SECONDS)
                .build();
    }

    public int calculate(Player player) {
        return calculate(player.getUniqueId(), player);
    }

    public int cached(Player player) {
        final UUID uuid = player.getUniqueId();
        Integer cached = this.playerLimitCache.getIfPresent(uuid);
        if (cached != null) {
            return cached;
        }

        int calculated = calculate(player);
        this.playerLimitCache.put(uuid, calculated);
        return calculated;
    }

    public Integer cachedValue(UUID playerUuid) {
        return this.playerLimitCache.getIfPresent(playerUuid);
    }

    public int calculateOffline(UUID playerUuid) {
        LimitSettings settings = this.configManager.settings().limitSettings();
        return settings.defaultLimit() + this.warpCache.bonusOf(playerUuid);
    }

    public int remaining(Player player) {
        return Math.max(0, calculate(player) - this.warpCache.countByOwner(player.getUniqueId()));
    }

    public void invalidateAll() {
        this.playerLimitCache.invalidateAll();
    }

    private int calculate(UUID playerUuid, Player player) {
        LimitSettings settings = this.configManager.settings().limitSettings();
        int limit = settings.defaultLimit();

        for (Map.Entry<String, Integer> entry : settings.permissionLimits().entrySet()) {
            if (player.hasPermission(entry.getKey())) {
                limit = Math.max(limit, entry.getValue());
            }
        }

        if (settings.numericPermissionEnabled()) {
            String prefix = settings.numericPermissionPrefix();
            for (PermissionAttachmentInfo permissionInfo : player.getEffectivePermissions()) {
                String permission = permissionInfo.getPermission().toLowerCase(Locale.ENGLISH);
                if (!permissionInfo.getValue() || !permission.startsWith(prefix)) {
                    continue;
                }

                String amountText = permission.substring(prefix.length());
                try {
                    int amount = Integer.parseInt(amountText);
                    if (amount > 0) {
                        limit = Math.max(limit, amount);
                    }
                } catch (NumberFormatException ignored) {
                    // Invalid numeric permissions are ignored so one bad node cannot break limit calculation.
                }
            }
        }

        return limit + this.warpCache.bonusOf(playerUuid);
    }
}
