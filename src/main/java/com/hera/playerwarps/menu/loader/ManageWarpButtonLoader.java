package com.hera.playerwarps.menu.loader;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.menu.MenuService;
import com.hera.playerwarps.menu.button.ManageWarpButton;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.button.ZButton;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ManageWarpButtonLoader extends AbstractButtonLoader {

    private final MenuService menuService;

    public ManageWarpButtonLoader(PlayerWarpsPlugin plugin, MenuService menuService) {
        super(plugin, "PWE_MANAGE_WARP");
        this.menuService = menuService;
    }

    @Override
    public Class<? extends Button> getButton() {
        return ManageWarpButton.class;
    }

    @Override
    protected ZButton create(YamlConfiguration configuration, String path) {
        String action = configuration.getString(path + "pwe-action", "");
        return new ManageWarpButton(this.menuService, action);
    }
}
