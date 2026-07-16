package com.hera.playerwarps.menu.button;

import com.hera.playerwarps.menu.MenuService;
import fr.maxlego08.menu.button.ZButton;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public abstract class ConfiguredButton extends ZButton {

    protected final MenuService menuService;

    protected ConfiguredButton(MenuService menuService) {
        this.menuService = menuService;
    }

    @Override
    public ItemStack getCustomItemStack(Player player) {
        return this.menuService.buildSessionItem(player, this);
    }
}
