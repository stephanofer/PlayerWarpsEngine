package com.hera.playerwarps.teleport;

import com.hera.playerwarps.bootstrap.PluginScheduler;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.config.TeleportSettings;
import com.hera.playerwarps.warp.VisitBuffer;
import com.hera.playerwarps.warp.Warp;
import com.hera.playerwarps.warp.WarpAccessResult;
import com.hera.playerwarps.warp.WarpAccessService;
import com.hera.playerwarps.warp.WarpCache;
import com.hera.playerwarps.warp.WarpLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

public final class TeleportService {

    private final ConfigManager configManager;
    private final PluginScheduler scheduler;
    private final WarpCache warpCache;
    private final WarpAccessService warpAccessService;
    private final SafetyChecker safetyChecker;
    private final ChunkPreloader chunkPreloader;
    private final VisitBuffer visitBuffer;
    private final Map<UUID, PendingTeleport> pendingTeleports = new ConcurrentHashMap<UUID, PendingTeleport>();
    private final Map<UUID, Long> lastTeleportAt = new ConcurrentHashMap<UUID, Long>();
    private final Queue<Runnable> finalTeleportQueue = new ConcurrentLinkedQueue<Runnable>();

    public TeleportService(ConfigManager configManager, PluginScheduler scheduler, WarpCache warpCache, WarpAccessService warpAccessService,
                           SafetyChecker safetyChecker, ChunkPreloader chunkPreloader, VisitBuffer visitBuffer) {
        this.configManager = configManager;
        this.scheduler = scheduler;
        this.warpCache = warpCache;
        this.warpAccessService = warpAccessService;
        this.safetyChecker = safetyChecker;
        this.chunkPreloader = chunkPreloader;
        this.visitBuffer = visitBuffer;
        this.scheduler.runTimerSync(new Runnable() {
            @Override
            public void run() {
                drainFinalTeleportQueue();
            }
        }, 1L, 1L);
    }

    public void teleport(Player player, Warp warp) {
        if (!canAccess(player, warp)) {
            return;
        }

        TeleportSettings settings = this.configManager.settings().teleportSettings();
        long now = System.currentTimeMillis();
        Long lastTeleport = this.lastTeleportAt.get(player.getUniqueId());
        if (lastTeleport != null && settings.cooldownSeconds() > 0) {
            long remainingMillis = settings.cooldownSeconds() * 1000L - (now - lastTeleport.longValue());
            if (remainingMillis > 0L) {
                Map<String, String> placeholders = new HashMap<String, String>();
                placeholders.put("seconds", String.valueOf((remainingMillis + 999L) / 1000L));
                this.configManager.messages().send(player, "messages.teleport-cooldown", placeholders);
                return;
            }
        }

        cancel(player.getUniqueId(), true);

        int delaySeconds = hasDelayBypass(player, settings) ? 0 : settings.delaySeconds();
        Location origin = player.getLocation();
        if (delaySeconds <= 0) {
            PendingTeleport pending = new PendingTeleport(player.getUniqueId(), warp.id(), origin.getWorld().getName(), origin.getBlockX(),
                    origin.getBlockY(), origin.getBlockZ(), now, null);
            this.pendingTeleports.put(player.getUniqueId(), pending);
            prepareTeleport(pending);
            return;
        }

        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("seconds", String.valueOf(delaySeconds));
        this.configManager.messages().send(player, "messages.teleport-starting", placeholders);

        BukkitTask task = this.scheduler.runLaterSync(new Runnable() {
            @Override
            public void run() {
                PendingTeleport pending = pendingTeleports.get(player.getUniqueId());
                if (pending != null && pending.warpId() == warp.id()) {
                    prepareTeleport(pending);
                }
            }
        }, delaySeconds * 20L);

        PendingTeleport pending = new PendingTeleport(player.getUniqueId(), warp.id(), origin.getWorld().getName(), origin.getBlockX(),
                origin.getBlockY(), origin.getBlockZ(), now, task);
        this.pendingTeleports.put(player.getUniqueId(), pending);
    }

    public boolean hasPending(UUID playerUuid) {
        return this.pendingTeleports.containsKey(playerUuid);
    }

    public PendingTeleport pending(UUID playerUuid) {
        return this.pendingTeleports.get(playerUuid);
    }

    public void cancel(UUID playerUuid, boolean notify) {
        PendingTeleport pending = this.pendingTeleports.remove(playerUuid);
        if (pending == null) {
            return;
        }
        if (pending.task() != null) {
            pending.task().cancel();
        }
        if (notify) {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null && player.isOnline()) {
                this.configManager.messages().send(player, "messages.teleport-cancelled");
            }
        }
    }

    public void cancelAll() {
        for (UUID playerUuid : this.pendingTeleports.keySet()) {
            cancel(playerUuid, false);
        }
        this.finalTeleportQueue.clear();
    }

    private boolean canAccess(Player player, Warp warp) {
        WarpAccessResult access = this.warpAccessService.canTeleport(player, warp);
        if (!access.isAllowed()) {
            this.configManager.messages().send(player, access.messageKey());
            return false;
        }
        return true;
    }

    private boolean hasDelayBypass(Player player, TeleportSettings settings) {
        return !settings.bypassDelayPermission().isEmpty() && player.hasPermission(settings.bypassDelayPermission());
    }

    private void prepareTeleport(final PendingTeleport pending) {
        final Player player = Bukkit.getPlayer(pending.playerUuid());
        if (player == null || !player.isOnline()) {
            this.pendingTeleports.remove(pending.playerUuid());
            return;
        }

        final Warp warp = this.warpCache.getById(pending.warpId()).orElse(null);
        if (warp == null) {
            this.pendingTeleports.remove(pending.playerUuid());
            debugTeleportFailure(player, null, "warp no longer exists in cache during prepare", null);
            this.configManager.messages().send(player, "messages.teleport-failed");
            return;
        }

        World world = Bukkit.getWorld(warp.location().world());
        if (world == null) {
            this.pendingTeleports.remove(pending.playerUuid());
            debugTeleportFailure(player, warp, "destination world is not loaded", null);
            this.configManager.messages().send(player, "messages.teleport-world-missing");
            return;
        }

        int chunkX = ((int) Math.floor(warp.location().x())) >> 4;
        int chunkZ = ((int) Math.floor(warp.location().z())) >> 4;
        TeleportSettings settings = this.configManager.settings().teleportSettings();
        CompletableFuture<Boolean> preload = settings.preloadChunk()
                ? this.chunkPreloader.preload(world, chunkX, chunkZ, settings.generateMissingChunks())
                : CompletableFuture.completedFuture(world.isChunkLoaded(chunkX, chunkZ));

        preload.whenComplete((loaded, throwable) -> scheduler.runSync(new Runnable() {
            @Override
            public void run() {
                if (throwable != null || loaded == null || !loaded.booleanValue()) {
                    pendingTeleports.remove(pending.playerUuid());
                    Player current = Bukkit.getPlayer(pending.playerUuid());
                    if (current != null && current.isOnline()) {
                        debugTeleportFailure(current, warp, "destination chunk could not be loaded", throwable);
                        configManager.messages().send(current, "messages.teleport-chunk-unavailable");
                    }
                    return;
                }
                enqueueFinalTeleport(pending);
            }
        }));
    }

    private void enqueueFinalTeleport(final PendingTeleport pending) {
        this.finalTeleportQueue.add(new Runnable() {
            @Override
            public void run() {
                executeFinalTeleport(pending);
            }
        });
    }

    private void drainFinalTeleportQueue() {
        int max = this.configManager.settings().teleportSettings().maxTeleportsPerTick();
        for (int i = 0; i < max; i++) {
            Runnable task = this.finalTeleportQueue.poll();
            if (task == null) {
                return;
            }
            task.run();
        }
    }

    private void executeFinalTeleport(PendingTeleport pending) {
        PendingTeleport currentPending = this.pendingTeleports.get(pending.playerUuid());
        if (currentPending == null || currentPending.warpId() != pending.warpId() || currentPending.createdAtMillis() != pending.createdAtMillis()) {
            return;
        }

        Player player = Bukkit.getPlayer(pending.playerUuid());
        if (player == null || !player.isOnline()) {
            this.pendingTeleports.remove(pending.playerUuid());
            return;
        }

        Warp warp = this.warpCache.getById(pending.warpId()).orElse(null);
        if (warp == null) {
            this.pendingTeleports.remove(pending.playerUuid());
            debugTeleportFailure(player, null, "warp no longer exists in cache during final teleport", null);
            this.configManager.messages().send(player, "messages.teleport-failed");
            return;
        }

        if (!canAccess(player, warp)) {
            this.pendingTeleports.remove(pending.playerUuid());
            return;
        }

        SafetyResult safety = this.safetyChecker.check(warp.location());
        if (!safety.isSafe() && this.configManager.settings().teleportSettings().unsafePolicy() == TeleportSettings.UnsafePolicy.BLOCK) {
            this.pendingTeleports.remove(pending.playerUuid());
            this.configManager.messages().send(player, safety.messageKey());
            return;
        }

        Location location = toBukkitLocation(warp.location());
        if (location == null) {
            this.pendingTeleports.remove(pending.playerUuid());
            debugTeleportFailure(player, warp, "destination world disappeared before final teleport", null);
            this.configManager.messages().send(player, "messages.teleport-world-missing");
            return;
        }

        boolean teleported = player.teleport(location, PlayerTeleportEvent.TeleportCause.PLUGIN);
        this.pendingTeleports.remove(pending.playerUuid());
        if (!teleported) {
            debugTeleportFailure(player, warp, "Bukkit Player#teleport returned false, likely cancelled by another plugin or server guard", null);
            this.configManager.messages().send(player, "messages.teleport-failed");
            return;
        }

        this.lastTeleportAt.put(player.getUniqueId(), System.currentTimeMillis());
        this.visitBuffer.record(warp.id());
        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("warp", warp.name());
        this.configManager.messages().send(player, "messages.teleport-success", placeholders);
    }

    private Location toBukkitLocation(WarpLocation warpLocation) {
        World world = Bukkit.getWorld(warpLocation.world());
        if (world == null) {
            return null;
        }
        return new Location(world, warpLocation.x(), warpLocation.y(), warpLocation.z(), warpLocation.yaw(), warpLocation.pitch());
    }

    private void debugTeleportFailure(Player player, Warp warp, String reason, Throwable throwable) {
        if (!this.configManager.settings().teleportSettings().debug()) {
            return;
        }

        StringBuilder message = new StringBuilder("[PlayerWarpsEngine] Teleport failed");
        if (player != null) {
            message.append(" player=").append(player.getName()).append(" uuid=").append(player.getUniqueId());
        }
        if (warp != null) {
            WarpLocation location = warp.location();
            message.append(" warp=").append(warp.name())
                    .append(" id=").append(warp.id())
                    .append(" world=").append(location.world())
                    .append(" x=").append(location.x())
                    .append(" y=").append(location.y())
                    .append(" z=").append(location.z());
        }
        message.append(" reason=").append(reason);

        if (throwable == null) {
            Bukkit.getLogger().warning(message.toString());
        } else {
            Bukkit.getLogger().log(Level.WARNING, message.toString(), throwable);
        }
    }
}
