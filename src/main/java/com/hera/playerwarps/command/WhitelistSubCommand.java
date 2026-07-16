package com.hera.playerwarps.command;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WhitelistSubCommand implements SubCommand {

    @Override
    public String name() {
        return "whitelist";
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
            sendUsage(context);
            return;
        }

        String action = context.args()[1].toLowerCase();
        String warp = context.args()[2];
        if (action.equals("enable")) {
            context.services().warpService().setWhitelistEnabled(context.sender(), warp, true);
            return;
        }
        if (action.equals("disable")) {
            context.services().warpService().setWhitelistEnabled(context.sender(), warp, false);
            return;
        }
        if (action.equals("list")) {
            context.services().warpService().listWhitelist(context.sender(), warp);
            return;
        }
        if (action.equals("add")) {
            if (context.args().length < 4) {
                sendUsage(context);
                return;
            }
            context.services().warpService().addWhitelist(context.sender(), warp, context.args()[3]);
            return;
        }
        if (action.equals("remove")) {
            if (context.args().length < 4) {
                sendUsage(context);
                return;
            }
            context.services().warpService().removeWhitelist(context.sender(), warp, context.args()[3]);
            return;
        }

        sendUsage(context);
    }

    @Override
    public List<String> tabComplete(CommandContext context) {
        if (context.args().length == 2) {
            return startsWith(context.args()[1], "enable", "disable", "add", "remove", "list");
        }
        if (context.args().length == 4 && context.args()[1].equalsIgnoreCase("add")) {
            String prefix = context.args()[3].toLowerCase();
            List<String> suggestions = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(prefix)) {
                    suggestions.add(player.getName());
                }
            }
            return suggestions;
        }
        return Collections.emptyList();
    }

    private List<String> startsWith(String prefixText, String... values) {
        String prefix = prefixText.toLowerCase();
        List<String> matches = new ArrayList<String>();
        for (String value : values) {
            if (value.startsWith(prefix)) {
                matches.add(value);
            }
        }
        return matches;
    }

    private void sendUsage(CommandContext context) {
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("command", context.label());
        context.messages().send(context.sender(), "messages.usage.whitelist", placeholders);
    }
}
