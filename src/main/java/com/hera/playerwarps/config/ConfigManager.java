package com.hera.playerwarps.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import com.hera.playerwarps.storage.DatabaseSettings;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public final class ConfigManager {

    private final Plugin plugin;
    private Configs configs;
    private Settings settings;
    private DatabaseSettings databaseSettings;
    private Messages messages;

    public ConfigManager(Plugin plugin) {
        this.plugin = plugin;
    }

    public void loadInitial() throws IOException {
        apply(load());
    }

    public LoadedConfig load() throws IOException {
        Configs loadedConfigs = new Configs(
                loadDocument("config.yml"),
                loadDocument("storage.yml"),
                loadDocument("messages.yml")
        );

        Settings loadedSettings = Settings.from(loadedConfigs.config());
        DatabaseSettings loadedDatabaseSettings = DatabaseSettings.from(loadedConfigs.storage());
        Messages loadedMessages = Messages.from(loadedConfigs.messages());

        return new LoadedConfig(loadedConfigs, loadedSettings, loadedDatabaseSettings, loadedMessages);
    }

    public void apply(LoadedConfig loadedConfig) {
        this.configs = loadedConfig.configs();
        this.settings = loadedConfig.settings();
        this.databaseSettings = loadedConfig.databaseSettings();
        this.messages = loadedConfig.messages();
    }

    public Configs configs() {
        return this.configs;
    }

    public Settings settings() {
        return this.settings;
    }

    public DatabaseSettings databaseSettings() {
        return this.databaseSettings;
    }

    public Messages messages() {
        return this.messages;
    }

    private YamlDocument loadDocument(String resourceName) throws IOException {
        File file = new File(this.plugin.getDataFolder(), resourceName);
        InputStream defaults = this.plugin.getResource(resourceName);

        if (defaults == null) {
            throw new IOException("Missing default resource " + resourceName);
        }

        try (InputStream defaultsStream = defaults) {
            return YamlDocument.create(
                    file,
                    defaultsStream,
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder()
                            .setCreateFileIfAbsent(true)
                            .setAutoUpdate(true)
                            .build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder()
                            .setKeepAll(true)
                            .build()
            );
        }
    }

    public static final class LoadedConfig {

        private final Configs configs;
        private final Settings settings;
        private final DatabaseSettings databaseSettings;
        private final Messages messages;

        private LoadedConfig(Configs configs, Settings settings, DatabaseSettings databaseSettings, Messages messages) {
            this.configs = configs;
            this.settings = settings;
            this.databaseSettings = databaseSettings;
            this.messages = messages;
        }

        public Configs configs() {
            return this.configs;
        }

        public Settings settings() {
            return this.settings;
        }

        public DatabaseSettings databaseSettings() {
            return this.databaseSettings;
        }

        public Messages messages() {
            return this.messages;
        }
    }
}
