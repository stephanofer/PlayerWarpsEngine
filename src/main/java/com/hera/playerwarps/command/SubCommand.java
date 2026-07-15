package com.hera.playerwarps.command;

import java.util.Collections;
import java.util.List;

public interface SubCommand {

    String name();

    List<String> aliases();

    String permission();

    void execute(CommandContext context);

    default List<String> tabComplete(CommandContext context) {
        return Collections.emptyList();
    }
}
