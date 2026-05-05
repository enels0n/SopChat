package net.enelson.sopchat.mention;

import me.clip.placeholderapi.PlaceholderAPI;
import net.enelson.sopchat.SopChatPlugin;
import net.enelson.sopli.lib.text.TextUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MentionService {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("(?<![A-Za-z0-9_])([A-Za-z0-9_]{3,32})(?![A-Za-z0-9_])");

    private final SopChatPlugin plugin;
    private final TextUtils textUtils;

    public MentionService(SopChatPlugin plugin, TextUtils textUtils) {
        this.plugin = plugin;
        this.textUtils = textUtils;
    }

    public MentionResult processMentions(Player sender, String formattedMessage) {
        return processMentions(sender, formattedMessage, "");
    }

    public MentionResult processMentions(Player sender, String formattedMessage, String baseMessageColors) {
        if (!this.plugin.getConfig().getBoolean("mentions.enabled", true) || formattedMessage == null || formattedMessage.isEmpty()) {
            return new MentionResult(formattedMessage, new HashSet<UUID>());
        }

        Matcher matcher = TOKEN_PATTERN.matcher(formattedMessage);
        StringBuffer buffer = new StringBuffer();
        boolean changed = false;
        Set<UUID> mentioned = new HashSet<UUID>();

        while (matcher.find()) {
            String token = matcher.group(1);
            MentionTarget target = findTarget(sender, token);
            if (target == null) {
                continue;
            }
            String restoreFormat = resolveRestoreFormat(baseMessageColors, formattedMessage, matcher.end());
            String replacement = Matcher.quoteReplacement(target.getFormattedValue() + restoreFormat);
            matcher.appendReplacement(buffer, replacement);
            changed = true;
            mentioned.add(target.getUniqueId());
        }

        if (!changed) {
            return new MentionResult(formattedMessage, mentioned);
        }

        matcher.appendTail(buffer);
        return new MentionResult(buffer.toString(), mentioned);
    }

    public void playMentionSound(final Player receiver) {
        if (receiver == null || !receiver.isOnline() || !this.plugin.getConfig().getBoolean("mentions.play-sound", true)) {
            return;
        }
        final Sound sound = resolveSound(this.plugin.getConfig().getString("mentions.sound", "ENTITY_EXPERIENCE_ORB_PICKUP"));
        final float volume = (float) this.plugin.getConfig().getDouble("mentions.volume", 1.0D);
        final float pitch = (float) this.plugin.getConfig().getDouble("mentions.pitch", 1.0D);
        Bukkit.getScheduler().runTask(this.plugin, new Runnable() {
            @Override
            public void run() {
                receiver.playSound(receiver.getLocation(), sound, volume, pitch);
            }
        });
    }

    private MentionTarget findTarget(Player sender, String token) {
        List<MentionTarget> targets = collectTargets(sender);
        String expected = normalize(token);
        for (MentionTarget target : targets) {
            if (normalize(target.getMatchValue()).equals(expected)) {
                return target;
            }
        }
        return null;
    }

    private List<MentionTarget> collectTargets(Player sender) {
        FileConfiguration config = this.plugin.getConfig();
        boolean hideSelfMention = config.getBoolean("mentions.hide-self-mention", true);
        boolean matchDisplayName = config.getBoolean("mentions.match-display-name", true);
        boolean ignoreVanishedPlayers = config.getBoolean("mentions.ignore-vanished-players", true);

        List<MentionTarget> targets = new ArrayList<MentionTarget>();
        for (Player target : Bukkit.getOnlinePlayers()) {
            if (hideSelfMention && sender.getUniqueId().equals(target.getUniqueId())) {
                continue;
            }
            if (ignoreVanishedPlayers && sender.spigot().getHiddenPlayers().contains(target)) {
                continue;
            }
            targets.add(new MentionTarget(target.getUniqueId(), target.getName(), formatTarget(target, false)));
            if (matchDisplayName) {
                String plainDisplayName = ChatColor.stripColor(target.getDisplayName());
                if (plainDisplayName != null && !plainDisplayName.trim().isEmpty() && !plainDisplayName.equalsIgnoreCase(target.getName())) {
                    targets.add(new MentionTarget(target.getUniqueId(), plainDisplayName, formatTarget(target, true)));
                }
            }
        }

        targets.sort(Comparator.comparingInt(new java.util.function.ToIntFunction<MentionTarget>() {
            @Override
            public int applyAsInt(MentionTarget value) {
                return value.getMatchValue().length();
            }
        }).reversed());
        return targets;
    }

    private String formatTarget(Player target, boolean displayNameVariant) {
        String template = displayNameVariant
                ? this.plugin.getConfig().getString("mentions.display-name-mask", "&e@{display_name}")
                : this.plugin.getConfig().getString("mentions.mask", "&e@{player}");
        String displayName = target.getDisplayName();
        String plainDisplayName = ChatColor.stripColor(displayName);

        String result = template
                .replace("{player}", target.getName())
                .replace("{player_name}", target.getName())
                .replace("{display_name}", displayName)
                .replace("{plain_display_name}", plainDisplayName == null ? target.getName() : plainDisplayName);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                result = PlaceholderAPI.setPlaceholders(target, result);
            } catch (Throwable ignored) {
            }
        }
        return this.textUtils.color(result);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return this.plugin.getConfig().getBoolean("mentions.ignore-case", true)
                ? value.toLowerCase(Locale.ROOT)
                : value;
    }

    private String resolveRestoreFormat(String baseMessageColors, String originalMessage, int endIndex) {
        String visiblePrefix = originalMessage.substring(0, Math.min(endIndex, originalMessage.length()));
        String restore = ChatColor.getLastColors((baseMessageColors == null ? "" : baseMessageColors) + visiblePrefix);
        if (restore == null) {
            restore = "";
        }
        return ChatColor.RESET.toString() + restore;
    }

    private Sound resolveSound(String raw) {
        try {
            Sound sound = Sound.valueOf(raw.toUpperCase(Locale.ROOT));
            return sound == null ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : sound;
        } catch (Exception ignored) {
            return Sound.ENTITY_EXPERIENCE_ORB_PICKUP;
        }
    }

    private static final class MentionTarget {
        private final UUID uniqueId;
        private final String matchValue;
        private final String formattedValue;

        private MentionTarget(UUID uniqueId, String matchValue, String formattedValue) {
            this.uniqueId = uniqueId;
            this.matchValue = matchValue;
            this.formattedValue = formattedValue;
        }

        private UUID getUniqueId() {
            return uniqueId;
        }

        private String getMatchValue() {
            return matchValue;
        }

        private String getFormattedValue() {
            return formattedValue;
        }
    }
}
