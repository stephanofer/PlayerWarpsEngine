package com.hera.playerwarps.menu.loader;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.menu.MenuService;
import com.hera.playerwarps.menu.button.WhitelistListButton;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.button.ZButton;
import org.bukkit.configuration.file.YamlConfiguration;

public final class WhitelistListButtonLoader extends AbstractButtonLoader {

    private final MenuService menuService;

    public WhitelistListButtonLoader(PlayerWarpsPlugin plugin, MenuService menuService) {
        super(plugin, "PWE_WHITELIST_LIST");
        this.menuService = menuService;
    }

    @Override
    public Class<? extends Button> getButton() {
        return WhitelistListButton.class;
    }

    @Override
    protected ZButton create(YamlConfiguration configuration, String path) {
        return new WhitelistListButton(this.menuService);
    }
}
