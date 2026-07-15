package com.hera.playerwarps.bootstrap;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class PluginScheduler {

    private final Plugin plugin;

    public PluginScheduler(Plugin plugin) {
        this.plugin = plugin;
    }

    public BukkitTask runSync(Runnable task) {
        return Bukkit.getScheduler().runTask(this.plugin, task);
    }

    public BukkitTask runAsync(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(this.plugin, task);
    }

    public BukkitTask runLaterSync(Runnable task, long delayTicks) {
        return Bukkit.getScheduler().runTaskLater(this.plugin, task, delayTicks);
    }

    public BukkitTask runTimerSync(Runnable task, long delayTicks, long periodTicks) {
        return Bukkit.getScheduler().runTaskTimer(this.plugin, task, delayTicks, periodTicks);
    }

    public void cancelAll() {
        Bukkit.getScheduler().cancelTasks(this.plugin);
    }
}
