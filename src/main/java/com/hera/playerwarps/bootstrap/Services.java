package com.hera.playerwarps.bootstrap;

import com.hera.playerwarps.command.CommandRegistrar;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.storage.Database;
import com.hera.playerwarps.warp.LimitBonusRepository;
import com.hera.playerwarps.warp.WarpCache;
import com.hera.playerwarps.warp.WarpLimitService;
import com.hera.playerwarps.warp.WarpRepository;
import com.hera.playerwarps.warp.WarpRules;
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
    private final WarpRules warpRules;
    private final WarpLimitService warpLimitService;
    private final WarpWarmupService warpWarmupService;

    public Services(ConfigManager configManager, PluginScheduler scheduler, CommandRegistrar commandRegistrar, Database database,
                    WarpRepository warpRepository, WhitelistRepository whitelistRepository, LimitBonusRepository limitBonusRepository,
                    WarpCache warpCache, WarpRules warpRules, WarpLimitService warpLimitService, WarpWarmupService warpWarmupService) {
        this.configManager = configManager;
        this.scheduler = scheduler;
        this.commandRegistrar = commandRegistrar;
        this.database = database;
        this.warpRepository = warpRepository;
        this.whitelistRepository = whitelistRepository;
        this.limitBonusRepository = limitBonusRepository;
        this.warpCache = warpCache;
        this.warpRules = warpRules;
        this.warpLimitService = warpLimitService;
        this.warpWarmupService = warpWarmupService;
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

    public WarpRules warpRules() {
        return this.warpRules;
    }

    public WarpLimitService warpLimitService() {
        return this.warpLimitService;
    }

    public WarpWarmupService warpWarmupService() {
        return this.warpWarmupService;
    }
}
