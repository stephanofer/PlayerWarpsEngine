package com.hera.playerwarps.teleport;

import com.hera.playerwarps.warp.WarpLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

public final class SafetyChecker {

    public SafetyResult check(WarpLocation warpLocation) {
        World world = Bukkit.getWorld(warpLocation.world());
        if (world == null) {
            return SafetyResult.unsafe("messages.teleport-world-missing");
        }
        return check(new Location(world, warpLocation.x(), warpLocation.y(), warpLocation.z(), warpLocation.yaw(), warpLocation.pitch()));
    }

    public SafetyResult check(Location location) {
        if (location == null || location.getWorld() == null) {
            return SafetyResult.unsafe("messages.teleport-world-missing");
        }

        World world = location.getWorld();
        int blockY = location.getBlockY();
        if (blockY <= 0 || blockY + 1 >= world.getMaxHeight()) {
            return SafetyResult.unsafe("messages.unsafe-location");
        }

        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!world.isChunkLoaded(chunkX, chunkZ)) {
            return SafetyResult.unsafe("messages.unsafe-location");
        }

        Block feet = world.getBlockAt(location.getBlockX(), blockY, location.getBlockZ());
        Block head = world.getBlockAt(location.getBlockX(), blockY + 1, location.getBlockZ());
        Block below = world.getBlockAt(location.getBlockX(), blockY - 1, location.getBlockZ());

        if (!isPassable(feet.getType()) || !isPassable(head.getType())) {
            return SafetyResult.unsafe("messages.unsafe-location");
        }

        if (!isSafeFloor(below.getType())) {
            return SafetyResult.unsafe("messages.unsafe-location");
        }

        return SafetyResult.safe();
    }

    private boolean isPassable(Material material) {
        return !material.isSolid() && !isDangerous(material);
    }

    private boolean isSafeFloor(Material material) {
        return material.isSolid() && !isDangerous(material);
    }

    private boolean isDangerous(Material material) {
        String name = material.name();
        return name.equals("LAVA")
                || name.equals("STATIONARY_LAVA")
                || name.equals("FIRE")
                || name.equals("CACTUS")
                || name.equals("PORTAL")
                || name.equals("ENDER_PORTAL")
                || name.equals("WATER")
                || name.equals("STATIONARY_WATER");
    }
}
