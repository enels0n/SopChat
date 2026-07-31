package net.enelson.sopchat.chat;

import net.enelson.sopchat.SopChatPlugin;
import org.bukkit.entity.Player;

public final class ChatCondition {

    private final ChatConditionType type;
    private final String typeId;
    private final String input;
    private final String output;

    public ChatCondition(ChatConditionType type, String typeId, String input, String output) {
        this.type = type;
        this.typeId = typeId == null ? "" : typeId;
        this.input = input == null ? "" : input;
        this.output = output == null ? "" : output;
    }

    public boolean test(SopChatPlugin plugin, Player player) {
        if (type == null) {
            return true;
        }

        String resolvedInput = plugin.resolveConditionPlaceholders(player, input).trim();
        String resolvedOutput = plugin.resolveConditionPlaceholders(player, output).trim();
        if (isUnavailableNumericPlaceholder(input, resolvedInput) || isUnavailableNumericPlaceholder(output, resolvedOutput)) {
            return false;
        }

        switch (type) {
            case HAS_PERM:
                return player != null && player.hasPermission(input);
            case HAS_NO_PERM:
                return player == null || !player.hasPermission(input);
            case STRING_EQUALS:
                return resolvedInput.equalsIgnoreCase(resolvedOutput);
            case STRING_NOT_EQUALS:
                return !resolvedInput.equalsIgnoreCase(resolvedOutput);
            case NUMBER_GREATER_OR_EQUALS:
            case NUMBER_GREATER:
            case NUMBER_LESS_OR_EQUALS:
            case NUMBER_LESS:
            case NUMBER_EQUALS:
            case NUMBER_NOT_EQUALS:
                return compareNumbers(resolvedInput, resolvedOutput, type);
            default:
                return true;
        }
    }

    private boolean isUnavailableNumericPlaceholder(String original, String resolved) {
        if (original == null || resolved == null) {
            return false;
        }
        if (!original.contains("%") && !original.contains("{")) {
            return false;
        }
        if (resolved.contains("%") || resolved.contains("{")) {
            return true;
        }
        return "0".equals(resolved.trim());
    }

    private boolean compareNumbers(String inputValue, String outputValue, ChatConditionType comparisonType) {
        try {
            double left = Double.parseDouble(inputValue);
            double right = Double.parseDouble(outputValue);
            int result = Double.compare(left, right);
            switch (comparisonType) {
                case NUMBER_GREATER_OR_EQUALS:
                    return result >= 0;
                case NUMBER_GREATER:
                    return result > 0;
                case NUMBER_LESS_OR_EQUALS:
                    return result <= 0;
                case NUMBER_LESS:
                    return result < 0;
                case NUMBER_EQUALS:
                    return result == 0;
                case NUMBER_NOT_EQUALS:
                    return result != 0;
                default:
                    return false;
            }
        } catch (NumberFormatException exception) {
            return false;
        }
    }
}
