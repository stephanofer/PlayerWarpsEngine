package com.hera.playerwarps.warp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class WarpCache {

    private final Map<Long, Warp> warpsById = new ConcurrentHashMap<Long, Warp>();
    private final Map<String, Long> warpIdByName = new ConcurrentHashMap<String, Long>();
    private final Map<UUID, Set<Long>> warpIdsByOwner = new ConcurrentHashMap<UUID, Set<Long>>();
    private final Map<Long, Set<UUID>> whitelistByWarpId = new ConcurrentHashMap<Long, Set<UUID>>();
    private final Map<UUID, Integer> bonusByPlayer = new ConcurrentHashMap<UUID, Integer>();
    private volatile List<Warp> allWarpSnapshot = Collections.emptyList();

    public synchronized void replaceAll(Collection<Warp> warps, Map<Long, List<WhitelistEntry>> whitelistEntries, Map<UUID, Integer> bonuses) {
        this.warpsById.clear();
        this.warpIdByName.clear();
        this.warpIdsByOwner.clear();
        this.whitelistByWarpId.clear();
        this.bonusByPlayer.clear();

        for (Warp warp : warps) {
            indexWarp(warp);
        }

        for (Map.Entry<Long, List<WhitelistEntry>> entry : whitelistEntries.entrySet()) {
            if (!this.warpsById.containsKey(entry.getKey())) {
                continue;
            }
            Set<UUID> players = new LinkedHashSet<UUID>();
            for (WhitelistEntry whitelistEntry : entry.getValue()) {
                players.add(whitelistEntry.playerUuid());
            }
            this.whitelistByWarpId.put(entry.getKey(), Collections.unmodifiableSet(players));
        }

        for (Map.Entry<UUID, Integer> entry : bonuses.entrySet()) {
            this.bonusByPlayer.put(entry.getKey(), Math.max(0, entry.getValue()));
        }

        rebuildSnapshot();
    }

    public Optional<Warp> getById(long id) {
        return Optional.ofNullable(this.warpsById.get(id));
    }

    public Optional<Warp> getByName(String normalizedName) {
        Long id = this.warpIdByName.get(normalizedName);
        return id == null ? Optional.<Warp>empty() : getById(id);
    }

    public synchronized List<Warp> getByOwner(UUID ownerUuid) {
        Set<Long> ids = this.warpIdsByOwner.get(ownerUuid);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Warp> warps = new ArrayList<Warp>();
        for (Long id : ids) {
            Warp warp = this.warpsById.get(id);
            if (warp != null) {
                warps.add(warp);
            }
        }
        return Collections.unmodifiableList(warps);
    }

    public synchronized int countByOwner(UUID ownerUuid) {
        Set<Long> ids = this.warpIdsByOwner.get(ownerUuid);
        return ids == null ? 0 : ids.size();
    }

    public List<Warp> allSnapshot() {
        return this.allWarpSnapshot;
    }

    public int totalWarps() {
        return this.allWarpSnapshot.size();
    }

    public boolean isWhitelisted(long warpId, UUID playerUuid) {
        Set<UUID> players = this.whitelistByWarpId.get(warpId);
        return players != null && players.contains(playerUuid);
    }

    public int bonusOf(UUID playerUuid) {
        Integer bonus = this.bonusByPlayer.get(playerUuid);
        return bonus == null ? 0 : Math.max(0, bonus);
    }

    public synchronized void putWarp(Warp warp) {
        Warp previous = this.warpsById.get(warp.id());
        if (previous != null) {
            removeIndexes(previous);
        }
        indexWarp(warp);
        rebuildSnapshot();
    }

    public synchronized void removeWarp(Warp warp) {
        removeIndexes(warp);
        this.whitelistByWarpId.remove(warp.id());
        rebuildSnapshot();
    }

    public synchronized void replaceWarp(Warp oldWarp, Warp newWarp) {
        removeIndexes(oldWarp);
        indexWarp(newWarp);
        rebuildSnapshot();
    }

    public synchronized void setWhitelist(long warpId, Set<UUID> entries) {
        this.whitelistByWarpId.put(warpId, Collections.unmodifiableSet(new LinkedHashSet<UUID>(entries)));
    }

    public synchronized void setBonus(UUID playerUuid, int bonus) {
        this.bonusByPlayer.put(playerUuid, Math.max(0, bonus));
    }

    public synchronized void clear() {
        this.warpsById.clear();
        this.warpIdByName.clear();
        this.warpIdsByOwner.clear();
        this.whitelistByWarpId.clear();
        this.bonusByPlayer.clear();
        this.allWarpSnapshot = Collections.emptyList();
    }

    private void indexWarp(Warp warp) {
        if (this.warpsById.containsKey(warp.id())) {
            throw new IllegalStateException("Duplicate warp id in cache: " + warp.id());
        }
        Long previousName = this.warpIdByName.get(warp.nameNormalized());
        if (previousName != null && previousName.longValue() != warp.id()) {
            throw new IllegalStateException("Duplicate warp name in cache: " + warp.nameNormalized());
        }

        this.warpsById.put(warp.id(), warp);
        this.warpIdByName.put(warp.nameNormalized(), warp.id());
        Set<Long> ownerIds = this.warpIdsByOwner.get(warp.ownerUuid());
        if (ownerIds == null) {
            ownerIds = new LinkedHashSet<Long>();
            this.warpIdsByOwner.put(warp.ownerUuid(), ownerIds);
        }
        ownerIds.add(warp.id());
    }

    private void removeIndexes(Warp warp) {
        this.warpsById.remove(warp.id());
        this.warpIdByName.remove(warp.nameNormalized());
        Set<Long> ownerIds = this.warpIdsByOwner.get(warp.ownerUuid());
        if (ownerIds != null) {
            ownerIds.remove(warp.id());
            if (ownerIds.isEmpty()) {
                this.warpIdsByOwner.remove(warp.ownerUuid());
            }
        }
    }

    private void rebuildSnapshot() {
        this.allWarpSnapshot = Collections.unmodifiableList(new ArrayList<Warp>(this.warpsById.values()));
    }
}
