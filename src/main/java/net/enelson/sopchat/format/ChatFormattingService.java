package net.enelson.sopchat.format;

import net.enelson.sopchat.SopChatPlugin;
import net.enelson.sopli.lib.text.TextUtils;
import org.bukkit.entity.Player;

import java.util.regex.Pattern;

public final class ChatFormattingService {

    private static final Pattern LEGACY_COLOR_PATTERN = Pattern.compile("(?i)&[0-9A-F]");
    private static final Pattern LEGACY_STYLE_PATTERN = Pattern.compile("(?i)&[LMNO]");
    private static final Pattern LEGACY_MAGIC_PATTERN = Pattern.compile("(?i)&K");
    private static final Pattern HEX_PATTERN = Pattern.compile("(?i)&#[A-F0-9]{6}");
    private static final Pattern MINI_MESSAGE_PATTERN = Pattern.compile("<[^\\n>]+>");

    private final SopChatPlugin plugin;
    private final TextUtils textUtils;

    public ChatFormattingService(SopChatPlugin plugin, TextUtils textUtils) {
        this.plugin = plugin;
        this.textUtils = textUtils;
    }

    public String formatPlayerMessage(Player player, String input) {
        String filtered = stripUnauthorizedFormatting(player, input == null ? "" : input);
        return this.textUtils.color(filtered);
    }

    public String sanitizePlayerMessage(Player player, String input) {
        return stripUnauthorizedFormatting(player, input == null ? "" : input);
    }

    public String formatSystemMessage(String input) {
        return this.textUtils.color(input);
    }

    private String stripUnauthorizedFormatting(Player player, String input) {
        String result = input;
        if (!canUse(player, "allow-colors", "color")) {
            result = LEGACY_COLOR_PATTERN.matcher(result).replaceAll("");
        }
        if (!canUse(player, "allow-styles", "style")) {
            result = LEGACY_STYLE_PATTERN.matcher(result).replaceAll("");
        }
        if (!canUse(player, "allow-magic", "magic")) {
            result = LEGACY_MAGIC_PATTERN.matcher(result).replaceAll("");
        }
        if (!canUse(player, "allow-hex", "hex")) {
            result = HEX_PATTERN.matcher(result).replaceAll("");
        }
        if (!canUse(player, "allow-minimessage", "minimessage")) {
            result = MINI_MESSAGE_PATTERN.matcher(result).replaceAll("");
        }
        return result;
    }

    private boolean canUse(Player player, String flagKey, String permissionKey) {
        if (!this.plugin.getConfig().getBoolean("formatting." + flagKey, true)) {
            return false;
        }
        String permission = this.plugin.getConfig().getString("formatting.permissions." + permissionKey, "");
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }
}
