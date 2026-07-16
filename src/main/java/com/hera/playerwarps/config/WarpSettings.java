package com.hera.playerwarps.config;

import dev.dejvokep.boostedyaml.YamlDocument;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class WarpSettings {

    private final Pattern namePattern;
    private final Set<String> reservedNames;
    private final AllowedWorldMode allowedWorldMode;
    private final Set<String> allowedWorlds;
    private final boolean defaultLocked;
    private final boolean defaultWhitelistEnabled;

    private WarpSettings(Pattern namePattern, Set<String> reservedNames, AllowedWorldMode allowedWorldMode,
                         Set<String> allowedWorlds, boolean defaultLocked, boolean defaultWhitelistEnabled) {
        this.namePattern = namePattern;
        this.reservedNames = Collections.unmodifiableSet(new LinkedHashSet<String>(reservedNames));
        this.allowedWorldMode = allowedWorldMode;
        this.allowedWorlds = Collections.unmodifiableSet(new LinkedHashSet<String>(allowedWorlds));
        this.defaultLocked = defaultLocked;
        this.defaultWhitelistEnabled = defaultWhitelistEnabled;
    }

    public static WarpSettings from(YamlDocument config) {
        String regex = config.getString("warps.name-regex", "^[a-zA-Z0-9_-]{3,32}$");
        Pattern namePattern;
        try {
            namePattern = Pattern.compile(regex);
        } catch (PatternSyntaxException exception) {
            throw new IllegalArgumentException("warps.name-regex is invalid: " + exception.getMessage(), exception);
        }

        Set<String> reservedNames = new LinkedHashSet<String>();
        for (String reservedName : config.getStringList("warps.reserved-names", Collections.<String>emptyList())) {
            String normalized = normalize(reservedName);
            if (!normalized.isEmpty()) {
                reservedNames.add(normalized);
            }
        }

        String modeName = config.getString("warps.allowed-worlds.mode", "BLACKLIST").trim().toUpperCase(Locale.ENGLISH);
        AllowedWorldMode allowedWorldMode;
        try {
            allowedWorldMode = AllowedWorldMode.valueOf(modeName);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("warps.allowed-worlds.mode must be BLACKLIST or WHITELIST");
        }

        Set<String> allowedWorlds = new LinkedHashSet<String>();
        for (String worldName : config.getStringList("warps.allowed-worlds.worlds", Collections.<String>emptyList())) {
            String normalized = normalize(worldName);
            if (!normalized.isEmpty()) {
                allowedWorlds.add(normalized);
            }
        }

        boolean defaultLocked = config.getBoolean("warps.default-locked", false);
        boolean defaultWhitelistEnabled = config.getBoolean("warps.default-whitelist-enabled", false);

        return new WarpSettings(namePattern, reservedNames, allowedWorldMode, allowedWorlds, defaultLocked, defaultWhitelistEnabled);
    }

    public Pattern namePattern() {
        return this.namePattern;
    }

    public Set<String> reservedNames() {
        return this.reservedNames;
    }

    public AllowedWorldMode allowedWorldMode() {
        return this.allowedWorldMode;
    }

    public Set<String> allowedWorlds() {
        return this.allowedWorlds;
    }

    public boolean defaultLocked() {
        return this.defaultLocked;
    }

    public boolean defaultWhitelistEnabled() {
        return this.defaultWhitelistEnabled;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }

    public enum AllowedWorldMode {
        BLACKLIST,
        WHITELIST
    }
}
