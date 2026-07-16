package com.hera.playerwarps.menu;

import com.hera.playerwarps.bootstrap.PluginScheduler;
import com.hera.playerwarps.config.ConfigManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuSessionStore {

    private static final long SESSION_TTL_MILLIS = 15L * 60L * 1000L;

    private final ConfigManager configManager;
    private final PluginScheduler scheduler;
    private final Map<UUID, MenuSession> sessions = new ConcurrentHashMap<UUID, MenuSession>();
    private final Map<UUID, Long> pendingSearches = new ConcurrentHashMap<UUID, Long>();
    private BukkitTask cleanupTask;

    public MenuSessionStore(ConfigManager configManager, PluginScheduler scheduler) {
        this.configManager = configManager;
        this.scheduler = scheduler;
    }

    public void start() {
        this.cleanupTask = this.scheduler.runTimerSync(new Runnable() {
            @Override
            public void run() {
                cleanup();
            }
        }, 20L, 20L);
    }

    public void stop() {
        if (this.cleanupTask != null) {
            this.cleanupTask.cancel();
            this.cleanupTask = null;
        }
        clear();
    }

    public MenuSession session(Player player) {
        MenuSession session = this.sessions.get(player.getUniqueId());
        if (session == null) {
            MenuSession created = new MenuSession(WarpSort.from(this.configManager.settings().menuSettings().defaultSort()));
            MenuSession previous = this.sessions.putIfAbsent(player.getUniqueId(), created);
            session = previous == null ? created : previous;
        }
        session.touch();
        return session;
    }

    public void beginSearch(Player player) {
        long timeoutMillis = this.configManager.settings().menuSettings().searchTimeoutSeconds() * 1000L;
        this.pendingSearches.put(player.getUniqueId(), System.currentTimeMillis() + timeoutMillis);
    }

    public boolean consumeSearch(Player player) {
        Long expiresAt = this.pendingSearches.remove(player.getUniqueId());
        return expiresAt != null && System.currentTimeMillis() <= expiresAt.longValue();
    }

    public boolean hasPendingSearch(Player player) {
        Long expiresAt = this.pendingSearches.get(player.getUniqueId());
        return expiresAt != null && System.currentTimeMillis() <= expiresAt.longValue();
    }

    public void remove(Player player) {
        this.sessions.remove(player.getUniqueId());
        this.pendingSearches.remove(player.getUniqueId());
    }

    public void clear() {
        this.sessions.clear();
        this.pendingSearches.clear();
    }

    private void cleanup() {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<UUID, Long>> pendingIterator = this.pendingSearches.entrySet().iterator();
        while (pendingIterator.hasNext()) {
            if (pendingIterator.next().getValue().longValue() < now) {
                pendingIterator.remove();
            }
        }

        Iterator<Map.Entry<UUID, MenuSession>> sessionIterator = this.sessions.entrySet().iterator();
        while (sessionIterator.hasNext()) {
            if (now - sessionIterator.next().getValue().lastAccessAt() > SESSION_TTL_MILLIS) {
                sessionIterator.remove();
            }
        }
    }
}
