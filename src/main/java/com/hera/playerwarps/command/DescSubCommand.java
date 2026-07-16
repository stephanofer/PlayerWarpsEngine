package com.hera.playerwarps.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DescSubCommand implements SubCommand {

    @Override
    public String name() {
        return "desc";
    }

    @Override
    public List<String> aliases() {
        return Arrays.asList("description");
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
        if (context.args()[1].equalsIgnoreCase("remove")) {
            context.services().warpService().removeDescription(context.sender(), context.args()[2]);
            return;
        }
        if (context.args()[1].equalsIgnoreCase("set")) {
            if (context.args().length < 4) {
                sendUsage(context);
                return;
            }
            context.services().warpService().setDescription(context.sender(), context.args()[2], join(context.args(), 3));
            return;
        }
        sendUsage(context);
    }

    private void sendUsage(CommandContext context) {
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("command", context.label());
        context.messages().send(context.sender(), "messages.usage.desc", placeholders);
    }

    private String join(String[] args, int start) {
        StringBuilder builder = new StringBuilder();
        for (int index = start; index < args.length; index++) {
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.toString().trim();
    }
}
