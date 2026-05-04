package net.enelson.sopchat.chat;

import net.enelson.sopchat.SopChatPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ChatTypeService {

    private final SopChatPlugin plugin;
    private final Map<String, ChatTypeDefinition> types = new LinkedHashMap<String, ChatTypeDefinition>();
    private final List<ChatTypeDefinition> sortedByTriggerLength = new ArrayList<ChatTypeDefinition>();

    public ChatTypeService(SopChatPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        this.types.clear();
        this.sortedByTriggerLength.clear();

        YamlConfiguration config = this.plugin.getChatTypesConfig();
        ConfigurationSection section = config.getConfigurationSection("chat-types");
        if (section == null) {
            return;
        }

        for (String id : section.getKeys(false)) {
            ConfigurationSection entry = section.getConfigurationSection(id);
            if (entry == null) {
                continue;
            }
            ChatTypeDefinition definition = new ChatTypeDefinition(
                    id,
                    entry.getString("trigger", ""),
                    ChatTypeMode.valueOf(entry.getString("mode", "radius").toUpperCase(Locale.ROOT)),
                    entry.getInt("radius", 100),
                    entry.getString("permission", ""),
                    entry.getBoolean("deny-if-no-permission", false),
                    entry.getBoolean("mention-enabled", true),
                    entry.getString("format", "{player}: {message}")
            );
            this.types.put(id.toLowerCase(Locale.ROOT), definition);
            this.sortedByTriggerLength.add(definition);
        }

        Collections.sort(this.sortedByTriggerLength, new Comparator<ChatTypeDefinition>() {
            @Override
            public int compare(ChatTypeDefinition left, ChatTypeDefinition right) {
                return Integer.compare(right.getTrigger().length(), left.getTrigger().length());
            }
        });
    }

    public ChatRoute resolveRoute(String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage;
        for (ChatTypeDefinition definition : this.sortedByTriggerLength) {
            String trigger = definition.getTrigger();
            if (trigger.isEmpty()) {
                continue;
            }
            if (message.startsWith(trigger)) {
                return new ChatRoute(definition, message.substring(trigger.length()).trim());
            }
        }

        ChatTypeDefinition normal = this.types.get("normal");
        if (normal == null && !this.sortedByTriggerLength.isEmpty()) {
            normal = this.sortedByTriggerLength.get(this.sortedByTriggerLength.size() - 1);
        }
        return normal == null ? null : new ChatRoute(normal, message.trim());
    }

    public List<ChatTypeDefinition> getTypes() {
        return Collections.unmodifiableList(this.sortedByTriggerLength);
    }

    public ChatTypeDefinition getType(String id) {
        if (id == null) {
            return null;
        }
        return this.types.get(id.toLowerCase(Locale.ROOT));
    }
}
