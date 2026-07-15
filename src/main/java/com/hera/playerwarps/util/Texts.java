package com.hera.playerwarps.util;

import org.bukkit.ChatColor;

public final class Texts {

    private Texts() {
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
