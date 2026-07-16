package com.hera.playerwarps.command;

import com.hera.playerwarps.bootstrap.Services;
import com.hera.playerwarps.config.ConfigManager;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReloadSubCommand implements SubCommand {

    private final ConfigManager configManager;
    private final CommandRegistrar commandRegistrar;
    private final PlayerWarpCommand rootCommand;
    private final Services services;

    public ReloadSubCommand(ConfigManager configManager, CommandRegistrar commandRegistrar, PlayerWarpCommand rootCommand, Services services) {
        this.configManager = configManager;
        this.commandRegistrar = commandRegistrar;
        this.rootCommand = rootCommand;
        this.services = services;
    }

    @Override
    public String name() {
        return "reload";
    }

    @Override
    public List<String> aliases() {
        return Collections.emptyList();
    }

    @Override
    public String permission() {
        return "pwarp.admin.reload";
    }

    @Override
    public void execute(CommandContext context) {
        Map<String, String> placeholders = new HashMap<String, String>();

        try {
            ConfigManager.LoadedConfig loadedConfig = this.configManager.load();
            this.services.menuService().validateReload(loadedConfig);
            this.commandRegistrar.reload(loadedConfig.settings(), this.rootCommand);
            this.configManager.apply(loadedConfig);
            this.services.warpLimitService().invalidateAll();
            this.services.menuService().reloadMenus();
            this.configManager.messages().send(context.sender(), "messages.reload.success");
        } catch (CommandRegistrationException exception) {
            placeholders.put("error", exception.getMessage());
            context.messages().send(context.sender(), "messages.reload.invalid-command", placeholders);
        } catch (Exception exception) {
            placeholders.put("error", exception.getMessage() == null ? "unknown error" : exception.getMessage());
            context.messages().send(context.sender(), "messages.reload.failed", placeholders);
        }
    }
}
