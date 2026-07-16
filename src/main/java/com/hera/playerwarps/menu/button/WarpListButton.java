package com.hera.playerwarps.menu.button;

import com.hera.playerwarps.menu.MenuService;
import com.hera.playerwarps.menu.MenuSession;
import com.hera.playerwarps.menu.WarpScope;
import com.hera.playerwarps.warp.Warp;
import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.menu.inventory.inventories.InventoryDefault;
import fr.maxlego08.menu.zcore.utils.inventory.ItemButton;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class WarpListButton extends ConfiguredButton implements PaginateButton {

    private final WarpScope forcedScope;

    public WarpListButton(MenuService menuService, WarpScope forcedScope) {
        super(menuService);
        this.forcedScope = forcedScope;
    }

    @Override
    public boolean hasCustomRender() {
        return true;
    }

    @Override
    public int getPaginationSize(Player player) {
        return this.menuService.browse(player, session(player)).size();
    }

    @Override
    public void onRender(final Player player, InventoryDefault inventory) {
        MenuSession session = session(player);
        List<Warp> warps = this.menuService.browse(player, session);
        List<Integer> slots = new ArrayList<Integer>((Collection<Integer>) getSlots());
        int page = Math.max(1, inventory.getPage());
        int from = (page - 1) * slots.size();
        int to = Math.min(warps.size(), from + slots.size());

        if (from >= warps.size()) {
            return;
        }

        for (int index = from; index < to; index++) {
            final Warp warp = warps.get(index);
            int slot = slots.get(index - from).intValue();
            ItemStack itemStack = this.menuService.buildWarpItem(player, this, warp);
            ItemButton itemButton = inventory.addItem(slot, itemStack);
            itemButton.setLeftClick(event -> this.menuService.teleportFromMenu(player, warp.id()));
            itemButton.setRightClick(event -> this.menuService.openManage(player, warp.id()));
        }
    }

    private MenuSession session(Player player) {
        MenuSession session = this.menuService.session(player);
        if (this.forcedScope != null) {
            session.scope(this.forcedScope);
        }
        return session;
    }
}
