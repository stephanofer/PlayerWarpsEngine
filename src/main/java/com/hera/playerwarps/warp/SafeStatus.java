package com.hera.playerwarps.warp;

import java.util.Locale;

public enum SafeStatus {
    UNKNOWN,
    SAFE,
    UNSAFE;

    public static SafeStatus fromDatabase(String value) {
        if (value == null) {
            return UNKNOWN;
        }

        try {
            return SafeStatus.valueOf(value.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException exception) {
            return UNKNOWN;
        }
    }
}
