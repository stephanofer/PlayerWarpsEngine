package com.hera.playerwarps.menu.loader;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.menu.MenuService;
import com.hera.playerwarps.menu.button.ScopeButton;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.button.ZButton;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ScopeButtonLoader extends AbstractButtonLoader {

    private final MenuService menuService;

    public ScopeButtonLoader(PlayerWarpsPlugin plugin, MenuService menuService) {
        super(plugin, "PWE_SCOPE");
        this.menuService = menuService;
    }

    @Override
    public Class<? extends Button> getButton() {
        return ScopeButton.class;
    }

    @Override
    protected ZButton create(YamlConfiguration configuration, String path) {
        return new ScopeButton(this.menuService);
    }
}
