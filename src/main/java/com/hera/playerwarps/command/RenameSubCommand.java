package com.hera.playerwarps.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RenameSubCommand implements SubCommand {

    @Override
    public String name() {
        return "rename";
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
            context.messages().send(context.sender(), "messages.usage.rename", placeholders);
            return;
        }
        context.services().warpService().renameWarp(context.sender(), context.args()[1], context.args()[2]);
    }
}
