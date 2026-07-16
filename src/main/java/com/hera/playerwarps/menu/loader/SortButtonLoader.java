package com.hera.playerwarps.menu.loader;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.menu.MenuService;
import com.hera.playerwarps.menu.button.SortButton;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.button.ZButton;
import org.bukkit.configuration.file.YamlConfiguration;

public final class SortButtonLoader extends AbstractButtonLoader {

    private final MenuService menuService;

    public SortButtonLoader(PlayerWarpsPlugin plugin, MenuService menuService) {
        super(plugin, "PWE_FILTER_SORT");
        this.menuService = menuService;
    }

    @Override
    public Class<? extends Button> getButton() {
        return SortButton.class;
    }

    @Override
    protected ZButton create(YamlConfiguration configuration, String path) {
        return new SortButton(this.menuService);
    }
}
