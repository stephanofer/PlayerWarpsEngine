package com.hera.playerwarps.menu.button;

import com.hera.playerwarps.menu.MenuService;
import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.inventory.inventories.InventoryDefault;
import fr.maxlego08.menu.zcore.utils.inventory.ItemButton;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WhitelistListButton extends ConfiguredButton implements PaginateButton {

    public WhitelistListButton(MenuService menuService) {
        super(menuService);
    }

    @Override
    public boolean hasCustomRender() {
        return true;
    }

    @Override
    public int getPaginationSize(Player player) {
        return this.menuService.whitelistMembers(player).size();
    }

    @Override
    public void onRender(final Player player, InventoryDefault inventory) {
        List<Map.Entry<UUID, String>> members = this.menuService.whitelistMembers(player);
        List<Integer> slots = new ArrayList<Integer>((Collection<Integer>) getSlots());
        int page = Math.max(1, inventory.getPage());
        int from = (page - 1) * slots.size();
        int to = Math.min(members.size(), from + slots.size());
        if (from >= members.size()) {
            return;
        }

        for (int index = from; index < to; index++) {
            final Map.Entry<UUID, String> member = members.get(index);
            int slot = slots.get(index - from).intValue();
            ItemStack itemStack = this.menuService.buildWhitelistItem(player, this, member);
            ItemButton itemButton = inventory.addItem(slot, itemStack);
            itemButton.setLeftClick(event -> this.menuService.removeWhitelistFromMenu(player, member.getKey(), member.getValue()));
        }
    }
}
