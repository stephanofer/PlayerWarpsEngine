package com.hera.playerwarps;

import com.hera.playerwarps.bootstrap.PluginBootstrap;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerWarpsPlugin extends JavaPlugin {

    private PluginBootstrap bootstrap;

    @Override
    public void onEnable() {
        this.bootstrap = new PluginBootstrap(this);

        try {
            this.bootstrap.enable();
        } catch (Exception exception) {
            getLogger().severe("PlayerWarpsEngine failed to enable: " + exception.getMessage());
            exception.printStackTrace();

            try {
                this.bootstrap.disable();
            } catch (Exception disableException) {
                getLogger().severe("PlayerWarpsEngine failed to clean up after enable failure: " + disableException.getMessage());
                disableException.printStackTrace();
            }

            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (this.bootstrap == null) {
            return;
        }

        try {
            this.bootstrap.disable();
        } catch (Exception exception) {
            getLogger().severe("PlayerWarpsEngine failed to disable cleanly: " + exception.getMessage());
            exception.printStackTrace();
        }
    }
}
