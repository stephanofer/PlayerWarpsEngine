package com.hera.playerwarps.bootstrap;

import com.hera.playerwarps.PlayerWarpsPlugin;
import com.hera.playerwarps.command.CommandRegistrar;
import com.hera.playerwarps.command.PlayerWarpCommand;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.listener.PlayerDamageListener;
import com.hera.playerwarps.listener.PlayerDeathListener;
import com.hera.playerwarps.listener.PlayerMoveListener;
import com.hera.playerwarps.listener.PlayerQuitListener;
import com.hera.playerwarps.listener.ChatSearchListener;
import com.hera.playerwarps.listener.MenuCloseListener;
import com.hera.playerwarps.menu.MenuService;
import com.hera.playerwarps.placeholder.PlaceholderIntegration;
import com.hera.playerwarps.storage.Database;
import com.hera.playerwarps.storage.MysqlLimitBonusRepository;
import com.hera.playerwarps.storage.MysqlWarpRepository;
import com.hera.playerwarps.storage.MysqlWhitelistRepository;
import com.hera.playerwarps.storage.SchemaMigrator;
import com.hera.playerwarps.teleport.AsyncChunkPreloader;
import com.hera.playerwarps.teleport.ChunkPreloader;
import com.hera.playerwarps.teleport.SafetyChecker;
import com.hera.playerwarps.teleport.SyncChunkPreloader;
import com.hera.playerwarps.teleport.TeleportService;
import com.hera.playerwarps.warp.LimitBonusRepository;
import com.hera.playerwarps.warp.VisitBuffer;
import com.hera.playerwarps.warp.WarpCache;
import com.hera.playerwarps.warp.WarpAccessService;
import com.hera.playerwarps.warp.WarpLimitService;
import com.hera.playerwarps.warp.WarpMutationGuard;
import com.hera.playerwarps.warp.WarpPurgeService;
import com.hera.playerwarps.warp.WarpRepository;
import com.hera.playerwarps.warp.WarpRules;
import com.hera.playerwarps.warp.WarpService;
import com.hera.playerwarps.warp.WarpWarmupService;
import com.hera.playerwarps.warp.WhitelistRepository;
import org.bukkit.Bukkit;

public final class PluginBootstrap {

    private final PlayerWarpsPlugin plugin;

    private ConfigManager configManager;
    private PluginScheduler scheduler;
    private CommandRegistrar commandRegistrar;
    private PlayerWarpCommand playerWarpCommand;
    private Database database;
    private WarpCache warpCache;
    private Services services;
    private TeleportService teleportService;
    private VisitBuffer visitBuffer;
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
        WarpAccessService warpAccessService = new WarpAccessService(this.warpCache);
        WarpMutationGuard warpMutationGuard = new WarpMutationGuard();
        WarpWarmupService warpWarmupService = new WarpWarmupService(warpRepository, whitelistRepository, limitBonusRepository, this.warpCache);
        WarpWarmupService.WarmupResult warmupResult = warpWarmupService.warmup(this.configManager.settings().serverId());

        SafetyChecker safetyChecker = new SafetyChecker();
        ChunkPreloader syncChunkPreloader = new SyncChunkPreloader(this.scheduler);
        ChunkPreloader chunkPreloader = new AsyncChunkPreloader(syncChunkPreloader);
        this.visitBuffer = new VisitBuffer(this.plugin, this.scheduler, warpRepository, this.configManager.settings().serverId());
        this.teleportService = new TeleportService(this.configManager, this.scheduler, this.warpCache, warpAccessService, safetyChecker, chunkPreloader, this.visitBuffer);
        WarpService warpService = new WarpService(this.configManager, this.scheduler, warpRepository, whitelistRepository, limitBonusRepository,
                this.warpCache, warpRules, warpLimitService, warpMutationGuard, safetyChecker, this.teleportService);
        WarpPurgeService warpPurgeService = new WarpPurgeService(this.configManager, this.scheduler, warpRepository, this.warpCache, safetyChecker);
        MenuService menuService = new MenuService(this.plugin, this.configManager, this.scheduler, this.warpCache, warpAccessService,
                warpLimitService, this.visitBuffer, warpService);
        PlaceholderIntegration placeholderIntegration = new PlaceholderIntegration(this.plugin, this.configManager, this.warpCache, warpLimitService,
                this.visitBuffer);
        this.visitBuffer.start();

        this.commandRegistrar = new CommandRegistrar(this.plugin);
        this.services = new Services(this.configManager, this.scheduler, this.commandRegistrar, this.database, warpRepository, whitelistRepository,
                limitBonusRepository, this.warpCache, warpAccessService, warpRules, warpLimitService, warpWarmupService, safetyChecker,
                this.teleportService, this.visitBuffer, warpService, warpPurgeService, menuService, placeholderIntegration);
        this.playerWarpCommand = new PlayerWarpCommand(this.plugin, this.configManager, this.commandRegistrar, this.services);
        this.commandRegistrar.register(this.configManager.settings(), this.playerWarpCommand);

        menuService.enable();
        placeholderIntegration.enable();

        Bukkit.getPluginManager().registerEvents(new PlayerMoveListener(this.configManager, this.teleportService), this.plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerDamageListener(this.configManager, this.teleportService), this.plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerDeathListener(this.teleportService), this.plugin);
        Bukkit.getPluginManager().registerEvents(new PlayerQuitListener(this.teleportService, menuService), this.plugin);
        Bukkit.getPluginManager().registerEvents(new ChatSearchListener(this.scheduler, menuService), this.plugin);
        Bukkit.getPluginManager().registerEvents(new MenuCloseListener(menuService), this.plugin);

        this.enabled = true;

        this.plugin.getLogger().info("PlayerWarpsEngine enabled on server-id '" + this.configManager.settings().serverId()
                + "' with " + warmupResult.warps() + " warps, " + warmupResult.whitelistWarps()
                + " whitelist sets and " + warmupResult.bonuses() + " bonuses loaded.");
    }

    public void disable() {
        if (this.teleportService != null) {
            this.teleportService.cancelAll();
        }

        if (this.services != null && this.services.placeholderIntegration() != null) {
            this.services.placeholderIntegration().disable();
        }

        if (this.services != null && this.services.menuService() != null) {
            this.services.menuService().disable();
        }

        if (this.visitBuffer != null) {
            this.visitBuffer.stop();
            this.visitBuffer.flushBlocking();
        }

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
