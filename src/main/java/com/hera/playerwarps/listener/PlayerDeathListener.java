package com.hera.playerwarps.listener;

import com.hera.playerwarps.teleport.TeleportService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public final class PlayerDeathListener implements Listener {

    private final TeleportService teleportService;

    public PlayerDeathListener(TeleportService teleportService) {
        this.teleportService = teleportService;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        this.teleportService.cancel(event.getEntity().getUniqueId(), false);
    }
}
