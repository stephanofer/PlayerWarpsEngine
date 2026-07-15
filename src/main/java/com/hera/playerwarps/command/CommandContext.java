package com.hera.playerwarps.command;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.bootstrap.Services;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.config.Messages;
import com.hera.playerwarps.config.Settings;
import org.bukkit.command.CommandSender;

public final class CommandContext {

    private final PlayerWarpsPlugin plugin;
    private final ConfigManager configManager;
    private final Services services;
    private final CommandSender sender;
    private final String label;
    private final String[] args;

    public CommandContext(PlayerWarpsPlugin plugin, ConfigManager configManager, Services services, CommandSender sender, String label, String[] args) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.services = services;
        this.sender = sender;
        this.label = label;
        this.args = args;
    }

    public PlayerWarpsPlugin plugin() {
        return this.plugin;
    }

    public ConfigManager configManager() {
        return this.configManager;
    }

    public Services services() {
        return this.services;
    }

    public Messages messages() {
        return this.configManager.messages();
    }

    public Settings settings() {
        return this.configManager.settings();
    }

    public CommandSender sender() {
        return this.sender;
    }

    public String label() {
        return this.label;
    }

    public String[] args() {
        return this.args;
    }
}
