package com.hera.playerwarps.warp;

import java.util.Locale;

public final class WarpName {

    private final String value;
    private final String normalized;

    private WarpName(String value, String normalized) {
        this.value = value;
        this.normalized = normalized;
    }

    public static WarpName of(String value) {
        String trimmed = value == null ? "" : value.trim();
        return new WarpName(trimmed, normalize(trimmed));
    }

    public static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ENGLISH);
    }

    public String value() {
        return this.value;
    }

    public String normalized() {
        return this.normalized;
    }
}
