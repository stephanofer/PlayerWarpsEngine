package com.hera.playerwarps.config;

import dev.dejvokep.boostedyaml.YamlDocument;

public final class PlaceholderSettings {

    private final String fallback;
    private final String locked;
    private final String unlocked;

    private PlaceholderSettings(String fallback, String locked, String unlocked) {
        this.fallback = fallback;
        this.locked = locked;
        this.unlocked = unlocked;
    }

    public static PlaceholderSettings from(YamlDocument config) {
        return new PlaceholderSettings(
                config.getString("placeholders.fallback", "N/A"),
                config.getString("placeholders.locked", "yes"),
                config.getString("placeholders.unlocked", "no")
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
}
