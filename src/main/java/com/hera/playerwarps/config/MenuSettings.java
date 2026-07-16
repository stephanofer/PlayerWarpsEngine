package com.hera.playerwarps.config;

import dev.dejvokep.boostedyaml.YamlDocument;

import java.util.Locale;

public final class MenuSettings {

    private final String defaultSort;
    private final int searchTimeoutSeconds;
    private final int searchMaxLength;
    private final int confirmationTimeoutSeconds;
    private final String fallbackIconMaterial;
    private final short fallbackIconData;

    private MenuSettings(String defaultSort, int searchTimeoutSeconds, int searchMaxLength, int confirmationTimeoutSeconds,
                         String fallbackIconMaterial, short fallbackIconData) {
        this.defaultSort = defaultSort;
        this.searchTimeoutSeconds = searchTimeoutSeconds;
        this.searchMaxLength = searchMaxLength;
        this.confirmationTimeoutSeconds = confirmationTimeoutSeconds;
        this.fallbackIconMaterial = fallbackIconMaterial;
        this.fallbackIconData = fallbackIconData;
    }

    public static MenuSettings from(YamlDocument config) {
        String defaultSort = config.getString("menus.default-sort", "newest").trim().toLowerCase(Locale.ENGLISH);
        if (!defaultSort.equals("newest") && !defaultSort.equals("oldest") && !defaultSort.equals("most-visits")
                && !defaultSort.equals("alphabetical") && !defaultSort.equals("owner")) {
            throw new IllegalArgumentException("menus.default-sort must be newest, oldest, most-visits, alphabetical or owner");
        }

        int searchTimeoutSeconds = Math.max(1, config.getInt("menus.search-timeout-seconds", 30));
        int searchMaxLength = Math.max(1, config.getInt("menus.search-max-length", 64));
        int confirmationTimeoutSeconds = Math.max(1, config.getInt("menus.confirmation-timeout-seconds", 5));
        String fallbackIconMaterial = config.getString("menus.fallback-icon.material", "ENDER_PEARL").trim().toUpperCase(Locale.ENGLISH);
        int fallbackIconData = config.getInt("menus.fallback-icon.data", 0);
        if (fallbackIconData < Short.MIN_VALUE || fallbackIconData > Short.MAX_VALUE) {
            throw new IllegalArgumentException("menus.fallback-icon.data is out of range");
        }

        return new MenuSettings(defaultSort, searchTimeoutSeconds, searchMaxLength, confirmationTimeoutSeconds,
                fallbackIconMaterial, (short) fallbackIconData);
    }

    public String defaultSort() {
        return this.defaultSort;
    }

    public int searchTimeoutSeconds() {
        return this.searchTimeoutSeconds;
    }

    public int searchMaxLength() {
        return this.searchMaxLength;
    }

    public int confirmationTimeoutSeconds() {
        return this.confirmationTimeoutSeconds;
    }

    public String fallbackIconMaterial() {
        return this.fallbackIconMaterial;
    }

    public short fallbackIconData() {
        return this.fallbackIconData;
    }
}
