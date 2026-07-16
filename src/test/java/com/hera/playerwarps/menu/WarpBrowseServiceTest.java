package com.hera.playerwarps.menu;

import com.hera.playerwarps.warp.SafeStatus;
import com.hera.playerwarps.warp.VisitBuffer;
import com.hera.playerwarps.warp.Warp;
import com.hera.playerwarps.warp.WarpCache;
import com.hera.playerwarps.warp.WarpLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class WarpBrowseServiceTest {

    @Test
    void accessibleScopeIncludesLockedAndWhitelistedWarps() {
        WarpCache cache = new WarpCache();
        cache.putWarp(warp(1L, "locked", true, false));
        cache.putWarp(warp(2L, "whitelisted", false, true));
        WarpBrowseService service = new WarpBrowseService(cache, new VisitBuffer(null, null, null, "server"));

        MenuSession session = new MenuSession(WarpSort.ALPHABETICAL);
        List<Warp> warps = service.browse(null, session);

        assertEquals(2, warps.size());
        assertEquals("locked", warps.get(0).name());
        assertEquals("whitelisted", warps.get(1).name());
    }

    private static Warp warp(long id, String name, boolean locked, boolean whitelistEnabled) {
        return new Warp(id, "server", name, name, UUID.randomUUID(), "owner",
                new WarpLocation("world", 0D, 64D, 0D, 0F, 0F), null, null, (short) 0,
                locked, whitelistEnabled, 0L, SafeStatus.SAFE, 0L, 0L, null);
    }
}
