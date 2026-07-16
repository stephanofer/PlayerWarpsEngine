package com.hera.playerwarps.command;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AmountSubCommand implements SubCommand {

    @Override
    public String name() {
        return "amount";
    }

    @Override
    public List<String> aliases() {
        return Collections.emptyList();
    }

    @Override
    public String permission() {
        return "pwarp.amount";
    }

    @Override
    public void execute(CommandContext context) {
        if (context.args().length >= 2) {
            Player target = Bukkit.getPlayerExact(context.args()[1]);
            if (target == null) {
                Map<String, String> placeholders = new HashMap<String, String>();
                placeholders.put("player", context.args()[1]);
                context.messages().send(context.sender(), "messages.player-not-online", placeholders);
                return;
            }
            sendAmount(context, target, "messages.amount-other");
            return;
        }

        CommandSender sender = context.sender();
        if (!(sender instanceof Player)) {
            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("command", context.label());
            context.messages().send(sender, "messages.usage.amount", placeholders);
            return;
        }

        sendAmount(context, (Player) sender, "messages.amount-self");
    }

    @Override
    public List<String> tabComplete(CommandContext context) {
        if (context.args().length != 2) {
            return Collections.emptyList();
        }
        String prefix = context.args()[1].toLowerCase();
        List<String> suggestions = new ArrayList<String>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getName().toLowerCase().startsWith(prefix)) {
                suggestions.add(player.getName());
            }
        }
        return suggestions;
    }

    private void sendAmount(CommandContext context, Player target, String path) {
        int amount = context.services().warpCache().countByOwner(target.getUniqueId());
        int limit = context.services().warpLimitService().cached(target);
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("player", target.getName());
        placeholders.put("amount", String.valueOf(amount));
        placeholders.put("limit", String.valueOf(limit));
        placeholders.put("remaining", String.valueOf(Math.max(0, limit - amount)));
        context.messages().send(context.sender(), path, placeholders);
    }
}
