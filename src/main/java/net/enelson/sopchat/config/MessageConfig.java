package net.enelson.sopchat.config;

import org.bukkit.configuration.file.YamlConfiguration;

public final class MessageConfig {

    private final YamlConfiguration config;

    public MessageConfig(YamlConfiguration config) {
        this.config = config;
    }

    public String get(String key, String fallback) {
        return this.config.getString("messages." + key, fallback);
    }
}
