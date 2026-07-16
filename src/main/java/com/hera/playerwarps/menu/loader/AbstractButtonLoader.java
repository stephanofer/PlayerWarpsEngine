package com.hera.playerwarps.menu.loader;

import com.hera.playerwarps.PlayerWarpsPlugin;
import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.button.DefaultButtonValue;
import fr.maxlego08.menu.api.loader.ButtonLoader;
import fr.maxlego08.menu.button.ZButton;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public abstract class AbstractButtonLoader implements ButtonLoader {

    private final PlayerWarpsPlugin plugin;
    private final String name;

    protected AbstractButtonLoader(PlayerWarpsPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Plugin getPlugin() {
        return this.plugin;
    }

    @Override
    public Button load(YamlConfiguration configuration, String path, DefaultButtonValue defaultButtonValue) {
        return create(configuration, normalizePath(path));
    }

    protected abstract ZButton create(YamlConfiguration configuration, String path);

    private static String normalizePath(String path) {
        return path.endsWith(".") ? path : path + ".";
    }
}
