package com.hera.playerwarps.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RemoveAllSubCommand implements SubCommand {

    @Override
    public String name() {
        return "removeall";
    }

    @Override
    public List<String> aliases() {
        return Collections.emptyList();
    }

    @Override
    public String permission() {
        return "pwarp.admin.removeall";
    }

    @Override
    public void execute(CommandContext context) {
        if (context.args().length < 2) {
            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("command", context.label());
            context.messages().send(context.sender(), "messages.usage.removeall", placeholders);
            return;
        }
        context.services().warpService().removeAll(context.sender(), context.args()[1]);
    }
}
