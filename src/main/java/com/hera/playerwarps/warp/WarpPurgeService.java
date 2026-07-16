package com.hera.playerwarps.warp;

import com.hera.playerwarps.bootstrap.PluginScheduler;
import com.hera.playerwarps.config.ConfigManager;
import com.hera.playerwarps.teleport.SafetyChecker;
import com.hera.playerwarps.teleport.SafetyResult;
import org.bukkit.command.CommandSender;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class WarpPurgeService {

    private static final int SCAN_BATCH_SIZE = 50;
    private static final long CONFIRM_TTL_MILLIS = 60000L;

    private final ConfigManager configManager;
    private final PluginScheduler scheduler;
    private final WarpRepository warpRepository;
    private final WarpCache warpCache;
    private final SafetyChecker safetyChecker;
    private final Map<String, PurgeSession> sessions = new ConcurrentHashMap<String, PurgeSession>();

    public WarpPurgeService(ConfigManager configManager, PluginScheduler scheduler, WarpRepository warpRepository, WarpCache warpCache,
                            SafetyChecker safetyChecker) {
        this.configManager = configManager;
        this.scheduler = scheduler;
        this.warpRepository = warpRepository;
        this.warpCache = warpCache;
        this.safetyChecker = safetyChecker;
    }

    public void previewUnsafe(final CommandSender sender) {
        if (!this.configManager.configs().config().getBoolean("purge.unsafe.enabled", true)) {
            this.configManager.messages().send(sender, "messages.purge-disabled");
            return;
        }

        final List<Warp> snapshot = new ArrayList<Warp>(this.warpCache.allSnapshot());
        final List<Warp> unsafe = new ArrayList<Warp>();
        scanUnsafeBatch(sender, snapshot, unsafe, 0);
    }

    public void previewInactive(CommandSender sender, int days) {
        if (!this.configManager.configs().config().getBoolean("purge.inactive.enabled", false)) {
            this.configManager.messages().send(sender, "messages.purge-disabled");
            return;
        }
        if (days <= 0) {
            this.configManager.messages().send(sender, "messages.invalid-days");
            return;
        }

        long cutoff = System.currentTimeMillis() - days * 86400000L;
        List<Warp> inactive = new ArrayList<Warp>();
        for (Warp warp : this.warpCache.allSnapshot()) {
            long reference = warp.lastVisitedAt() == null ? warp.createdAt() : warp.lastVisitedAt().longValue();
            if (reference < cutoff) {
                inactive.add(warp);
            }
        }
        storeAndReport(sender, "inactive", inactive);
    }

    public void confirm(final CommandSender sender, String type) {
        String key = sessionKey(sender);
        PurgeSession session = this.sessions.get(key);
        if (session == null || session.expired() || !session.type().equals(type.toLowerCase(Locale.ENGLISH))) {
            this.sessions.remove(key);
            this.configManager.messages().send(sender, "messages.purge-expired");
            return;
        }

        final List<Long> ids = new ArrayList<Long>(session.ids());
        final List<Warp> warps = warpsByIds(ids);
        this.sessions.remove(key);
        this.scheduler.runAsync(new Runnable() {
            @Override
            public void run() {
                try {
                    final int removed = warpRepository.deleteByIds(configManager.settings().serverId(), ids);
                    scheduler.runSync(new Runnable() {
                        @Override
                        public void run() {
                            warpCache.removeWarps(warps);
                            Map<String, String> placeholders = new HashMap<String, String>();
                            placeholders.put("amount", String.valueOf(removed));
                            configManager.messages().send(sender, "messages.purge-complete", placeholders);
                        }
                    });
                } catch (SQLException exception) {
                    scheduler.runSync(new Runnable() {
                        @Override
                        public void run() {
                            configManager.messages().send(sender, "messages.storage-error");
                        }
                    });
                }
            }
        });
    }

    private void scanUnsafeBatch(final CommandSender sender, final List<Warp> snapshot, final List<Warp> unsafe, final int startIndex) {
        int end = Math.min(snapshot.size(), startIndex + SCAN_BATCH_SIZE);
        for (int index = startIndex; index < end; index++) {
            Warp warp = snapshot.get(index);
            SafetyResult result = this.safetyChecker.check(warp.location());
            if (!result.isSafe()) {
                unsafe.add(warp);
            }
        }

        if (end >= snapshot.size()) {
            storeAndReport(sender, "unsafe", unsafe);
            return;
        }

        this.scheduler.runLaterSync(new Runnable() {
            @Override
            public void run() {
                scanUnsafeBatch(sender, snapshot, unsafe, end);
            }
        }, 1L);
    }

    private void storeAndReport(CommandSender sender, String type, List<Warp> warps) {
        List<Long> ids = new ArrayList<Long>();
        for (Warp warp : warps) {
            ids.add(warp.id());
        }
        this.sessions.put(sessionKey(sender), new PurgeSession(type, ids, System.currentTimeMillis() + CONFIRM_TTL_MILLIS));

        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("type", type.equals("unsafe") ? "ubicaciones inseguras" : "warps inactivos");
        placeholders.put("amount", String.valueOf(ids.size()));
        this.configManager.messages().send(sender, "messages.purge-preview", placeholders);

        int shown = 0;
        Iterator<Warp> iterator = warps.iterator();
        while (iterator.hasNext() && shown < 10) {
            Warp warp = iterator.next();
            Map<String, String> line = new HashMap<String, String>();
            line.put("warp", warp.name());
            line.put("owner", warp.ownerName());
            this.configManager.messages().send(sender, "messages.purge-preview-line", line);
            shown++;
        }
        this.configManager.messages().send(sender, "messages.purge-confirm-required");
    }

    private List<Warp> warpsByIds(Collection<Long> ids) {
        List<Warp> warps = new ArrayList<Warp>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            Warp warp = this.warpCache.getById(id.longValue()).orElse(null);
            if (warp != null) {
                warps.add(warp);
            }
        }
        return warps;
    }

    private String sessionKey(CommandSender sender) {
        return sender.getName().toLowerCase(Locale.ENGLISH);
    }

    private static final class PurgeSession {

        private final String type;
        private final List<Long> ids;
        private final long expiresAt;

        private PurgeSession(String type, List<Long> ids, long expiresAt) {
            this.type = type;
            this.ids = ids;
            this.expiresAt = expiresAt;
        }

        private String type() {
            return this.type;
        }

        private List<Long> ids() {
            return this.ids;
        }

        private boolean expired() {
            return System.currentTimeMillis() > this.expiresAt;
        }
    }
}
