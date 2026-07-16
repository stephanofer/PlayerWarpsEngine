package com.hera.playerwarps.command;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class LockSubCommand implements SubCommand {

    @Override
    public String name() {
        return "lock";
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
        if (context.args().length < 2) {
            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("command", context.label());
            context.messages().send(context.sender(), "messages.usage.lock", placeholders);
            return;
        }
        boolean locked = true;
        if (context.args().length >= 3) {
            if (context.args()[2].equalsIgnoreCase("true") || context.args()[2].equalsIgnoreCase("on")) {
                locked = true;
            } else if (context.args()[2].equalsIgnoreCase("false") || context.args()[2].equalsIgnoreCase("off")) {
                locked = false;
            } else {
                context.messages().send(context.sender(), "messages.invalid-boolean");
                return;
            }
        }
        context.services().warpService().setLocked(context.sender(), context.args()[1], locked);
    }
}
