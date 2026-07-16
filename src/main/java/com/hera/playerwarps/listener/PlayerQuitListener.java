package com.hera.playerwarps.listener;

import com.hera.playerwarps.menu.MenuService;
import com.hera.playerwarps.teleport.TeleportService;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerQuitListener implements Listener {

    private final TeleportService teleportService;
    private final MenuService menuService;

    public PlayerQuitListener(TeleportService teleportService, MenuService menuService) {
        this.teleportService = teleportService;
        this.menuService = menuService;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.teleportService.cancel(event.getPlayer().getUniqueId(), false);
        this.menuService.removeSession(event.getPlayer());
    }
}
