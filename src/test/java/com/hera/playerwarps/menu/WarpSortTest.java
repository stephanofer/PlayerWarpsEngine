package com.hera.playerwarps.menu;

import com.hera.playerwarps.warp.SafeStatus;
import com.hera.playerwarps.warp.VisitBuffer;
import com.hera.playerwarps.warp.Warp;
import com.hera.playerwarps.warp.WarpLocation;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WarpSortTest {

    @Test
    void newestSortsByCreatedAtDescendingThenName() {
        List<Warp> warps = new ArrayList<Warp>();
        warps.add(warp(1L, "zeta", "amy", 10L, 0L));
        warps.add(warp(2L, "alpha", "bob", 20L, 0L));
        warps.add(warp(3L, "beta", "carl", 20L, 0L));

        Collections.sort(warps, WarpSort.NEWEST.comparator(new VisitBuffer(null, null, null, "server")));

        assertEquals("alpha", warps.get(0).name());
        assertEquals("beta", warps.get(1).name());
        assertEquals("zeta", warps.get(2).name());
    }

    @Test
    void mostVisitsUsesEffectiveSessionVisits() {
        VisitBuffer visitBuffer = new VisitBuffer(null, null, null, "server");
        Warp lowPersisted = warp(1L, "low", "amy", 10L, 5L);
        Warp highSession = warp(2L, "high", "bob", 10L, 0L);
        visitBuffer.record(highSession.id());
        visitBuffer.record(highSession.id());
        visitBuffer.record(highSession.id());
        visitBuffer.record(highSession.id());
        visitBuffer.record(highSession.id());
        visitBuffer.record(highSession.id());

        List<Warp> warps = new ArrayList<Warp>();
        warps.add(lowPersisted);
        warps.add(highSession);

        Collections.sort(warps, WarpSort.MOST_VISITS.comparator(visitBuffer));

        assertEquals("high", warps.get(0).name());
    }

    private static Warp warp(long id, String name, String owner, long createdAt, long visits) {
        return new Warp(id, "server", name, name, UUID.nameUUIDFromBytes(owner.getBytes()), owner,
                new WarpLocation("world", 0D, 64D, 0D, 0F, 0F), null, (short) 0,
                false, false, visits, SafeStatus.UNKNOWN, createdAt, createdAt, null);
    }
}
