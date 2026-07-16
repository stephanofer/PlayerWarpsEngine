package com.hera.playerwarps.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PurgeSubCommand implements SubCommand {

    @Override
    public String name() {
        return "purge";
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("clean");
    }

    @Override
    public String permission() {
        return "pwarp.admin.purge";
    }

    @Override
    public void execute(CommandContext context) {
        if (context.args().length < 2) {
            sendUsage(context);
            return;
        }

        String type = context.args()[1].toLowerCase();
        if (type.equals("unsafe")) {
            if (context.args().length >= 3 && context.args()[2].equalsIgnoreCase("confirm")) {
                context.services().warpPurgeService().confirm(context.sender(), "unsafe");
                return;
            }
            context.services().warpPurgeService().previewUnsafe(context.sender());
            return;
        }

        if (type.equals("inactive")) {
            if (context.args().length < 3) {
                sendUsage(context);
                return;
            }
            Integer days = parseInt(context.args()[2]);
            if (days == null) {
                context.messages().send(context.sender(), "messages.invalid-days");
                return;
            }
            if (context.args().length >= 4 && context.args()[3].equalsIgnoreCase("confirm")) {
                context.services().warpPurgeService().confirm(context.sender(), "inactive");
                return;
            }
            context.services().warpPurgeService().previewInactive(context.sender(), days.intValue());
            return;
        }

        sendUsage(context);
    }

    private Integer parseInt(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void sendUsage(CommandContext context) {
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("command", context.label());
        context.messages().send(context.sender(), "messages.usage.purge", placeholders);
    }
}
