package com.hera.playerwarps.util;

import java.util.Locale;
import java.util.regex.Pattern;

public final class Names {

    private static final Pattern COMMAND_LABEL = Pattern.compile("^[a-z0-9_-]{1,32}$");

    private Names() {
    }

    public static String normalizeCommandLabel(String label) {
        if (label == null) {
            return "";
        }
        return label.trim().toLowerCase(Locale.ENGLISH);
    }

    public static boolean isValidCommandLabel(String label) {
        return label != null && COMMAND_LABEL.matcher(label).matches();
    }
}
