package com.hera.playerwarps.bootstrap;

import com.hera.playerwarps.command.CommandRegistrar;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.menu.MenuService;
import com.hera.playerwarps.placeholder.PlaceholderIntegration;
import com.hera.playerwarps.storage.Database;
import com.hera.playerwarps.teleport.SafetyChecker;
import com.hera.playerwarps.teleport.TeleportService;
import com.hera.playerwarps.warp.LimitBonusRepository;
import com.hera.playerwarps.warp.VisitBuffer;
import com.hera.playerwarps.warp.WarpCache;
import com.hera.playerwarps.warp.WarpAccessService;
import com.hera.playerwarps.warp.WarpLimitService;
import com.hera.playerwarps.warp.WarpPurgeService;
import com.hera.playerwarps.warp.WarpRepository;
import com.hera.playerwarps.warp.WarpRules;
import com.hera.playerwarps.warp.WarpService;
import com.hera.playerwarps.warp.WarpWarmupService;
import com.hera.playerwarps.warp.WhitelistRepository;

public final class Services {

    private final ConfigManager configManager;
    private final PluginScheduler scheduler;
    private final CommandRegistrar commandRegistrar;
    private final Database database;
    private final WarpRepository warpRepository;
    private final WhitelistRepository whitelistRepository;
    private final LimitBonusRepository limitBonusRepository;
    private final WarpCache warpCache;
    private final WarpAccessService warpAccessService;
    private final WarpRules warpRules;
    private final WarpLimitService warpLimitService;
    private final WarpWarmupService warpWarmupService;
    private final SafetyChecker safetyChecker;
    private final TeleportService teleportService;
    private final VisitBuffer visitBuffer;
    private final WarpService warpService;
    private final WarpPurgeService warpPurgeService;
    private final MenuService menuService;
    private final PlaceholderIntegration placeholderIntegration;

    public Services(ConfigManager configManager, PluginScheduler scheduler, CommandRegistrar commandRegistrar, Database database,
                    WarpRepository warpRepository, WhitelistRepository whitelistRepository, LimitBonusRepository limitBonusRepository,
                    WarpCache warpCache, WarpAccessService warpAccessService, WarpRules warpRules, WarpLimitService warpLimitService, WarpWarmupService warpWarmupService,
                    SafetyChecker safetyChecker, TeleportService teleportService, VisitBuffer visitBuffer, WarpService warpService,
                    WarpPurgeService warpPurgeService, MenuService menuService, PlaceholderIntegration placeholderIntegration) {
        this.configManager = configManager;
        this.scheduler = scheduler;
        this.commandRegistrar = commandRegistrar;
        this.database = database;
        this.warpRepository = warpRepository;
        this.whitelistRepository = whitelistRepository;
        this.limitBonusRepository = limitBonusRepository;
        this.warpCache = warpCache;
        this.warpAccessService = warpAccessService;
        this.warpRules = warpRules;
        this.warpLimitService = warpLimitService;
        this.warpWarmupService = warpWarmupService;
        this.safetyChecker = safetyChecker;
        this.teleportService = teleportService;
        this.visitBuffer = visitBuffer;
        this.warpService = warpService;
        this.warpPurgeService = warpPurgeService;
        this.menuService = menuService;
        this.placeholderIntegration = placeholderIntegration;
    }

    public ConfigManager configManager() {
        return this.configManager;
    }

    public PluginScheduler scheduler() {
        return this.scheduler;
    }

    public CommandRegistrar commandRegistrar() {
        return this.commandRegistrar;
    }

    public Database database() {
        return this.database;
    }

    public WarpRepository warpRepository() {
        return this.warpRepository;
    }

    public WhitelistRepository whitelistRepository() {
        return this.whitelistRepository;
    }

    public LimitBonusRepository limitBonusRepository() {
        return this.limitBonusRepository;
    }

    public WarpCache warpCache() {
        return this.warpCache;
    }

    public WarpAccessService warpAccessService() {
        return this.warpAccessService;
    }

    public WarpRules warpRules() {
        return this.warpRules;
    }

    public WarpLimitService warpLimitService() {
        return this.warpLimitService;
    }

    public WarpWarmupService warpWarmupService() {
        return this.warpWarmupService;
    }

    public SafetyChecker safetyChecker() {
        return this.safetyChecker;
    }

    public TeleportService teleportService() {
        return this.teleportService;
    }

    public VisitBuffer visitBuffer() {
        return this.visitBuffer;
    }

    public WarpService warpService() {
        return this.warpService;
    }

    public WarpPurgeService warpPurgeService() {
        return this.warpPurgeService;
    }

    public MenuService menuService() {
        return this.menuService;
    }

    public PlaceholderIntegration placeholderIntegration() {
        return this.placeholderIntegration;
    }
}
