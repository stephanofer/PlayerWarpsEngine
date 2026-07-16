package com.hera.playerwarps.menu.button;

import com.hera.playerwarps.menu.MenuService;
import fr.maxlego08.menu.inventory.inventories.InventoryDefault;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class ManageWarpButton extends ConfiguredButton {

    private final String action;

    public ManageWarpButton(MenuService menuService, String action) {
        super(menuService);
        this.action = action;
    }

    @Override
    public void onLeftClick(Player player, InventoryClickEvent event, InventoryDefault inventory, int slot) {
        this.menuService.manageAction(player, this.action);
    }
}
