package com.hera.playerwarps.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SetSubCommand implements SubCommand {

    @Override
    public String name() {
        return "set";
    }

    @Override
    public List<String> aliases() {
        return Collections.emptyList();
    }

    @Override
    public String permission() {
        return "pwarp.set";
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();
        if (!(sender instanceof Player)) {
            context.messages().send(sender, "messages.only-player");
            return;
        }
        if (context.args().length < 2) {
            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("command", context.label());
            context.messages().send(sender, "messages.usage.set", placeholders);
            return;
        }

        context.services().warpService().createWarp((Player) sender, context.args()[1]);
    }
}
