package com.hera.playerwarps.config;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.block.implementation.Section;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class LimitSettings {

    private final int defaultLimit;
    private final boolean numericPermissionEnabled;
    private final String numericPermissionPrefix;
    private final Map<String, Integer> permissionLimits;

    private LimitSettings(int defaultLimit, boolean numericPermissionEnabled, String numericPermissionPrefix, Map<String, Integer> permissionLimits) {
        this.defaultLimit = defaultLimit;
        this.numericPermissionEnabled = numericPermissionEnabled;
        this.numericPermissionPrefix = numericPermissionPrefix;
        this.permissionLimits = Collections.unmodifiableMap(new LinkedHashMap<String, Integer>(permissionLimits));
    }

    public static LimitSettings from(YamlDocument config) {
        int defaultLimit = config.getInt("limits.default", 10);
        if (defaultLimit < 0) {
            throw new IllegalArgumentException("limits.default cannot be negative");
        }

        boolean numericPermissionEnabled = config.getBoolean("limits.numeric-permission.enabled", true);
        String numericPermissionPrefix = config.getString("limits.numeric-permission.prefix", "pwarp.limit.").trim().toLowerCase(Locale.ENGLISH);
        if (numericPermissionEnabled && numericPermissionPrefix.isEmpty()) {
            throw new IllegalArgumentException("limits.numeric-permission.prefix cannot be empty when numeric permissions are enabled");
        }

        Map<String, Integer> permissionLimits = new LinkedHashMap<String, Integer>();
        Section groups = config.getSection("limits.groups", null);
        if (groups != null) {
            for (String groupKey : groups.getRoutesAsStrings(false)) {
                String permission = config.getString("limits.groups." + groupKey + ".permission", "").trim().toLowerCase(Locale.ENGLISH);
                int amount = config.getInt("limits.groups." + groupKey + ".amount", -1);
                if (permission.isEmpty()) {
                    throw new IllegalArgumentException("limits.groups." + groupKey + ".permission cannot be empty");
                }
                if (amount < 0) {
                    throw new IllegalArgumentException("limits.groups." + groupKey + ".amount cannot be negative");
                }
                permissionLimits.put(permission, amount);
            }
        }

        return new LimitSettings(defaultLimit, numericPermissionEnabled, numericPermissionPrefix, permissionLimits);
    }

    public int defaultLimit() {
        return this.defaultLimit;
    }

    public boolean numericPermissionEnabled() {
        return this.numericPermissionEnabled;
    }

    public String numericPermissionPrefix() {
        return this.numericPermissionPrefix;
    }

    public Map<String, Integer> permissionLimits() {
        return this.permissionLimits;
    }
}
