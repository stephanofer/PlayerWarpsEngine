package com.hera.playerwarps.warp;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class VisitBufferTest {

    @Test
    void effectiveVisitsIncludesSessionRecordedVisits() {
        VisitBuffer visitBuffer = new VisitBuffer(null, null, null, "server");
        Warp warp = new Warp(1L, "server", "spawn", "spawn", UUID.randomUUID(), "Owner",
                new WarpLocation("world", 0D, 64D, 0D, 0F, 0F), null, (short) 0,
                false, false, 7L, SafeStatus.UNKNOWN, 1L, 1L, null);

        visitBuffer.record(warp.id());
        visitBuffer.record(warp.id());

        assertEquals(9L, visitBuffer.effectiveVisits(warp));
    }
}
