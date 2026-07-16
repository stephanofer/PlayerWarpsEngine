package com.hera.playerwarps.teleport;

import com.hera.playerwarps.bootstrap.PluginScheduler;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

public final class SyncChunkPreloader implements ChunkPreloader {

    private final PluginScheduler scheduler;

    public SyncChunkPreloader(PluginScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public CompletableFuture<Boolean> preload(final World world, final int chunkX, final int chunkZ, final boolean generate) {
        final CompletableFuture<Boolean> future = new CompletableFuture<Boolean>();
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    if (world.isChunkLoaded(chunkX, chunkZ)) {
                        future.complete(true);
                        return;
                    }
                    if (!generate && !world.chunkExists(chunkX, chunkZ)) {
                        future.complete(false);
                        return;
                    }
                    future.complete(world.loadChunk(chunkX, chunkZ, generate));
                } catch (Throwable throwable) {
                    future.complete(false);
                }
            }
        };

        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            this.scheduler.runSync(task);
        }
        return future;
    }
}
