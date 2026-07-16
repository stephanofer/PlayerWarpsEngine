package com.hera.playerwarps.listener;

import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.teleport.TeleportService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public final class PlayerDamageListener implements Listener {

    private final ConfigManager configManager;
    private final TeleportService teleportService;

    public PlayerDamageListener(ConfigManager configManager, TeleportService teleportService) {
        this.configManager = configManager;
        this.teleportService = teleportService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!this.configManager.settings().teleportSettings().cancelOnDamage() || !(event.getEntity() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getEntity();
        this.teleportService.cancel(player.getUniqueId(), true);
    }
}
