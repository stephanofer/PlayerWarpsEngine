package com.hera.playerwarps.warp;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WarpMutationGuard {

    private final Set<Long> activeWarpIds = ConcurrentHashMap.newKeySet();

    public boolean tryStart(long warpId) {
        return this.activeWarpIds.add(warpId);
    }

    public boolean tryStart(Collection<Long> warpIds) {
        Set<Long> acquired = new LinkedHashSet<Long>();
        for (Long warpId : warpIds) {
            if (warpId == null) {
                continue;
            }
            if (!this.activeWarpIds.add(warpId.longValue())) {
                this.activeWarpIds.removeAll(acquired);
                return false;
            }
            acquired.add(warpId.longValue());
        }
        return true;
    }

    public void finish(long warpId) {
        this.activeWarpIds.remove(warpId);
    }

    public void finish(Collection<Long> warpIds) {
        for (Long warpId : warpIds) {
            if (warpId != null) {
                finish(warpId.longValue());
            }
        }
    }
}
