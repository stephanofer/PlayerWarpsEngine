package com.hera.playerwarps.command;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SetOwnerSubCommand implements SubCommand {

    @Override
    public String name() {
        return "setowner";
    }

    @Override
    public List<String> aliases() {
        return Collections.emptyList();
    }

    @Override
    public String permission() {
        return "";
    }

    @Override
    public void execute(CommandContext context) {
        if (context.args().length < 3) {
            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("command", context.label());
            context.messages().send(context.sender(), "messages.usage.setowner", placeholders);
            return;
        }
        context.services().warpService().transferOwner(context.sender(), context.args()[1], context.args()[2]);
    }

    @Override
    public List<String> tabComplete(CommandContext context) {
        if (context.args().length != 3) {
            return Collections.emptyList();
        }
        String prefix = context.args()[2].toLowerCase();
        List<String> suggestions = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(prefix)) {
                suggestions.add(player.getName());
            }
        }
        return suggestions;
    }
}
