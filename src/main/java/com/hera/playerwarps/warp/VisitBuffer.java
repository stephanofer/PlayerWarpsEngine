package com.hera.playerwarps.warp;

import com.hera.playerwarps.bootstrap.PluginScheduler;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public final class VisitBuffer {

    private static final long FLUSH_PERIOD_TICKS = 20L * 60L;

    private final Plugin plugin;
    private final PluginScheduler scheduler;
    private final WarpRepository warpRepository;
    private final String serverId;
    private final Map<Long, AtomicLong> deltas = new ConcurrentHashMap<Long, AtomicLong>();
    private final Map<Long, AtomicLong> sessionTotals = new ConcurrentHashMap<Long, AtomicLong>();
    private BukkitTask flushTask;

    public VisitBuffer(Plugin plugin, PluginScheduler scheduler, WarpRepository warpRepository, String serverId) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.warpRepository = warpRepository;
        this.serverId = serverId;
    }

    public void start() {
        this.flushTask = this.scheduler.runTimerSync(new Runnable() {
            @Override
            public void run() {
                flushAsync();
            }
        }, FLUSH_PERIOD_TICKS, FLUSH_PERIOD_TICKS);
    }

    public void record(long warpId) {
        AtomicLong counter = this.deltas.get(warpId);
        if (counter == null) {
            AtomicLong created = new AtomicLong();
            AtomicLong previous = this.deltas.putIfAbsent(warpId, created);
            counter = previous == null ? created : previous;
        }
        counter.incrementAndGet();

        AtomicLong sessionCounter = this.sessionTotals.get(warpId);
        if (sessionCounter == null) {
            AtomicLong created = new AtomicLong();
            AtomicLong previous = this.sessionTotals.putIfAbsent(warpId, created);
            sessionCounter = previous == null ? created : previous;
        }
        sessionCounter.incrementAndGet();
    }

    public long sessionVisits(long warpId) {
        AtomicLong counter = this.sessionTotals.get(warpId);
        return counter == null ? 0L : Math.max(0L, counter.get());
    }

    public long effectiveVisits(Warp warp) {
        long delta = sessionVisits(warp.id());
        if (Long.MAX_VALUE - warp.visits() < delta) {
            return Long.MAX_VALUE;
        }
        return warp.visits() + delta;
    }

    public void flushAsync() {
        final Map<Long, Long> snapshot = drain();
        if (snapshot.isEmpty()) {
            return;
        }

        this.scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                flushSnapshot(snapshot);
            }
        });
    }

    public void flushBlocking() {
        Map<Long, Long> snapshot = drain();
        if (!snapshot.isEmpty()) {
            flushSnapshot(snapshot);
        }
    }

    public void stop() {
        if (this.flushTask != null) {
            this.flushTask.cancel();
            this.flushTask = null;
        }
    }

    private Map<Long, Long> drain() {
        Map<Long, Long> snapshot = new HashMap<Long, Long>();
        for (Map.Entry<Long, AtomicLong> entry : this.deltas.entrySet()) {
            long delta = entry.getValue().getAndSet(0L);
            if (delta > 0L) {
                snapshot.put(entry.getKey(), delta);
            }
        }
        return snapshot;
    }

    private void flushSnapshot(Map<Long, Long> snapshot) {
        try {
            this.warpRepository.incrementVisits(this.serverId, snapshot, System.currentTimeMillis());
        } catch (SQLException exception) {
            restore(snapshot);
            this.plugin.getLogger().warning("Failed to flush player warp visits: " + exception.getMessage());
        }
    }

    private void restore(Map<Long, Long> snapshot) {
        for (Map.Entry<Long, Long> entry : snapshot.entrySet()) {
            AtomicLong counter = this.deltas.get(entry.getKey());
            if (counter == null) {
                AtomicLong created = new AtomicLong();
                AtomicLong previous = this.deltas.putIfAbsent(entry.getKey(), created);
                counter = previous == null ? created : previous;
            }
            counter.addAndGet(entry.getValue());
        }
    }
}
