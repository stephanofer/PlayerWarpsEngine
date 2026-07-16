package com.hera.playerwarps.menu;

import com.hera.playerwarps.warp.VisitBuffer;
import com.hera.playerwarps.warp.Warp;
import com.hera.playerwarps.warp.WarpCache;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class WarpBrowseService {

    private final WarpCache warpCache;
    private final VisitBuffer visitBuffer;
    private final Map<WarpSort, CachedSort> sortedCache = new HashMap<WarpSort, CachedSort>();

    public WarpBrowseService(WarpCache warpCache, VisitBuffer visitBuffer) {
        this.warpCache = warpCache;
        this.visitBuffer = visitBuffer;
    }

    public List<Warp> browse(Player player, MenuSession session) {
        List<Warp> sorted = sorted(session.sort());
        String query = normalize(session.query());
        List<Warp> result = new ArrayList<Warp>();

        for (Warp warp : sorted) {
            if (session.scope() == WarpScope.MY_WARPS) {
                if (!warp.ownerUuid().equals(player.getUniqueId())) {
                    continue;
                }
            }

            if (!query.isEmpty() && !matches(warp, query)) {
                continue;
            }
            result.add(warp);
        }

        return result;
    }

    private List<Warp> sorted(WarpSort sort) {
        if (sort == WarpSort.MOST_VISITS) {
            List<Warp> warps = new ArrayList<Warp>(this.warpCache.allSnapshot());
            Collections.sort(warps, sort.comparator(this.visitBuffer));
            return warps;
        }

        long revision = this.warpCache.revision();
        CachedSort cached = this.sortedCache.get(sort);
        if (cached != null && cached.revision == revision) {
            return cached.warps;
        }

        List<Warp> warps = new ArrayList<Warp>(this.warpCache.allSnapshot());
        Collections.sort(warps, sort.comparator(this.visitBuffer));
        List<Warp> unmodifiable = Collections.unmodifiableList(warps);
        this.sortedCache.put(sort, new CachedSort(revision, unmodifiable));
        return unmodifiable;
    }

    private static boolean matches(Warp warp, String query) {
        if (normalize(warp.name()).contains(query)) {
            return true;
        }
        if (normalize(warp.ownerName()).contains(query)) {
            return true;
        }
        return normalize(warp.description()).contains(query);
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }

    private static final class CachedSort {
        private final long revision;
        private final List<Warp> warps;

        private CachedSort(long revision, List<Warp> warps) {
            this.revision = revision;
            this.warps = warps;
        }
    }
}
