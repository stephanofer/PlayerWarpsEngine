package com.hera.playerwarps.teleport;

import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

public final class AsyncChunkPreloader implements ChunkPreloader {

    private final ChunkPreloader fallback;

    public AsyncChunkPreloader(ChunkPreloader fallback) {
        this.fallback = fallback;
    }

    @Override
    public CompletableFuture<Boolean> preload(final World world, final int chunkX, final int chunkZ, final boolean generate) {
        final CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        try {
            if (world.isChunkLoaded(chunkX, chunkZ)) {
                future.complete(true);
                return future;
            }
            if (!generate && !world.chunkExists(chunkX, chunkZ)) {
                future.complete(false);
                return future;
            }

            world.getChunkAtAsync(chunkX, chunkZ, chunk -> future.complete(chunk != null && world.isChunkLoaded(chunkX, chunkZ)));
        } catch (Throwable throwable) {
            return this.fallback.preload(world, chunkX, chunkZ, generate);
        }
        return future;
    }
}
