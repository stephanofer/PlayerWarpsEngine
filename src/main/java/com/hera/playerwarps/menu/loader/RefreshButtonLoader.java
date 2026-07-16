package com.hera.playerwarps.menu.loader;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.menu.MenuService;
import com.hera.playerwarps.menu.button.RefreshButton;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.button.ZButton;
import org.bukkit.configuration.file.YamlConfiguration;

public final class RefreshButtonLoader extends AbstractButtonLoader {

    private final MenuService menuService;

    public RefreshButtonLoader(PlayerWarpsPlugin plugin, MenuService menuService) {
        super(plugin, "PWE_REFRESH");
        this.menuService = menuService;
    }

    @Override
    public Class<? extends Button> getButton() {
        return RefreshButton.class;
    }

    @Override
    protected ZButton create(YamlConfiguration configuration, String path) {
        return new RefreshButton(this.menuService);
    }
}
