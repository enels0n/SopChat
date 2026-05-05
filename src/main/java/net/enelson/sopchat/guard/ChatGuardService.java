package net.enelson.sopchat.guard;

import net.enelson.sopchat.SopChatPlugin;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

public final class ChatGuardService {

    private static final Pattern IPV4_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern DOMAIN_PATTERN = Pattern.compile("(?i)\\b(?:[a-z0-9-]+\\.)+[a-z]{2,}\\b");

    private final SopChatPlugin plugin;
    private final Map<UUID, Long> lastMessageAt = new HashMap<UUID, Long>();
    private final Map<UUID, LastMessageData> lastMessageData = new HashMap<UUID, LastMessageData>();

    public ChatGuardService(SopChatPlugin plugin) {
        this.plugin = plugin;
    }

    public ChatGuardResult check(Player player, String rawMessage) {
        String message = rawMessage == null ? "" : rawMessage;

        ChatGuardResult globalMute = checkGlobalMute(player);
        if (!globalMute.isAllowed()) {
            return globalMute;
        }

        ChatGuardResult flood = checkFlood(player);
        if (!flood.isAllowed()) {
            return flood;
        }

        ChatGuardResult repeat = checkRepeat(player, message);
        if (!repeat.isAllowed()) {
            return repeat;
        }

        ChatGuardResult spam = checkSpam(player, message);
        if (!spam.isAllowed()) {
            return spam;
        }

        recordMessage(player, message);
        return ChatGuardResult.allowed();
    }

    private ChatGuardResult checkGlobalMute(Player player) {
        if (!plugin.getModerationService().isGlobalMute()) {
            return ChatGuardResult.allowed();
        }
        if (player.hasPermission("sopchat.moderation.bypass")) {
            return ChatGuardResult.allowed();
        }
        String text = plugin.getMessageConfig().get("global-mute", "{prefix}&cЧат временно закрыт.")
                .replace("{prefix}", plugin.getMessageConfig().get("prefix", "&6SopChat &8| "));
        return ChatGuardResult.denied(plugin.getChatFormattingService().formatSystemMessage(text));
    }

    private ChatGuardResult checkFlood(Player player) {
        if (!plugin.getConfig().getBoolean("anti-flood.enabled", true)) {
            return ChatGuardResult.allowed();
        }
        String bypassPermission = plugin.getConfig().getString("anti-flood.bypass-permission", "sopchat.antiflood.bypass");
        if (bypassPermission != null && !bypassPermission.isEmpty() && player.hasPermission(bypassPermission)) {
            return ChatGuardResult.allowed();
        }

        double cooldownSeconds = plugin.getModerationService().getSlowmodeSeconds() > 0
                ? plugin.getModerationService().getSlowmodeSeconds()
                : plugin.getConfig().getDouble("anti-flood.message-cooldown-seconds", 1.5D);
        long cooldownMs = Math.max(0L, Math.round(cooldownSeconds * 1000.0D));
        Long lastAt = lastMessageAt.get(player.getUniqueId());
        long now = System.currentTimeMillis();
        if (lastAt != null && now - lastAt.longValue() < cooldownMs) {
            long remainingMs = cooldownMs - (now - lastAt.longValue());
            double remainingSeconds = remainingMs / 1000.0D;
            String text = plugin.getMessageConfig().get("anti-flood", "{prefix}&cПодождите &e{seconds}&c сек. перед следующим сообщением.")
                    .replace("{prefix}", plugin.getMessageConfig().get("prefix", "&6SopChat &8| "))
                    .replace("{seconds}", String.format(Locale.US, "%.1f", remainingSeconds));
            return ChatGuardResult.denied(plugin.getChatFormattingService().formatSystemMessage(text));
        }
        return ChatGuardResult.allowed();
    }

    private ChatGuardResult checkRepeat(Player player, String message) {
        if (!plugin.getConfig().getBoolean("anti-repeat.enabled", true)) {
            return ChatGuardResult.allowed();
        }
        LastMessageData data = lastMessageData.get(player.getUniqueId());
        if (data == null) {
            return ChatGuardResult.allowed();
        }
        String normalizedCurrent = normalizeForRepeat(message);
        if (!normalizedCurrent.equals(data.normalizedMessage)) {
            return ChatGuardResult.allowed();
        }
        long delayMs = Math.max(0L, Math.round(plugin.getConfig().getDouble("anti-repeat.same-message-delay-seconds", 15.0D) * 1000.0D));
        long now = System.currentTimeMillis();
        if (now - data.sentAt < delayMs) {
            String text = plugin.getMessageConfig().get("anti-repeat", "{prefix}&cНельзя повторять одно и то же сообщение так часто.")
                    .replace("{prefix}", plugin.getMessageConfig().get("prefix", "&6SopChat &8| "));
            return ChatGuardResult.denied(plugin.getChatFormattingService().formatSystemMessage(text));
        }
        return ChatGuardResult.allowed();
    }

    private ChatGuardResult checkSpam(Player player, String message) {
        if (!plugin.getConfig().getBoolean("anti-spam.enabled", true)) {
            return ChatGuardResult.allowed();
        }
        String bypassPermission = plugin.getConfig().getString("anti-spam.bypass-permission", "sopchat.antispam.bypass");
        if (bypassPermission != null && !bypassPermission.isEmpty() && player.hasPermission(bypassPermission)) {
            return ChatGuardResult.allowed();
        }

        String lower = message.toLowerCase(Locale.ROOT);
        List<String> whitelist = plugin.getConfig().getStringList("anti-spam.whitelist");
        if (containsWhitelisted(lower, whitelist)) {
            return ChatGuardResult.allowed();
        }

        if (plugin.getConfig().getBoolean("anti-spam.block-ipv4", true) && IPV4_PATTERN.matcher(message).find()) {
            return deniedSpam();
        }
        if (plugin.getConfig().getBoolean("anti-spam.block-domains", true) && DOMAIN_PATTERN.matcher(message).find()) {
            return deniedSpam();
        }
        for (String blocked : plugin.getConfig().getStringList("anti-spam.blacklist")) {
            if (blocked != null && !blocked.isEmpty() && lower.contains(blocked.toLowerCase(Locale.ROOT))) {
                return deniedSpam();
            }
        }
        for (String regex : plugin.getConfig().getStringList("anti-spam.regex-rules")) {
            if (regex == null || regex.isEmpty()) {
                continue;
            }
            try {
                if (Pattern.compile(regex).matcher(message).find()) {
                    return deniedSpam();
                }
            } catch (Exception ignored) {
            }
        }
        return ChatGuardResult.allowed();
    }

    private ChatGuardResult deniedSpam() {
        String text = plugin.getMessageConfig().get("anti-spam", "{prefix}&cЭто сообщение похоже на спам и было заблокировано.")
                .replace("{prefix}", plugin.getMessageConfig().get("prefix", "&6SopChat &8| "));
        return ChatGuardResult.denied(plugin.getChatFormattingService().formatSystemMessage(text));
    }

    private boolean containsWhitelisted(String message, List<String> whitelist) {
        if (whitelist == null) {
            return false;
        }
        for (String allowed : whitelist) {
            if (allowed != null && !allowed.isEmpty() && message.contains(allowed.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private void recordMessage(Player player, String message) {
        long now = System.currentTimeMillis();
        lastMessageAt.put(player.getUniqueId(), Long.valueOf(now));
        lastMessageData.put(player.getUniqueId(), new LastMessageData(normalizeForRepeat(message), now));
    }

    private String normalizeForRepeat(String message) {
        String result = message == null ? "" : message;
        if (plugin.getConfig().getBoolean("anti-repeat.normalize-case", true)) {
            result = result.toLowerCase(Locale.ROOT);
        }
        if (plugin.getConfig().getBoolean("anti-repeat.trim-extra-spaces", true)) {
            result = result.trim().replaceAll("\\s+", " ");
        }
        return result;
    }

    private static final class LastMessageData {
        private final String normalizedMessage;
        private final long sentAt;

        private LastMessageData(String normalizedMessage, long sentAt) {
            this.normalizedMessage = normalizedMessage;
            this.sentAt = sentAt;
        }
    }
}
