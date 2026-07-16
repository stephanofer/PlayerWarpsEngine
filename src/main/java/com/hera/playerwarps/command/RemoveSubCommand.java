package com.hera.playerwarps.command;

import com.hera.playerwarps.warp.Warp;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RemoveSubCommand implements SubCommand {

    @Override
    public String name() {
        return "remove";
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("del");
    }

    @Override
    public String permission() {
        return "";
    }

    @Override
    public void execute(CommandContext context) {
        CommandSender sender = context.sender();
        if (context.args().length < 2) {
            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("command", context.label());
            context.messages().send(sender, "messages.usage.remove", placeholders);
            return;
        }

        context.services().warpService().removeWarp(sender, context.args()[1], context.args().length >= 3 ? context.args()[2] : null);
    }

    @Override
    public List<String> tabComplete(CommandContext context) {
        if (!(context.sender() instanceof Player) || context.args().length != 2) {
            return new ArrayList<String>();
        }
        Player player = (Player) context.sender();
        String prefix = context.args()[1].toLowerCase();
        List<String> suggestions = new ArrayList<String>();
        for (Warp warp : context.services().warpCache().getByOwner(player.getUniqueId())) {
            if (warp.nameNormalized().startsWith(prefix)) {
                suggestions.add(warp.name());
            }
        }
        return suggestions;
    }
}
