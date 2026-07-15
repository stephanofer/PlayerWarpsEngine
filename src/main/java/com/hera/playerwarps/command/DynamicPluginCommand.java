package com.hera.playerwarps.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;

import java.util.List;

public final class DynamicPluginCommand extends Command {

    private final Plugin plugin;
    private final PlayerWarpCommand handler;

    public DynamicPluginCommand(String name, List<String> aliases, Plugin plugin, PlayerWarpCommand handler) {
        super(name, "Player warps command.", "/" + name + " help", aliases);
        this.plugin = plugin;
        this.handler = handler;
    }

    @Override
    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        if (!this.plugin.isEnabled()) {
            return true;
        }
        return this.handler.execute(sender, commandLabel, args);
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String alias, String[] args) throws IllegalArgumentException {
        return this.handler.tabComplete(sender, alias, args);
    }
}
