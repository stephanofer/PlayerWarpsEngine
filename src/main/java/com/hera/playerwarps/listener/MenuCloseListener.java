package com.hera.playerwarps.listener;

import com.hera.playerwarps.menu.MenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class MenuCloseListener implements Listener {

    private final MenuService menuService;

    public MenuCloseListener(MenuService menuService) {
        this.menuService = menuService;
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            this.menuService.handleInventoryClose((Player) event.getPlayer());
        }
    }
}
