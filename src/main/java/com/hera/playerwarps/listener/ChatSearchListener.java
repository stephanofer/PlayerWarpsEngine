package com.hera.playerwarps.listener;

import com.hera.playerwarps.bootstrap.PluginScheduler;
import com.hera.playerwarps.menu.MenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ChatSearchListener implements Listener {

    private final PluginScheduler scheduler;
    private final MenuService menuService;

    public ChatSearchListener(PluginScheduler scheduler, MenuService menuService) {
        this.scheduler = scheduler;
        this.menuService = menuService;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        final Player player = event.getPlayer();
        if (!this.menuService.hasPendingSearch(player)) {
            return;
        }

        event.setCancelled(true);
        final String message = event.getMessage();
        this.scheduler.runSync(new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    menuService.consumeSearch(player, message);
                }
            }
        });
    }
}
