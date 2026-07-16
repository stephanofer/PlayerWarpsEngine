package com.hera.playerwarps.teleport;

import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

public interface ChunkPreloader {

    CompletableFuture<Boolean> preload(World world, int chunkX, int chunkZ, boolean generate);
}
