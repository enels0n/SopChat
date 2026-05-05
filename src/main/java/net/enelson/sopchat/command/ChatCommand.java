package net.enelson.sopchat.command;

import net.enelson.sopchat.SopChatPlugin;
import net.enelson.sopchat.channel.PlayerChannel;
import net.enelson.sopchat.format.ChatFormattingService;
import net.enelson.sopchat.guard.ChatGuardResult;
import net.enelson.sopchat.privatechat.DirectMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ChatCommand implements CommandExecutor, TabCompleter {

    private final SopChatPlugin plugin;
    private final Map<UUID, UUID> lastMessaged = new HashMap<UUID, UUID>();

    public ChatCommand(SopChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if ("chat".equals(name)) {
            return handleChat(sender, args);
        }
        if ("channel".equals(name)) {
            return handleChannel(sender, args);
        }
        if ("msg".equals(name)) {
            return handleMessage(sender, args);
        }
        if ("reply".equals(name)) {
            return handleReply(sender, args);
        }
        if ("ignore".equals(name)) {
            return handleIgnore(sender, args, true);
        }
        if ("unignore".equals(name)) {
            return handleIgnore(sender, args, false);
        }
        if ("socialspy".equals(name)) {
            return handleSocialSpy(sender);
        }
        sender.sendMessage(msg("command-not-implemented", "{prefix}&cКоманда ещё не реализована."));
        return true;
    }

    private boolean handleChat(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(msg("usage-chat-reload", "{prefix}&e/chat reload"));
                sender.sendMessage(msg("usage-chat-gui", "{prefix}&e/chat gui"));
                return true;
            }
            plugin.getChatMenuService().openMain((Player) sender);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("sopchat.reload")) {
                sender.sendMessage(msg("no-permission", "{prefix}&cНедостаточно прав."));
                return true;
            }
            plugin.reloadLocalConfigs();
            sender.sendMessage(msg("reload", "{prefix}&aКонфигурация SopChat перезагружена."));
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("gui")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(msg("player-only", "{prefix}&cКоманда доступна только игроку."));
                return true;
            }
            plugin.getChatMenuService().openMain((Player) sender);
            return true;
        }
        if (args.length == 1 && args[0].equalsIgnoreCase("clear")) {
            if (!sender.hasPermission("sopchat.moderation.clear")) {
                sender.sendMessage(msg("no-permission", "{prefix}&cНедостаточно прав."));
                return true;
            }
            for (int index = 0; index < 120; index++) {
                Bukkit.broadcastMessage(" ");
            }
            Bukkit.broadcastMessage(msg("chat-cleared", "{prefix}&eЧат очищен."));
            return true;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("slowmode")) {
            if (!sender.hasPermission("sopchat.moderation.slowmode")) {
                sender.sendMessage(msg("no-permission", "{prefix}&cНедостаточно прав."));
                return true;
            }
            try {
                int seconds = Math.max(0, Integer.parseInt(args[1]));
                plugin.getModerationService().setSlowmodeSeconds(seconds);
                sender.sendMessage(msg("slowmode-set", "{prefix}&eSlowmode установлен на &6{seconds}&e сек.", "{seconds}", Integer.toString(seconds)));
            } catch (NumberFormatException exception) {
                sender.sendMessage(msg("usage-chat-slowmode", "{prefix}&e/chat slowmode <seconds>"));
            }
            return true;
        }
        if (args.length == 1 && (args[0].equalsIgnoreCase("mutechat") || args[0].equalsIgnoreCase("globalmute"))) {
            if (!sender.hasPermission("sopchat.moderation.globalmute")) {
                sender.sendMessage(msg("no-permission", "{prefix}&cНедостаточно прав."));
                return true;
            }
            boolean enabled = plugin.getModerationService().toggleGlobalMute();
            Bukkit.broadcastMessage(msg(enabled ? "global-mute-enabled" : "global-mute-disabled",
                    enabled ? "{prefix}&cГлобальный чат закрыт." : "{prefix}&aГлобальный чат снова открыт."));
            return true;
        }
        sender.sendMessage(msg("usage-chat-reload", "{prefix}&e/chat reload"));
        sender.sendMessage(msg("usage-chat-gui", "{prefix}&e/chat gui"));
        sender.sendMessage(msg("usage-chat-clear", "{prefix}&e/chat clear"));
        sender.sendMessage(msg("usage-chat-slowmode", "{prefix}&e/chat slowmode <seconds>"));
        sender.sendMessage(msg("usage-chat-globalmute", "{prefix}&e/chat mutechat"));
        return true;
    }

    private boolean handleChannel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("player-only", "{prefix}&cКоманда доступна только игроку."));
            return true;
        }
        Player player = (Player) sender;
        if (args.length >= 2 && args[0].equalsIgnoreCase("create")) {
            try {
                PlayerChannel channel = plugin.getChannelService().createChannel(player, args[1]);
                sender.sendMessage(msg("channel-created", "{prefix}&aКанал &e{channel}&a создан.", "{channel}", channel.getName()));
            } catch (SQLException exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("transfer")) {
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(msg("player-not-found", "{prefix}&cИгрок не найден."));
                return true;
            }
            try {
                PlayerChannel channel = plugin.getChannelService().transferOwnership(player, args[1], target);
                sender.sendMessage(msg("channel-owner-transferred", "{prefix}&aВладелец канала &e{channel}&a изменён на &e{player}&a.", "{channel}", channel.getName(), "{player}", target.getName()));
            } catch (SQLException exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("invite")) {
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(msg("player-not-found", "{prefix}&cИгрок не найден."));
                return true;
            }
            try {
                plugin.getChannelService().invite(player, args[1], target);
                sender.sendMessage(msg("channel-invite-sent", "{prefix}&aПриглашение отправлено игроку &e{player}&a.", "{player}", target.getName()));
                target.sendMessage(msg("channel-invite-received", "{prefix}&eВас пригласили в канал &6{channel}&e.", "{channel}", args[1]));
            } catch (SQLException exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("use")) {
            try {
                plugin.getChannelService().setActiveChannel(player, args[1]);
                sender.sendMessage(msg("channel-active-set", "{prefix}&aАктивный канал переключён на &e{channel}&a.", "{channel}", args[1]));
            } catch (SQLException exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        if (args.length >= 1 && (args[0].equalsIgnoreCase("off") || args[0].equalsIgnoreCase("clear"))) {
            plugin.getChannelService().clearActiveChannel(player);
            sender.sendMessage(msg("channel-active-cleared", "{prefix}&eАктивный канал отключён."));
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("leave")) {
            try {
                plugin.getChannelService().leaveChannel(player, args[1]);
                sender.sendMessage(msg("channel-left", "{prefix}&eВы покинули канал &6{channel}&e.", "{channel}", args[1]));
            } catch (SQLException exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("delete")) {
            try {
                plugin.getChannelService().deleteChannel(player, args[1]);
                sender.sendMessage(msg("channel-deleted", "{prefix}&eКанал &6{channel}&e удалён.", "{channel}", args[1]));
            } catch (SQLException exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("kick")) {
            OfflinePlayer target = plugin.getDirectMessageService().findKnownPlayer(args[2]);
            if (target == null) {
                sender.sendMessage(msg("player-not-found", "{prefix}&cИгрок не найден."));
                return true;
            }
            try {
                plugin.getChannelService().kickMember(player, args[1], target);
                sender.sendMessage(msg("channel-kicked", "{prefix}&eИгрок &6{player}&e исключён из канала &6{channel}&e.", "{player}", safeName(target), "{channel}", args[1]));
            } catch (SQLException exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        if (args.length >= 3 && (args[0].equalsIgnoreCase("chat") || args[0].equalsIgnoreCase("say"))) {
            String message = joinArgs(args, 2);
            ChatGuardResult guardResult = plugin.getChatGuardService().check(player, message);
            if (!guardResult.isAllowed()) {
                if (guardResult.getMessage() != null && !guardResult.getMessage().isEmpty()) {
                    sender.sendMessage(guardResult.getMessage());
                }
                return true;
            }
            try {
                plugin.getChannelService().sendChannelMessage(player, args[1], message);
            } catch (SQLException exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("accept")) {
            try {
                PlayerChannel channel = plugin.getChannelService().acceptInvite(player, Long.parseLong(args[1]));
                sender.sendMessage(msg("channel-invite-accepted", "{prefix}&aВы вступили в канал &e{channel}&a.", "{channel}", channel.getName()));
            } catch (Exception exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("deny")) {
            try {
                plugin.getChannelService().denyInvite(player, Long.parseLong(args[1]));
                sender.sendMessage(msg("channel-invite-denied", "{prefix}&eПриглашение отклонено."));
            } catch (Exception exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        sender.sendMessage(msg("usage-channel-create", "{prefix}&e/channel create <name>"));
        sender.sendMessage(msg("usage-channel-transfer", "{prefix}&e/channel transfer <channel> <player>"));
        sender.sendMessage(msg("usage-channel-invite", "{prefix}&e/channel invite <channel> <player>"));
        sender.sendMessage(msg("usage-channel-use", "{prefix}&e/channel use <channel>"));
        sender.sendMessage(msg("usage-channel-off", "{prefix}&e/channel off"));
        sender.sendMessage(msg("usage-channel-leave", "{prefix}&e/channel leave <channel>"));
        sender.sendMessage(msg("usage-channel-delete", "{prefix}&e/channel delete <channel>"));
        sender.sendMessage(msg("usage-channel-kick", "{prefix}&e/channel kick <channel> <player>"));
        sender.sendMessage(msg("usage-channel-chat", "{prefix}&e/channel chat <channel> <message>"));
        sender.sendMessage(msg("usage-channel-accept", "{prefix}&e/channel accept <channelId>"));
        sender.sendMessage(msg("usage-channel-deny", "{prefix}&e/channel deny <channelId>"));
        return true;
    }

    private boolean handleMessage(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("player-only", "{prefix}&cКоманда доступна только игроку."));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(msg("usage-msg", "{prefix}&e/msg <player> <message>"));
            return true;
        }
        Player player = (Player) sender;
        OfflinePlayer target = plugin.getDirectMessageService().findKnownPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(msg("player-not-found", "{prefix}&cИгрок не найден."));
            return true;
        }
        if (!target.isOnline() && !plugin.getConfig().getBoolean("private-messages.allow-offline-messages", true)) {
            sender.sendMessage(msg("player-offline", "{prefix}&cИгрок оффлайн."));
            return true;
        }
        try {
            if (plugin.getPlayerPreferenceService().isIgnoring(target, player)) {
                sender.sendMessage(msg("target-ignores-you", "{prefix}&cЭтот игрок вас игнорирует."));
                return true;
            }
            String message = joinArgs(args, 1);
            ChatGuardResult guardResult = plugin.getChatGuardService().check(player, message);
            if (!guardResult.isAllowed()) {
                if (guardResult.getMessage() != null && !guardResult.getMessage().isEmpty()) {
                    sender.sendMessage(guardResult.getMessage());
                }
                return true;
            }
            sendPrivateMessage(player, target, target.getName() == null ? args[0] : target.getName(), message);
        } catch (SQLException exception) {
            sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
        }
        return true;
    }

    private boolean handleReply(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("player-only", "{prefix}&cКоманда доступна только игроку."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(msg("usage-reply", "{prefix}&e/reply <message>"));
            return true;
        }
        Player player = (Player) sender;
        UUID lastTargetId = lastMessaged.get(player.getUniqueId());
        if (lastTargetId == null) {
            sender.sendMessage(msg("no-reply-target", "{prefix}&cНекому отвечать."));
            return true;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(lastTargetId);
        if (!target.isOnline() && !plugin.getConfig().getBoolean("private-messages.allow-offline-messages", true)) {
            sender.sendMessage(msg("player-offline", "{prefix}&cИгрок оффлайн."));
            return true;
        }
        try {
            if (plugin.getPlayerPreferenceService().isIgnoring(target, player)) {
                sender.sendMessage(msg("target-ignores-you", "{prefix}&cЭтот игрок вас игнорирует."));
                return true;
            }
            String message = joinArgs(args, 0);
            ChatGuardResult guardResult = plugin.getChatGuardService().check(player, message);
            if (!guardResult.isAllowed()) {
                if (guardResult.getMessage() != null && !guardResult.getMessage().isEmpty()) {
                    sender.sendMessage(guardResult.getMessage());
                }
                return true;
            }
            sendPrivateMessage(player, target, target.getName() == null ? "Unknown" : target.getName(), message);
        } catch (SQLException exception) {
            sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
        }
        return true;
    }

    private boolean handleIgnore(CommandSender sender, String[] args, boolean ignore) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("player-only", "{prefix}&cКоманда доступна только игроку."));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(msg(ignore ? "usage-ignore" : "usage-unignore", ignore ? "{prefix}&e/ignore <player>" : "{prefix}&e/unignore <player>"));
            return true;
        }
        Player player = (Player) sender;
        OfflinePlayer target = plugin.getDirectMessageService().findKnownPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(msg("player-not-found", "{prefix}&cИгрок не найден."));
            return true;
        }
        try {
            if (ignore) {
                plugin.getPlayerPreferenceService().addIgnore(player, target);
                sender.sendMessage(msg("ignore-added", "{prefix}&aТеперь вы игнорируете &e{player}&a.", "{player}", safeName(target)));
            } else {
                plugin.getPlayerPreferenceService().removeIgnore(player, target.getUniqueId());
                sender.sendMessage(msg("ignore-removed", "{prefix}&aВы больше не игнорируете &e{player}&a.", "{player}", safeName(target)));
            }
        } catch (SQLException exception) {
            sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
        }
        return true;
    }

    private boolean handleSocialSpy(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(msg("player-only", "{prefix}&cКоманда доступна только игроку."));
            return true;
        }
        Player player = (Player) sender;
        try {
            boolean enabled = plugin.getPlayerPreferenceService().toggleSocialSpy(player);
            sender.sendMessage(msg(enabled ? "socialspy-enabled" : "socialspy-disabled",
                    enabled ? "{prefix}&aSocial spy включён." : "{prefix}&eSocial spy выключен."));
        } catch (SQLException exception) {
            sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        String name = command.getName().toLowerCase(Locale.ROOT);
        if ("chat".equals(name)) {
            if (args.length == 1) {
                return filter(Arrays.asList("reload", "gui", "clear", "slowmode", "mutechat", "globalmute"), args[0]);
            }
            return Collections.<String>emptyList();
        }
        if ("msg".equals(name) || "ignore".equals(name) || "unignore".equals(name)) {
            if (args.length == 1) {
                List<String> result = new ArrayList<String>();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    result.add(player.getName());
                }
                return filter(result, args[0]);
            }
            return Collections.emptyList();
        }
        if ("reply".equals(name) || "socialspy".equals(name)) {
            return Collections.emptyList();
        }
        if (!"channel".equals(name)) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filter(Arrays.asList("create", "transfer", "invite", "use", "off", "clear", "leave", "delete", "kick", "chat", "say", "accept", "deny"), args[0]);
        }
        if (args.length == 2 && sender instanceof Player
                && (args[0].equalsIgnoreCase("transfer") || args[0].equalsIgnoreCase("invite")
                || args[0].equalsIgnoreCase("use")
                || args[0].equalsIgnoreCase("leave") || args[0].equalsIgnoreCase("delete")
                || args[0].equalsIgnoreCase("kick")
                || args[0].equalsIgnoreCase("chat") || args[0].equalsIgnoreCase("say"))) {
            try {
                List<String> result = new ArrayList<String>();
                for (PlayerChannel channel : plugin.getChannelService().findAccessibleChannels((Player) sender)) {
                    result.add(channel.getName());
                }
                return filter(result, args[1]);
            } catch (SQLException ignored) {
                return Collections.emptyList();
            }
        }
        if (args.length == 3 && (args[0].equalsIgnoreCase("transfer") || args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            List<String> result = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                result.add(player.getName());
            }
            return filter(result, args[2]);
        }
        return Collections.emptyList();
    }

    public Set<UUID> getIgnoredPlayerIds(Player player) {
        try {
            return new java.util.HashSet<UUID>(plugin.getPlayerPreferenceService().findIgnoredPlayerIds(player));
        } catch (SQLException ignored) {
            return Collections.emptySet();
        }
    }

    public boolean unignore(Player player, UUID targetId) {
        try {
            boolean removed = plugin.getPlayerPreferenceService().findIgnoredPlayerIds(player).contains(targetId);
            plugin.getPlayerPreferenceService().removeIgnore(player, targetId);
            return removed;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private List<String> filter(List<String> values, String input) {
        List<String> result = new ArrayList<String>();
        String lower = input == null ? "" : input.toLowerCase(Locale.ROOT);
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }

    private String prefix() {
        return plugin.getMessageConfig().get("prefix", "&6SopChat &8| ");
    }

    private String msg(String key, String fallback, String... replacements) {
        String text = plugin.getMessageConfig().get(key, fallback).replace("{prefix}", prefix());
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            text = text.replace(replacements[index], replacements[index + 1]);
        }
        return color(text);
    }

    private String color(String input) {
        ChatFormattingService formatting = plugin.getChatFormattingService();
        return formatting.formatSystemMessage(input);
    }

    private void sendPrivateMessage(Player sender, OfflinePlayer target, String targetName, String message) {
        String outgoing = msg("private-message-outgoing", "{prefix}&8[&aЯ &7-> &f{target}&8] &7{message}", "{target}", targetName, "{message}", message);
        sender.sendMessage(outgoing);
        try {
            String playerFormattedMessage = plugin.getChatFormattingService().formatPlayerMessage(sender, message);
            DirectMessage stored = plugin.getDirectMessageService().saveMessage(sender, target, targetName, playerFormattedMessage);
            if (target.isOnline() && target.getPlayer() != null) {
                Player onlineTarget = target.getPlayer();
                String incoming = msg("private-message-incoming", "{prefix}&8[&f{sender} &7-> &aЯ&8] &7{message}", "{sender}", sender.getName(), "{message}", stored.getMessage());
                onlineTarget.sendMessage(incoming);
                plugin.getDirectMessageService().markConversationRead(onlineTarget, sender.getUniqueId(), stored.getId());
            }
            lastMessaged.put(sender.getUniqueId(), target.getUniqueId());
            lastMessaged.put(target.getUniqueId(), sender.getUniqueId());
            notifySocialSpy(sender, targetName, target.getUniqueId(), stored.getMessage());
        } catch (SQLException exception) {
            sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
        }
    }

    private void notifySocialSpy(Player sender, String targetName, UUID targetUuid, String message) {
        String spyMessage = msg("private-message-spy", "{prefix}&8[SPY] &f{sender} &7-> &f{target}&8: &7{message}",
                "{sender}", sender.getName(),
                "{target}", targetName,
                "{message}", message);
        List<UUID> watchers;
        try {
            watchers = plugin.getPlayerPreferenceService().findSocialSpyEnabled();
        } catch (SQLException exception) {
            sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            return;
        }
        for (UUID uniqueId : watchers) {
            Player online = Bukkit.getPlayer(uniqueId);
            if (online == null) {
                continue;
            }
            if (online.getUniqueId().equals(sender.getUniqueId()) || online.getUniqueId().equals(targetUuid)) {
                continue;
            }
            online.sendMessage(spyMessage);
        }
    }

    private String safeName(OfflinePlayer player) {
        return player.getName() == null ? player.getUniqueId().toString() : player.getName();
    }

    private String joinArgs(String[] args, int startIndex) {
        StringBuilder builder = new StringBuilder();
        for (int index = startIndex; index < args.length; index++) {
            if (index > startIndex) {
                builder.append(' ');
            }
            builder.append(args[index]);
        }
        return builder.toString();
    }
}
