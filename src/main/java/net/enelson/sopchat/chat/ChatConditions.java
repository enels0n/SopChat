package net.enelson.sopchat.chat;

import net.enelson.sopchat.SopChatPlugin;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class ChatConditions {

    private final boolean any;
    private final List<ChatCondition> checks;

    public ChatConditions(boolean any, List<ChatCondition> checks) {
        this.any = any;
        this.checks = checks == null ? Collections.<ChatCondition>emptyList() : new ArrayList<ChatCondition>(checks);
    }

    public static ChatConditions alwaysAllowed() {
        return new ChatConditions(false, Collections.<ChatCondition>emptyList());
    }

    public boolean test(SopChatPlugin plugin, Player player) {
        if (checks.isEmpty()) {
            return true;
        }
        if (any) {
            for (ChatCondition check : checks) {
                if (check.test(plugin, player)) {
                    return true;
                }
            }
            return false;
        }
        for (ChatCondition check : checks) {
            if (!check.test(plugin, player)) {
                return false;
            }
        }
        return true;
    }

    public static ChatConditions fromSection(ConfigurationSection section) {
        if (section == null) {
            return alwaysAllowed();
        }

        boolean any = "any".equalsIgnoreCase(section.getString("type", "all"));
        List<Map<?, ?>> rawChecks = section.getMapList("checks");
        List<ChatCondition> checks = new ArrayList<ChatCondition>();
        for (Map<?, ?> raw : rawChecks) {
            if (raw == null) {
                continue;
            }
            Object typeValue = raw.get("type");
            ChatConditionType type = ChatConditionType.fromString(typeValue == null ? null : String.valueOf(typeValue));
            if (type == null) {
                continue;
            }
            Object inputValue = raw.get("input");
            Object outputValue = raw.get("output");
            checks.add(new ChatCondition(
                    type,
                    typeValue == null ? "" : String.valueOf(typeValue),
                    inputValue == null ? "" : String.valueOf(inputValue),
                    outputValue == null ? "" : String.valueOf(outputValue)
            ));
        }
        if (checks.isEmpty()) {
            return alwaysAllowed();
        }
        return new ChatConditions(any, checks);
    }
}
