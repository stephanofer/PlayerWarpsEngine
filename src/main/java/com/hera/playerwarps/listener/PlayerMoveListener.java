package com.hera.playerwarps.listener;

import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.teleport.PendingTeleport;
import com.hera.playerwarps.teleport.TeleportService;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class PlayerMoveListener implements Listener {

    private final ConfigManager configManager;
    private final TeleportService teleportService;

    public PlayerMoveListener(ConfigManager configManager, TeleportService teleportService) {
        this.configManager = configManager;
        this.teleportService = teleportService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        if (!this.configManager.settings().teleportSettings().cancelOnMove()) {
            return;
        }

        PendingTeleport pending = this.teleportService.pending(event.getPlayer().getUniqueId());
        if (pending == null || !changedBlock(event.getFrom(), event.getTo())) {
            return;
        }

        this.teleportService.cancel(event.getPlayer().getUniqueId(), true);
    }

    private boolean changedBlock(Location from, Location to) {
        if (to == null) {
            return false;
        }
        return from.getBlockX() != to.getBlockX()
                || from.getBlockY() != to.getBlockY()
                || from.getBlockZ() != to.getBlockZ()
                || !from.getWorld().equals(to.getWorld());
    }
}
