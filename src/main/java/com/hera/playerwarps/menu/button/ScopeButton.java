package com.hera.playerwarps.menu.button;

import com.hera.playerwarps.menu.MenuService;
import fr.maxlego08.menu.inventory.inventories.InventoryDefault;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class ScopeButton extends ConfiguredButton {

    public ScopeButton(MenuService menuService) {
        super(menuService);
    }

    @Override
    public void onLeftClick(Player player, InventoryClickEvent event, InventoryDefault inventory, int slot) {
        this.menuService.toggleScope(player);
    }
}
