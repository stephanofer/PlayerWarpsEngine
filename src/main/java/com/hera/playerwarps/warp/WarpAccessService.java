package com.hera.playerwarps.warp;

import org.bukkit.entity.Player;

public final class WarpAccessService {

    private final WarpCache warpCache;

    public WarpAccessService(WarpCache warpCache) {
        this.warpCache = warpCache;
    }

    public WarpAccessResult canTeleport(Player player, Warp warp) {
        if (warp.ownerUuid().equals(player.getUniqueId())) {
            return WarpAccessResult.allowed();
        }
        if (player.hasPermission("pwarp.admin.access.bypass")) {
            return WarpAccessResult.allowed();
        }
        if (warp.locked()) {
            return WarpAccessResult.denied("messages.teleport-locked");
        }
        if (warp.whitelistEnabled() && !this.warpCache.isWhitelisted(warp.id(), player.getUniqueId())) {
            return WarpAccessResult.denied("messages.teleport-whitelist");
        }
        return WarpAccessResult.allowed();
    }

    public boolean isVisible(Player player, Warp warp) {
        return canTeleport(player, warp).isAllowed();
    }

    public boolean canManage(Player player, Warp warp) {
        return warp.ownerUuid().equals(player.getUniqueId()) || player.hasPermission("pwarp.admin.manage");
    }
}
