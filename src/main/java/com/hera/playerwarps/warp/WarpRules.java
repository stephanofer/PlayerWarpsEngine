package com.hera.playerwarps.warp;

import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.config.WarpSettings;

import java.util.Locale;

public final class WarpRules {

    private final ConfigManager configManager;

    public WarpRules(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public WarpName validateName(String input) {
        WarpName name = WarpName.of(input);
        WarpSettings settings = this.configManager.settings().warpSettings();

        if (!settings.namePattern().matcher(name.value()).matches()) {
            throw new IllegalArgumentException("Warp name does not match configured regex");
        }
        if (settings.reservedNames().contains(name.normalized())) {
            throw new IllegalArgumentException("Warp name is reserved");
        }

        return name;
    }

    public boolean isWorldAllowed(String worldName) {
        WarpSettings settings = this.configManager.settings().warpSettings();
        String normalized = worldName == null ? "" : worldName.trim().toLowerCase(Locale.ENGLISH);
        boolean listed = settings.allowedWorlds().contains(normalized);

        if (settings.allowedWorldMode() == WarpSettings.AllowedWorldMode.WHITELIST) {
            return listed;
        }

        return !listed;
    }

    public void validateWorld(String worldName) {
        if (!isWorldAllowed(worldName)) {
            throw new IllegalArgumentException("World is not allowed for player warps");
        }
    }

    public boolean defaultLocked() {
        return this.configManager.settings().warpSettings().defaultLocked();
    }

    public boolean defaultWhitelistEnabled() {
        return this.configManager.settings().warpSettings().defaultWhitelistEnabled();
    }
}
