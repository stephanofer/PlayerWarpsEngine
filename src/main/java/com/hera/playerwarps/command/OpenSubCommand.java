package com.hera.playerwarps.command;

import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public final class OpenSubCommand implements SubCommand {

    @Override
    public String name() {
        return "open";
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("menu");
    }

    @Override
    public String permission() {
        return "pwarp.open";
    }

    @Override
    public void execute(CommandContext context) {
        if (!(context.sender() instanceof Player)) {
            context.messages().send(context.sender(), "messages.only-player");
            return;
        }
        Player player = (Player) context.sender();
        if (context.args().length == 1) {
            context.services().menuService().openMain(player);
            return;
        }

        String menu = context.args()[1].toLowerCase();
        int page = 1;
        if (context.args().length >= 3) {
            try {
                page = Integer.parseInt(context.args()[2]);
            } catch (NumberFormatException exception) {
                context.messages().send(context.sender(), "messages.list-invalid-page");
                return;
            }
        }

        if (!menu.equals("main") && !menu.equals("manage") && !menu.equals("whitelist")) {
            context.messages().send(context.sender(), "messages.menu-not-found");
            return;
        }
        context.services().menuService().open(player, menu, page);
    }

    @Override
    public List<String> tabComplete(CommandContext context) {
        if (context.args().length == 2) {
            return Arrays.asList("main", "manage", "whitelist");
        }
        return SubCommand.super.tabComplete(context);
    }
}
