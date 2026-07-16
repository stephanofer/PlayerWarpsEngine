package com.hera.playerwarps.placeholder;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.warp.VisitBuffer;
import com.hera.playerwarps.warp.WarpCache;
import com.hera.playerwarps.warp.WarpLimitService;
import org.bukkit.Bukkit;

public final class PlaceholderIntegration {

    private final PlayerWarpsPlugin plugin;
    private final ConfigManager configManager;
    private final WarpCache warpCache;
    private final WarpLimitService warpLimitService;
    private final VisitBuffer visitBuffer;
    private PlayerWarpsExpansion expansion;

    public PlaceholderIntegration(PlayerWarpsPlugin plugin, ConfigManager configManager, WarpCache warpCache,
                                  WarpLimitService warpLimitService, VisitBuffer visitBuffer) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.warpCache = warpCache;
        this.warpLimitService = warpLimitService;
        this.visitBuffer = visitBuffer;
    }

    public void enable() {
        if (!Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            return;
        }
        this.expansion = new PlayerWarpsExpansion(this.plugin, this.configManager, this.warpCache, this.warpLimitService, this.visitBuffer);
        this.expansion.register();
        this.plugin.getLogger().info("PlaceholderAPI expansion registered.");
    }

    public void disable() {
        if (this.expansion != null) {
            this.expansion.unregister();
            this.expansion = null;
        }
    }
}
