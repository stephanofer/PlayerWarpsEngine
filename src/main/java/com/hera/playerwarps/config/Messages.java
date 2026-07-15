package com.hera.playerwarps.config;

import com.hera.playerwarps.util.Texts;
import dev.dejvokep.boostedyaml.YamlDocument;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class Messages {

    private final YamlDocument document;
    private final String prefix;

    private Messages(YamlDocument document, String prefix) {
        this.document = document;
        this.prefix = prefix;
    }

    public static Messages from(YamlDocument document) {
        return new Messages(document, document.getString("messages.prefix", "&8[&bPlayerWarps&8] "));
    }

    public String prefix() {
        return this.prefix;
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(format(this.document.getString(path, ""), Collections.<String, String>emptyMap()));
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(format(this.document.getString(path, ""), placeholders));
    }

    public void sendList(CommandSender sender, String path, Map<String, String> placeholders) {
        for (String line : this.document.getStringList(path, Collections.<String>emptyList())) {
            sender.sendMessage(format(line, placeholders));
        }
    }

    public List<String> formatList(String path, Map<String, String> placeholders) {
        List<String> formatted = new ArrayList<String>();
        for (String line : this.document.getStringList(path, Collections.<String>emptyList())) {
            formatted.add(format(line, placeholders));
        }
        return formatted;
    }

    public String format(String text, Map<String, String> placeholders) {
        String formatted = text.replace("%prefix%", this.prefix);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            formatted = formatted.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return Texts.color(formatted);
    }
}
