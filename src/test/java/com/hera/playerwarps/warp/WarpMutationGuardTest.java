package com.hera.playerwarps.warp;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WarpMutationGuardTest {

    @Test
    void doesNotAllowTwoConcurrentMutationsForSameWarp() {
        WarpMutationGuard guard = new WarpMutationGuard();

        assertTrue(guard.tryStart(10L));
        assertFalse(guard.tryStart(10L));

        guard.finish(10L);
        assertTrue(guard.tryStart(10L));
    }

    @Test
    void batchAcquireRollsBackIfAnyWarpIsBusy() {
        WarpMutationGuard guard = new WarpMutationGuard();
        assertTrue(guard.tryStart(2L));

        assertFalse(guard.tryStart(Arrays.asList(1L, 2L, 3L)));
        assertTrue(guard.tryStart(1L));
        assertTrue(guard.tryStart(3L));
    }
}
