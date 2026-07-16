package com.hera.playerwarps.warp;

import com.hera.playerwarps.bootstrap.PluginScheduler;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.teleport.SafetyChecker;
import com.hera.playerwarps.teleport.SafetyResult;
import com.hera.playerwarps.teleport.TeleportService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class WarpService {

    private static final String ADMIN_MANAGE_PERMISSION = "pwarp.admin.manage";

    private final ConfigManager configManager;
    private final PluginScheduler scheduler;
    private final WarpRepository warpRepository;
    private final WhitelistRepository whitelistRepository;
    private final LimitBonusRepository limitBonusRepository;
    private final WarpCache warpCache;
    private final WarpRules warpRules;
    private final WarpLimitService warpLimitService;
    private final WarpMutationGuard mutationGuard;
    private final SafetyChecker safetyChecker;
    private final TeleportService teleportService;

    public WarpService(ConfigManager configManager, PluginScheduler scheduler, WarpRepository warpRepository, WhitelistRepository whitelistRepository,
                        LimitBonusRepository limitBonusRepository, WarpCache warpCache, WarpRules warpRules, WarpLimitService warpLimitService,
                        WarpMutationGuard mutationGuard, SafetyChecker safetyChecker, TeleportService teleportService) {
        this.configManager = configManager;
        this.scheduler = scheduler;
        this.warpRepository = warpRepository;
        this.whitelistRepository = whitelistRepository;
        this.limitBonusRepository = limitBonusRepository;
        this.warpCache = warpCache;
        this.warpRules = warpRules;
        this.warpLimitService = warpLimitService;
        this.mutationGuard = mutationGuard;
        this.safetyChecker = safetyChecker;
        this.teleportService = teleportService;
    }

    public void createWarp(final Player player, String rawName) {
        final WarpName name;
        try {
            name = this.warpRules.validateName(rawName);
        } catch (IllegalArgumentException exception) {
            this.configManager.messages().send(player, exception.getMessage() != null && exception.getMessage().contains("reserved")
                    ? "messages.reserved-warp-name" : "messages.invalid-warp-name");
            return;
        }

        if (this.warpCache.getByName(name.normalized()).isPresent()) {
            sendWarpMessage(player, "messages.warp-already-exists", name.value());
            return;
        }

        int limit = this.warpLimitService.cached(player);
        if (this.warpCache.countByOwner(player.getUniqueId()) >= limit) {
            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("limit", String.valueOf(limit));
            this.configManager.messages().send(player, "messages.warp-limit-reached", placeholders);
            return;
        }

        Location location = player.getLocation();
        try {
            this.warpRules.validateWorld(location.getWorld().getName());
        } catch (IllegalArgumentException exception) {
            this.configManager.messages().send(player, "messages.world-not-allowed");
            return;
        }

        SafetyResult safety = this.safetyChecker.check(location);
        if (!safety.isSafe()) {
            this.configManager.messages().send(player, safety.messageKey());
            return;
        }

        final WarpCreateRequest request = new WarpCreateRequest(
                this.configManager.settings().serverId(),
                name,
                player.getUniqueId(),
                player.getName(),
                new WarpLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()),
                this.warpRules.defaultLocked(),
                this.warpRules.defaultWhitelistEnabled(),
                System.currentTimeMillis()
        );

        this.scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final Warp created = warpRepository.insert(request);
                    scheduler.runSync(new Runnable() {
                        @Override
                        public void run() {
                            warpCache.putWarp(created);
                            if (player.isOnline()) {
                                sendWarpMessage(player, "messages.warp-created", created.name());
                            }
                        }
                    });
                } catch (final SQLException exception) {
                    scheduler.runSync(new Runnable() {
                        @Override
                        public void run() {
                            if (!player.isOnline()) {
                                return;
                            }
                            if (isDuplicateName(exception)) {
                                sendWarpMessage(player, "messages.warp-already-exists", name.value());
                            } else {
                                configManager.messages().send(player, "messages.storage-error");
                            }
                        }
                    });
                }
            }
        });
    }

    public void removeWarp(final CommandSender sender, String rawName, String ownerGuard) {
        final Warp warp = findWarpOrNotify(sender, rawName);
        if (warp == null) {
            return;
        }
        if (!canManage(sender, warp, "pwarp.remove", "pwarp.admin.remove")) {
            return;
        }
        if (ownerGuard != null && !ownerGuard.trim().isEmpty() && !warp.ownerName().equalsIgnoreCase(ownerGuard.trim())) {
            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("warp", warp.name());
            placeholders.put("owner", warp.ownerName());
            this.configManager.messages().send(sender, "messages.owner-guard-mismatch", placeholders);
            return;
        }
        if (!this.mutationGuard.tryStart(warp.id())) {
            this.configManager.messages().send(sender, "messages.operation-in-progress");
            return;
        }

        this.scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final boolean removed = warpRepository.deleteById(configManager.settings().serverId(), warp.id());
                    scheduler.runSync(new Runnable() {
                        @Override
                        public void run() {
                            mutationGuard.finish(warp.id());
                            if (removed) {
                                warpCache.removeWarp(warp);
                                sendWarpMessage(sender, "messages.warp-removed", warp.name());
                            } else {
                                sendWarpMessage(sender, "messages.warp-not-found", warp.name());
                            }
                        }
                    });
                } catch (SQLException exception) {
                    mutationGuard.finish(warp.id());
                    sendStorageError(sender);
                }
            }
        });
    }

    public void removeAll(final CommandSender sender, String ownerName) {
        final List<Warp> warps = findWarpsByOwnerName(ownerName);
        if (warps.isEmpty()) {
            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("player", ownerName);
            this.configManager.messages().send(sender, "messages.removeall-empty", placeholders);
            return;
        }

        final List<Long> ids = warpIds(warps);
        if (!this.mutationGuard.tryStart(ids)) {
            this.configManager.messages().send(sender, "messages.operation-in-progress");
            return;
        }
        this.scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final int removed = warpRepository.deleteByIds(configManager.settings().serverId(), ids);
                    scheduler.runSync(new Runnable() {
                        @Override
                        public void run() {
                            mutationGuard.finish(ids);
                            warpCache.removeWarps(warps);
                            Map<String, String> placeholders = new HashMap<String, String>();
                            placeholders.put("player", ownerName);
                            placeholders.put("amount", String.valueOf(removed));
                            configManager.messages().send(sender, "messages.removeall-success", placeholders);
                        }
                    });
                } catch (SQLException exception) {
                    mutationGuard.finish(ids);
                    sendStorageError(sender);
                }
            }
        });
    }

    public void setLocked(final CommandSender sender, String rawName, final boolean locked) {
        final Warp warp = findWarpOrNotify(sender, rawName);
        if (warp == null || !canManage(sender, warp, "pwarp.lock", ADMIN_MANAGE_PERMISSION)) {
            return;
        }
        updateWarp(sender, warp, locked ? "messages.warp-locked" : "messages.warp-unlocked", new WarpUpdater() {
            @Override
            public boolean persist(Warp oldWarp, long now) throws SQLException {
                return warpRepository.updateLocked(configManager.settings().serverId(), oldWarp.id(), locked, now);
            }

            @Override
            public Warp update(Warp oldWarp, long now) {
                return oldWarp.withLocked(locked, now);
            }
        });
    }

    public void resetLocation(final Player player, String rawName) {
        final Warp warp = findWarpOrNotify(player, rawName);
        if (warp == null || !canManage(player, warp, "pwarp.reset", ADMIN_MANAGE_PERMISSION)) {
            return;
        }
        Location location = player.getLocation();
        try {
            this.warpRules.validateWorld(location.getWorld().getName());
        } catch (IllegalArgumentException exception) {
            this.configManager.messages().send(player, "messages.world-not-allowed");
            return;
        }
        SafetyResult safety = this.safetyChecker.check(location);
        if (!safety.isSafe()) {
            this.configManager.messages().send(player, safety.messageKey());
            return;
        }
        final WarpLocation warpLocation = new WarpLocation(location.getWorld().getName(), location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch());
        updateWarp(player, warp, "messages.warp-reset", new WarpUpdater() {
            @Override
            public boolean persist(Warp oldWarp, long now) throws SQLException {
                return warpRepository.updateLocation(configManager.settings().serverId(), oldWarp.id(), warpLocation, SafeStatus.SAFE, now);
            }

            @Override
            public Warp update(Warp oldWarp, long now) {
                return oldWarp.withLocation(warpLocation, SafeStatus.SAFE, now);
            }
        });
    }

    public void renameWarp(final CommandSender sender, String rawName, String newRawName) {
        final Warp warp = findWarpOrNotify(sender, rawName);
        if (warp == null || !canManage(sender, warp, "pwarp.rename", ADMIN_MANAGE_PERMISSION)) {
            return;
        }
        final WarpName name;
        try {
            name = this.warpRules.validateName(newRawName);
        } catch (IllegalArgumentException exception) {
            this.configManager.messages().send(sender, exception.getMessage() != null && exception.getMessage().contains("reserved")
                    ? "messages.reserved-warp-name" : "messages.invalid-warp-name");
            return;
        }
        if (warp.nameNormalized().equals(name.normalized())) {
            sendWarpMessage(sender, "messages.warp-renamed", warp.name());
            return;
        }
        if (this.warpCache.getByName(name.normalized()).isPresent()) {
            sendWarpMessage(sender, "messages.warp-already-exists", name.value());
            return;
        }
        updateWarp(sender, warp, "messages.warp-renamed", new WarpUpdater() {
            @Override
            public boolean persist(Warp oldWarp, long now) throws SQLException {
                return warpRepository.updateName(configManager.settings().serverId(), oldWarp.id(), name, now);
            }

            @Override
            public Warp update(Warp oldWarp, long now) {
                return oldWarp.withName(name, now);
            }
        });
    }

    public void transferOwner(final CommandSender sender, String rawName, String targetName) {
        final Warp warp = findWarpOrNotify(sender, rawName);
        if (warp == null || !canManage(sender, warp, "pwarp.setowner", "pwarp.admin.setowner")) {
            return;
        }
        final Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sendPlayerNotOnline(sender, targetName);
            return;
        }
        if (warp.ownerUuid().equals(target.getUniqueId())) {
            this.configManager.messages().send(sender, "messages.owner-already-target");
            return;
        }
        int limit = this.warpLimitService.cached(target);
        int currentAmount = this.warpCache.countByOwner(target.getUniqueId());
        if (currentAmount >= limit && !sender.hasPermission("pwarp.admin.limit.bypass")) {
            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("player", target.getName());
            placeholders.put("limit", String.valueOf(limit));
            this.configManager.messages().send(sender, "messages.target-limit-reached", placeholders);
            return;
        }
        updateWarp(sender, warp, "messages.owner-transferred", new WarpUpdater() {
            @Override
            public boolean persist(Warp oldWarp, long now) throws SQLException {
                return warpRepository.updateOwner(configManager.settings().serverId(), oldWarp.id(), target.getUniqueId(), target.getName(), now);
            }

            @Override
            public Warp update(Warp oldWarp, long now) {
                warpLimitService.invalidateAll();
                return oldWarp.withOwner(target.getUniqueId(), target.getName(), now);
            }
        });
    }

    public void setWhitelistEnabled(final CommandSender sender, String rawName, final boolean enabled) {
        final Warp warp = findWarpOrNotify(sender, rawName);
        if (warp == null || !canManage(sender, warp, "pwarp.whitelist", ADMIN_MANAGE_PERMISSION)) {
            return;
        }
        updateWarp(sender, warp, enabled ? "messages.whitelist-enabled" : "messages.whitelist-disabled", new WarpUpdater() {
            @Override
            public boolean persist(Warp oldWarp, long now) throws SQLException {
                return warpRepository.updateWhitelistEnabled(configManager.settings().serverId(), oldWarp.id(), enabled, now);
            }

            @Override
            public Warp update(Warp oldWarp, long now) {
                return oldWarp.withWhitelistEnabled(enabled, now);
            }
        });
    }

    public void addWhitelist(final CommandSender sender, String rawName, String targetName) {
        final Warp warp = findWarpOrNotify(sender, rawName);
        if (warp == null || !canManage(sender, warp, "pwarp.whitelist", ADMIN_MANAGE_PERMISSION)) {
            return;
        }
        final Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sendPlayerNotOnline(sender, targetName);
            return;
        }
        if (warp.ownerUuid().equals(target.getUniqueId())) {
            this.configManager.messages().send(sender, "messages.whitelist-owner");
            return;
        }
        if (this.warpCache.isWhitelisted(warp.id(), target.getUniqueId())) {
            this.configManager.messages().send(sender, "messages.whitelist-already-added");
            return;
        }
        if (!this.mutationGuard.tryStart(warp.id())) {
            this.configManager.messages().send(sender, "messages.operation-in-progress");
            return;
        }
        this.scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final boolean added = whitelistRepository.add(warp.id(), target.getUniqueId(), target.getName(), System.currentTimeMillis());
                    scheduler.runSync(new Runnable() {
                        @Override
                        public void run() {
                            mutationGuard.finish(warp.id());
                            if (added) {
                                warpCache.addWhitelist(warp.id(), target.getUniqueId(), target.getName());
                            }
                            Map<String, String> placeholders = warpPlaceholders(warp.name());
                            placeholders.put("player", target.getName());
                            configManager.messages().send(sender, added ? "messages.whitelist-added" : "messages.whitelist-already-added", placeholders);
                        }
                    });
                } catch (SQLException exception) {
                    mutationGuard.finish(warp.id());
                    sendStorageError(sender);
                }
            }
        });
    }

    public void removeWhitelist(final CommandSender sender, String rawName, String targetName) {
        final Warp warp = findWarpOrNotify(sender, rawName);
        if (warp == null || !canManage(sender, warp, "pwarp.whitelist", ADMIN_MANAGE_PERMISSION)) {
            return;
        }
        final UUID targetUuid = findWhitelistedUuid(warp.id(), targetName);
        if (targetUuid == null) {
            Map<String, String> placeholders = new HashMap<String, String>();
            placeholders.put("player", targetName);
            this.configManager.messages().send(sender, "messages.whitelist-not-found", placeholders);
            return;
        }
        if (!this.mutationGuard.tryStart(warp.id())) {
            this.configManager.messages().send(sender, "messages.operation-in-progress");
            return;
        }
        this.scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final boolean removed = whitelistRepository.remove(warp.id(), targetUuid);
                    scheduler.runSync(new Runnable() {
                        @Override
                        public void run() {
                            mutationGuard.finish(warp.id());
                            if (removed) {
                                warpCache.removeWhitelist(warp.id(), targetUuid);
                            }
                            Map<String, String> placeholders = warpPlaceholders(warp.name());
                            placeholders.put("player", targetName);
                            configManager.messages().send(sender, removed ? "messages.whitelist-removed" : "messages.whitelist-not-found", placeholders);
                        }
                    });
                } catch (SQLException exception) {
                    mutationGuard.finish(warp.id());
                    sendStorageError(sender);
                }
            }
        });
    }

    public void listWhitelist(CommandSender sender, String rawName) {
        Warp warp = findWarpOrNotify(sender, rawName);
        if (warp == null || !canManage(sender, warp, "pwarp.whitelist", ADMIN_MANAGE_PERMISSION)) {
            return;
        }
        Map<UUID, String> members = this.warpCache.whitelistMemberNames(warp.id());
        if (members.isEmpty()) {
            sendWarpMessage(sender, "messages.whitelist-empty", warp.name());
            return;
        }
        Map<String, String> placeholders = warpPlaceholders(warp.name());
        placeholders.put("amount", String.valueOf(members.size()));
        this.configManager.messages().send(sender, "messages.whitelist-list-header", placeholders);
        for (Map.Entry<UUID, String> entry : members.entrySet()) {
            Map<String, String> line = new HashMap<String, String>();
            line.put("player", entry.getValue());
            line.put("uuid", entry.getKey().toString());
            this.configManager.messages().send(sender, "messages.whitelist-list-line", line);
        }
    }

    public void addWarps(final CommandSender sender, String targetName, String amountText) {
        final Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            sendPlayerNotOnline(sender, targetName);
            return;
        }
        final int delta;
        try {
            delta = Integer.parseInt(amountText);
        } catch (NumberFormatException exception) {
            this.configManager.messages().send(sender, "messages.invalid-amount");
            return;
        }
        if (delta == 0) {
            this.configManager.messages().send(sender, "messages.invalid-amount");
            return;
        }
        int current = this.warpCache.bonusOf(target.getUniqueId());
        long calculated = (long) current + delta;
        if (calculated < 0L || calculated > Integer.MAX_VALUE) {
            this.configManager.messages().send(sender, "messages.invalid-amount");
            return;
        }
        final int newBonus = (int) calculated;
        this.scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    limitBonusRepository.setBonus(configManager.settings().serverId(), target.getUniqueId(), newBonus);
                    scheduler.runSync(new Runnable() {
                        @Override
                        public void run() {
                            warpCache.setBonus(target.getUniqueId(), newBonus);
                            warpLimitService.invalidateAll();
                            Map<String, String> placeholders = new HashMap<String, String>();
                            placeholders.put("player", target.getName());
                            placeholders.put("bonus", String.valueOf(newBonus));
                            placeholders.put("delta", String.valueOf(delta));
                            configManager.messages().send(sender, "messages.bonus-updated", placeholders);
                        }
                    });
                } catch (SQLException exception) {
                    sendStorageError(sender);
                }
            }
        });
    }

    public void teleport(Player player, String rawName) {
        Warp warp = this.warpCache.getByName(WarpName.normalize(rawName)).orElse(null);
        if (warp == null) {
            sendWarpMessage(player, "messages.warp-not-found", rawName);
            return;
        }
        this.teleportService.teleport(player, warp);
    }

    private void updateWarp(final CommandSender sender, final Warp warp, final String successMessage, final WarpUpdater updater) {
        if (!this.mutationGuard.tryStart(warp.id())) {
            this.configManager.messages().send(sender, "messages.operation-in-progress");
            return;
        }
        this.scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final long now = System.currentTimeMillis();
                    final boolean updated = updater.persist(warp, now);
                    scheduler.runSync(new Runnable() {
                        @Override
                        public void run() {
                            mutationGuard.finish(warp.id());
                            if (!updated) {
                                sendWarpMessage(sender, "messages.warp-not-found", warp.name());
                                return;
                            }
                            Warp newWarp = updater.update(warp, now);
                            warpCache.replaceWarp(warp, newWarp);
                            sendWarpMessage(sender, successMessage, newWarp.name());
                        }
                    });
                } catch (final SQLException exception) {
                    scheduler.runSync(new Runnable() {
                        @Override
                        public void run() {
                            mutationGuard.finish(warp.id());
                            if (isDuplicateName(exception)) {
                                sendWarpMessage(sender, "messages.warp-already-exists", "");
                            } else {
                                configManager.messages().send(sender, "messages.storage-error");
                            }
                        }
                    });
                }
            }
        });
    }

    private Warp findWarpOrNotify(CommandSender sender, String rawName) {
        Warp warp = this.warpCache.getByName(WarpName.normalize(rawName)).orElse(null);
        if (warp == null) {
            sendWarpMessage(sender, "messages.warp-not-found", rawName);
        }
        return warp;
    }

    private boolean canManage(CommandSender sender, Warp warp, String ownerPermission, String adminPermission) {
        boolean isOwner = sender instanceof Player && warp.ownerUuid().equals(((Player) sender).getUniqueId());
        if (isOwner) {
            if (ownerPermission == null || ownerPermission.isEmpty() || sender.hasPermission(ownerPermission)) {
                return true;
            }
            this.configManager.messages().send(sender, "messages.no-permission");
            return false;
        }
        if (adminPermission != null && !adminPermission.isEmpty() && sender.hasPermission(adminPermission)) {
            return true;
        }
        this.configManager.messages().send(sender, sender instanceof Player ? "messages.not-warp-owner" : "messages.no-permission");
        return false;
    }

    private List<Warp> findWarpsByOwnerName(String ownerName) {
        String normalized = ownerName == null ? "" : ownerName.trim().toLowerCase(Locale.ENGLISH);
        Player online = Bukkit.getPlayerExact(ownerName);
        if (online != null) {
            return this.warpCache.getByOwner(online.getUniqueId());
        }
        List<Warp> warps = new ArrayList<Warp>();
        for (Warp warp : this.warpCache.allSnapshot()) {
            if (warp.ownerName().toLowerCase(Locale.ENGLISH).equals(normalized)) {
                warps.add(warp);
            }
        }
        return warps;
    }

    private UUID findWhitelistedUuid(long warpId, String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
        Map<UUID, String> members = this.warpCache.whitelistMemberNames(warpId);
        for (Map.Entry<UUID, String> entry : members.entrySet()) {
            if (entry.getValue().toLowerCase(Locale.ENGLISH).equals(normalized) || entry.getKey().toString().equalsIgnoreCase(normalized)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private List<Long> warpIds(Collection<Warp> warps) {
        List<Long> ids = new ArrayList<Long>();
        for (Warp warp : warps) {
            ids.add(warp.id());
        }
        return ids;
    }

    private boolean isDuplicateName(SQLException exception) {
        String state = exception.getSQLState();
        return state != null && state.startsWith("23");
    }

    private void sendStorageError(final CommandSender sender) {
        this.scheduler.runSync(new Runnable() {
            @Override
            public void run() {
                configManager.messages().send(sender, "messages.storage-error");
            }
        });
    }

    private void sendPlayerNotOnline(CommandSender sender, String playerName) {
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("player", playerName);
        this.configManager.messages().send(sender, "messages.player-not-online", placeholders);
    }

    private void sendWarpMessage(CommandSender sender, String path, String warpName) {
        this.configManager.messages().send(sender, path, warpPlaceholders(warpName));
    }

    private Map<String, String> warpPlaceholders(String warpName) {
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("warp", warpName == null ? "" : warpName);
        return placeholders;
    }

    private interface WarpUpdater {

        boolean persist(Warp oldWarp, long now) throws SQLException;

        Warp update(Warp oldWarp, long now);
    }
}
