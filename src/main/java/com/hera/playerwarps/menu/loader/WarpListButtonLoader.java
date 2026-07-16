package com.hera.playerwarps.menu.loader;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.menu.MenuService;
import com.hera.playerwarps.menu.WarpScope;
import com.hera.playerwarps.menu.button.WarpListButton;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.button.ZButton;
import org.bukkit.configuration.file.YamlConfiguration;

public final class WarpListButtonLoader extends AbstractButtonLoader {

    private final MenuService menuService;
    private final WarpScope forcedScope;

    public WarpListButtonLoader(PlayerWarpsPlugin plugin, MenuService menuService, String name, WarpScope forcedScope) {
        super(plugin, name);
        this.menuService = menuService;
        this.forcedScope = forcedScope;
    }

    @Override
    public Class<? extends Button> getButton() {
        return WarpListButton.class;
    }

    @Override
    protected ZButton create(YamlConfiguration configuration, String path) {
        return new WarpListButton(this.menuService, this.forcedScope);
    }
}
