package com.hera.playerwarps.config;

import com.hera.playerwarps.util.Names;
import dev.dejvokep.boostedyaml.YamlDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Settings {

    private final String serverId;
    private final String primaryCommand;
    private final List<String> commandAliases;
    private final WarpSettings warpSettings;
    private final LimitSettings limitSettings;

    private Settings(String serverId, String primaryCommand, List<String> commandAliases, WarpSettings warpSettings, LimitSettings limitSettings) {
        this.serverId = serverId;
        this.primaryCommand = primaryCommand;
        this.commandAliases = Collections.unmodifiableList(new ArrayList<String>(commandAliases));
        this.warpSettings = warpSettings;
        this.limitSettings = limitSettings;
    }

    public static Settings from(YamlDocument config) {
        String serverId = config.getString("server-id", "survival-1").trim();
        String primaryCommand = Names.normalizeCommandLabel(config.getString("commands.primary", "pwarp"));

        if (serverId.isEmpty()) {
            throw new IllegalArgumentException("server-id cannot be empty");
        }

        if (!Names.isValidCommandLabel(primaryCommand)) {
            throw new IllegalArgumentException("commands.primary must contain only letters, numbers, '_' or '-'");
        }

        Set<String> aliases = new LinkedHashSet<String>();
        for (String alias : config.getStringList("commands.aliases", Collections.<String>emptyList())) {
            String normalized = Names.normalizeCommandLabel(alias);
            if (normalized.isEmpty() || normalized.equals(primaryCommand)) {
                continue;
            }
            if (!Names.isValidCommandLabel(normalized)) {
                throw new IllegalArgumentException("commands.aliases contains an invalid label: " + alias);
            }
            aliases.add(normalized);
        }

        return new Settings(serverId, primaryCommand, new ArrayList<String>(aliases), WarpSettings.from(config), LimitSettings.from(config));
    }

    public String serverId() {
        return this.serverId;
    }

    public String primaryCommand() {
        return this.primaryCommand;
    }

    public List<String> commandAliases() {
        return this.commandAliases;
    }

    public WarpSettings warpSettings() {
        return this.warpSettings;
    }

    public LimitSettings limitSettings() {
        return this.limitSettings;
    }
}
