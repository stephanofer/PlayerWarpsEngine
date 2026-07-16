package com.hera.playerwarps.command;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.bootstrap.Services;
import com.hera.playerwarps.config.ConfigManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PlayerWarpCommand {

    private final PlayerWarpsPlugin plugin;
    private final ConfigManager configManager;
    private final Services services;
    private final Map<String, SubCommand> subCommands = new HashMap<String, SubCommand>();
    private final List<SubCommand> rootSubCommands = new ArrayList<SubCommand>();

    public PlayerWarpCommand(PlayerWarpsPlugin plugin, ConfigManager configManager, CommandRegistrar commandRegistrar, Services services) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.services = services;

        registerSubCommand(new HelpSubCommand());
        registerSubCommand(new OpenSubCommand());
        registerSubCommand(new SetSubCommand());
        registerSubCommand(new RemoveSubCommand());
        registerSubCommand(new ListSubCommand());
        registerSubCommand(new AmountSubCommand());
        registerSubCommand(new LockSubCommand());
        registerSubCommand(new ResetSubCommand());
        registerSubCommand(new RenameSubCommand());
        registerSubCommand(new SetOwnerSubCommand());
        registerSubCommand(new WhitelistSubCommand());
        registerSubCommand(new RemoveAllSubCommand());
        registerSubCommand(new AddWarpsSubCommand());
        registerSubCommand(new PurgeSubCommand());
        registerSubCommand(new ReloadSubCommand(configManager, commandRegistrar, this, services));
    }

    public boolean execute(CommandSender sender, String label, String[] args) {
        CommandContext context = new CommandContext(this.plugin, this.configManager, this.services, sender, label, args);

        if (args.length == 0) {
            executeOpenMenu(context);
            return true;
        }

        SubCommand subCommand = this.subCommands.get(args[0].toLowerCase(Locale.ENGLISH));
        if (subCommand == null) {
            executeWarpTeleport(context);
            return true;
        }

        executeSubCommand(subCommand, context);
        return true;
    }

    public List<String> tabComplete(CommandSender sender, String label, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ENGLISH);
            List<String> suggestions = new ArrayList<String>();

            for (SubCommand subCommand : this.rootSubCommands) {
                if (!hasPermission(sender, subCommand.permission())) {
                    continue;
                }
                if (subCommand.name().startsWith(prefix)) {
                    suggestions.add(subCommand.name());
                }
            }

            for (com.hera.playerwarps.warp.Warp warp : this.services.warpCache().allSnapshot()) {
                if (warp.nameNormalized().startsWith(prefix)) {
                    suggestions.add(warp.name());
                }
            }

            return suggestions;
        }

        SubCommand subCommand = this.subCommands.get(args[0].toLowerCase(Locale.ENGLISH));
        if (subCommand == null || !hasPermission(sender, subCommand.permission())) {
            return Collections.emptyList();
        }

        return subCommand.tabComplete(new CommandContext(this.plugin, this.configManager, this.services, sender, label, args));
    }

    private void executeSubCommand(SubCommand subCommand, CommandContext context) {
        if (!hasPermission(context.sender(), subCommand.permission())) {
            context.messages().send(context.sender(), "messages.no-permission");
            return;
        }

        subCommand.execute(context);
    }

    private void executeWarpTeleport(CommandContext context) {
        if (!hasPermission(context.sender(), "pwarp.warp")) {
            context.messages().send(context.sender(), "messages.no-permission");
            return;
        }
        if (!(context.sender() instanceof Player)) {
            context.messages().send(context.sender(), "messages.only-player");
            return;
        }
        this.services.warpService().teleport((Player) context.sender(), context.args()[0]);
    }

    private void executeOpenMenu(CommandContext context) {
        if (!hasPermission(context.sender(), "pwarp.open")) {
            context.messages().send(context.sender(), "messages.no-permission");
            return;
        }
        if (!(context.sender() instanceof Player)) {
            context.messages().send(context.sender(), "messages.only-player");
            return;
        }
        this.services.menuService().openMain((Player) context.sender());
    }

    private void registerSubCommand(SubCommand subCommand) {
        this.rootSubCommands.add(subCommand);
        this.subCommands.put(subCommand.name(), subCommand);
        for (String alias : subCommand.aliases()) {
            this.subCommands.put(alias.toLowerCase(Locale.ENGLISH), subCommand);
        }
    }

    private boolean hasPermission(CommandSender sender, String permission) {
        return permission == null || permission.isEmpty() || sender.hasPermission(permission);
    }
}
