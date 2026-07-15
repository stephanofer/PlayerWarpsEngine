package com.hera.playerwarps.config;

import dev.dejvokep.boostedyaml.YamlDocument;

public final class Configs {

    private final YamlDocument config;
    private final YamlDocument storage;
    private final YamlDocument messages;

    public Configs(YamlDocument config, YamlDocument storage, YamlDocument messages) {
        this.config = config;
        this.storage = storage;
        this.messages = messages;
    }

    public YamlDocument config() {
        return this.config;
    }

    public YamlDocument storage() {
        return this.storage;
    }

    public YamlDocument messages() {
        return this.messages;
    }
}
