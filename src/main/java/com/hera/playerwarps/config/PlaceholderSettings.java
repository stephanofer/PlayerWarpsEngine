package com.hera.playerwarps.config;

import dev.dejvokep.boostedyaml.YamlDocument;

public final class PlaceholderSettings {

    private final String fallback;
    private final String locked;
    private final String unlocked;
    private final String accessAllowed;
    private final String accessLocked;
    private final String accessWhitelist;
    private final String accessAllowedAction;
    private final String accessDeniedAction;
    private final String menuLocked;
    private final String menuUnlocked;
    private final String menuWhitelistEnabled;
    private final String menuWhitelistDisabled;

    private PlaceholderSettings(String fallback, String locked, String unlocked, String accessAllowed, String accessLocked,
                                String accessWhitelist, String accessAllowedAction, String accessDeniedAction, String menuLocked,
                                String menuUnlocked, String menuWhitelistEnabled, String menuWhitelistDisabled) {
        this.fallback = fallback;
        this.locked = locked;
        this.unlocked = unlocked;
        this.accessAllowed = accessAllowed;
        this.accessLocked = accessLocked;
        this.accessWhitelist = accessWhitelist;
        this.accessAllowedAction = accessAllowedAction;
        this.accessDeniedAction = accessDeniedAction;
        this.menuLocked = menuLocked;
        this.menuUnlocked = menuUnlocked;
        this.menuWhitelistEnabled = menuWhitelistEnabled;
        this.menuWhitelistDisabled = menuWhitelistDisabled;
    }

    public static PlaceholderSettings from(YamlDocument config) {
        return new PlaceholderSettings(
                config.getString("placeholders.fallback", "N/A"),
                config.getString("placeholders.locked", "yes"),
                config.getString("placeholders.unlocked", "no"),
                config.getString("placeholders.access.allowed", "&aON"),
                config.getString("placeholders.access.locked", "&cBloqueado"),
                config.getString("placeholders.access.whitelist", "&cSin acceso"),
                config.getString("placeholders.access.allowed-action", "&eClic izquierdo &7para teletransportarte."),
                config.getString("placeholders.access.denied-action", "&cNo podés teletransportarte a este warp."),
                config.getString("placeholders.menu.locked", "&cBloqueado"),
                config.getString("placeholders.menu.unlocked", "&aDesbloqueado"),
                config.getString("placeholders.menu.whitelist-enabled", "&aON"),
                config.getString("placeholders.menu.whitelist-disabled", "&cOFF")
        );
    }

    public String fallback() {
        return this.fallback;
    }

    public String locked() {
        return this.locked;
    }

    public String unlocked() {
        return this.unlocked;
    }

    public String accessAllowed() {
        return this.accessAllowed;
    }

    public String accessLocked() {
        return this.accessLocked;
    }

    public String accessWhitelist() {
        return this.accessWhitelist;
    }

    public String accessAllowedAction() {
        return this.accessAllowedAction;
    }

    public String accessDeniedAction() {
        return this.accessDeniedAction;
    }

    public String menuLocked() {
        return this.menuLocked;
    }

    public String menuUnlocked() {
        return this.menuUnlocked;
    }

    public String menuWhitelistEnabled() {
        return this.menuWhitelistEnabled;
    }

    public String menuWhitelistDisabled() {
        return this.menuWhitelistDisabled;
    }
}
