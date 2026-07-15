package com.hera.playerwarps.bootstrap;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.command.CommandRegistrar;
import com.hera.playerwarps.command.PlayerWarpCommand;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.storage.Database;
import com.hera.playerwarps.storage.MysqlLimitBonusRepository;
import com.hera.playerwarps.storage.MysqlWarpRepository;
import com.hera.playerwarps.storage.MysqlWhitelistRepository;
import com.hera.playerwarps.storage.SchemaMigrator;
import com.hera.playerwarps.warp.LimitBonusRepository;
import com.hera.playerwarps.warp.WarpCache;
import com.hera.playerwarps.warp.WarpLimitService;
import com.hera.playerwarps.warp.WarpRepository;
import com.hera.playerwarps.warp.WarpRules;
import com.hera.playerwarps.warp.WarpWarmupService;
import com.hera.playerwarps.warp.WhitelistRepository;

public final class PluginBootstrap {

    private final PlayerWarpsPlugin plugin;

    private ConfigManager configManager;
    private PluginScheduler scheduler;
    private CommandRegistrar commandRegistrar;
    private PlayerWarpCommand playerWarpCommand;
    private Database database;
    private WarpCache warpCache;
    private Services services;
    private boolean enabled;

    public PluginBootstrap(PlayerWarpsPlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() throws Exception {
        this.plugin.getDataFolder().mkdirs();

        this.configManager = new ConfigManager(this.plugin);
        this.configManager.loadInitial();

        this.scheduler = new PluginScheduler(this.plugin);

        this.database = new Database(this.configManager.databaseSettings());
        this.database.connect();
        new SchemaMigrator(this.database).migrate();

        WarpRepository warpRepository = new MysqlWarpRepository(this.database);
        WhitelistRepository whitelistRepository = new MysqlWhitelistRepository(this.database);
        LimitBonusRepository limitBonusRepository = new MysqlLimitBonusRepository(this.database);
        this.warpCache = new WarpCache();
        WarpRules warpRules = new WarpRules(this.configManager);
        WarpLimitService warpLimitService = new WarpLimitService(this.configManager, this.warpCache);
        WarpWarmupService warpWarmupService = new WarpWarmupService(warpRepository, whitelistRepository, limitBonusRepository, this.warpCache);
        WarpWarmupService.WarmupResult warmupResult = warpWarmupService.warmup(this.configManager.settings().serverId());

        this.commandRegistrar = new CommandRegistrar(this.plugin);
        this.services = new Services(this.configManager, this.scheduler, this.commandRegistrar, this.database, warpRepository, whitelistRepository,
                limitBonusRepository, this.warpCache, warpRules, warpLimitService, warpWarmupService);
        this.playerWarpCommand = new PlayerWarpCommand(this.plugin, this.configManager, this.commandRegistrar, this.services);
        this.commandRegistrar.register(this.configManager.settings(), this.playerWarpCommand);

        this.enabled = true;

        this.plugin.getLogger().info("PlayerWarpsEngine enabled on server-id '" + this.configManager.settings().serverId()
                + "' with " + warmupResult.warps() + " warps, " + warmupResult.whitelistWarps()
                + " whitelist sets and " + warmupResult.bonuses() + " bonuses loaded.");
    }

    public void disable() {
        if (this.commandRegistrar != null) {
            this.commandRegistrar.unregister();
        }

        if (this.scheduler != null) {
            this.scheduler.cancelAll();
        }

        if (this.warpCache != null) {
            this.warpCache.clear();
        }

        if (this.database != null) {
            this.database.close();
        }

        this.enabled = false;
        this.services = null;

        this.plugin.getLogger().info("PlayerWarpsEngine disabled.");
    }

    public boolean enabled() {
        return this.enabled;
    }

    public Services services() {
        return this.services;
    }
}
