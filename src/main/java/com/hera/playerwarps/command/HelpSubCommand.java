package com.hera.playerwarps.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HelpSubCommand implements SubCommand {

    @Override
    public String name() {
        return "help";
    }

    @Override
    public List<String> aliases() {
        return Collections.emptyList();
    }

    @Override
    public String permission() {
        return "pwarp.help";
    }

    @Override
    public void execute(CommandContext context) {
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("command", context.label());

        context.messages().send(context.sender(), "messages.help.header", placeholders);
        context.messages().sendList(context.sender(), "messages.help.lines", placeholders);
    }
}
