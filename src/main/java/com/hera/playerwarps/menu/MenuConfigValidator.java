package com.hera.playerwarps.menu;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class MenuConfigValidator {

    private static final Set<String> CUSTOM_TYPES = new HashSet<String>(Arrays.asList(
            "PWE_WARP_LIST",
            "PWE_MY_WARPS",
            "PWE_SEARCH",
            "PWE_REFRESH",
            "PWE_SCOPE",
            "PWE_FILTER_SORT",
            "PWE_MANAGE_WARP",
            "PWE_WHITELIST_LIST"
    ));

    public void validateInventory(File file) {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        int size = configuration.getInt("size", 54);
        if (size <= 0 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException(file.getName() + " has invalid size: " + size);
        }
        validateSection(file, configuration, "items", size);
    }

    public void validatePattern(File file) {
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        int size = configuration.getInt("size", 54);
        if (size <= 0 || size > 54 || size % 9 != 0) {
            throw new IllegalArgumentException(file.getName() + " has invalid size: " + size);
        }
        validateSection(file, configuration, "items", size);
    }

    public void validateMaterial(String materialName, String context) {
        if (materialName == null || materialName.trim().isEmpty()) {
            throw new IllegalArgumentException(context + " material cannot be empty");
        }
        Material material = Material.matchMaterial(materialName.trim());
        if (material == null || material == Material.AIR) {
            throw new IllegalArgumentException(context + " uses invalid material: " + materialName);
        }
    }

    private void validateSection(File file, YamlConfiguration configuration, String path, int size) {
        ConfigurationSection section = configuration.getConfigurationSection(path);
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            String buttonPath = path + "." + key;
            String type = configuration.getString(buttonPath + ".type", "NONE").trim().toUpperCase();
            if (type.startsWith("PWE_") && !CUSTOM_TYPES.contains(type)) {
                throw new IllegalArgumentException(file.getName() + " has unknown custom button type: " + type);
            }
            validateSlots(file, configuration, buttonPath, size);
            validateItemMaterial(file, configuration, buttonPath + ".item");
            if (configuration.contains(buttonPath + ".else")) {
                validateItemMaterial(file, configuration, buttonPath + ".else.item");
            }
        }
    }

    private void validateSlots(File file, YamlConfiguration configuration, String path, int size) {
        if (configuration.contains(path + ".slot")) {
            String value = String.valueOf(configuration.get(path + ".slot"));
            int slot = parseSlot(value.contains("-") ? value.substring(value.indexOf('-') + 1) : value);
            if (slot < 0 || slot >= size) {
                throw new IllegalArgumentException(file.getName() + " has out-of-range slot at " + path + ": " + slot);
            }
        }
        for (String slotValue : configuration.getStringList(path + ".slots")) {
            String[] split = slotValue.split("-");
            int start = parseSlot(split[0]);
            int end = parseSlot(split.length > 1 ? split[1] : split[0]);
            if (start < 0 || end < start || end >= size) {
                throw new IllegalArgumentException(file.getName() + " has out-of-range slots at " + path + ": " + slotValue);
            }
        }
    }

    private void validateItemMaterial(File file, YamlConfiguration configuration, String path) {
        String material = configuration.getString(path + ".material", null);
        String playerHead = configuration.getString(path + ".playerHead", null);
        String url = configuration.getString(path + ".url", null);
        if (material == null || material.trim().isEmpty()) {
            return;
        }
        if (material.indexOf('%') >= 0 || playerHead != null || url != null) {
            return;
        }
        validateMaterial(material, file.getName() + " at " + path);
    }

    private static int parseSlot(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid slot value: " + value);
        }
    }
}
