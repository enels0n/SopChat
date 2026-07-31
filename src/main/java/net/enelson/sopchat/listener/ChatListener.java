package net.enelson.sopchat.listener;

import me.clip.placeholderapi.PlaceholderAPI;
import net.enelson.sopchat.SopChatPlugin;
import net.enelson.sopchat.chat.ChatRoute;
import net.enelson.sopchat.chat.ChatTypeDefinition;
import net.enelson.sopchat.chat.ChatTypeMode;
import net.enelson.sopchat.guard.ChatGuardResult;
import net.enelson.sopchat.mention.MentionResult;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ChatListener implements Listener {

    private final SopChatPlugin plugin;

    public ChatListener(SopChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!this.plugin.getChatTypeService().hasExplicitTrigger(event.getMessage())) {
            try {
                net.enelson.sopchat.channel.PlayerChannel activeChannel = this.plugin.getChannelService().getActiveChannel(player);
                if (activeChannel != null) {
                    event.setCancelled(true);
                    ChatGuardResult guardResult = this.plugin.getChatGuardService().check(player, event.getMessage());
                    if (!guardResult.isAllowed()) {
                        if (guardResult.getMessage() != null && !guardResult.getMessage().isEmpty()) {
                            player.sendMessage(guardResult.getMessage());
                        }
                        return;
                    }
                    this.plugin.getChannelService().sendChannelMessage(player, activeChannel.getName(), event.getMessage());
                    return;
                }
            } catch (Exception exception) {
                player.sendMessage(this.plugin.getChatFormattingService().formatSystemMessage(
                        (this.plugin.getMessageConfig().get("prefix", "&6SopChat &8| ") + "&c" + exception.getMessage())
                ));
                event.setCancelled(true);
                return;
            }
        }
        ChatRoute route = this.plugin.getChatTypeService().resolveRoute(event.getMessage());
        if (route == null) {
            player.sendMessage(this.plugin.getChatFormattingService().formatSystemMessage(
                    this.plugin.getMessageConfig().get("unknown-chat-type", "{prefix}&cНе удалось определить тип чата.")
                            .replace("{prefix}", this.plugin.getMessageConfig().get("prefix", "&6SopChat &8| "))
            ));
            event.setCancelled(true);
            return;
        }

        ChatTypeDefinition type = route.getType();
        if (!isAllowed(player, type)) {
            player.sendMessage(this.plugin.getChatFormattingService().formatSystemMessage(
                    this.plugin.getMessageConfig().get("no-permission", "{prefix}&cНедостаточно прав.")
                            .replace("{prefix}", this.plugin.getMessageConfig().get("prefix", "&6SopChat &8| "))
            ));
            event.setCancelled(true);
            return;
        }

        if (!type.getConditions().test(this.plugin, player)) {
            player.sendMessage(this.plugin.getChatFormattingService().formatSystemMessage(
                    this.plugin.getMessageConfig().get("chat-type-unavailable", "{prefix}&cThis chat type is unavailable here.")
                            .replace("{prefix}", this.plugin.getMessageConfig().get("prefix", "&6SopChat &8| "))
            ));
            event.setCancelled(true);
            return;
        }

        String content = route.getContent();
        if (content.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        ChatGuardResult guardResult = this.plugin.getChatGuardService().check(player, content);
        if (!guardResult.isAllowed()) {
            event.setCancelled(true);
            if (guardResult.getMessage() != null && !guardResult.getMessage().isEmpty()) {
                player.sendMessage(guardResult.getMessage());
            }
            return;
        }

        String messageContent = canUseMessagePlaceholders(player) ? applyPlaceholders(player, content) : content;
        String sanitizedMessage = this.plugin.getChatFormattingService().sanitizePlayerMessage(player, messageContent);
        String formattedTemplate = applyPlaceholders(player, type.getFormat());
        String templatePrefix = formattedTemplate
                .replace("{player}", player.getName())
                .replace("{chat_type}", type.getId())
                .replace("{message}", "");
        String baseMessageColors = org.bukkit.ChatColor.getLastColors(
                this.plugin.getChatFormattingService().formatSystemMessage(templatePrefix)
        );
        MentionResult mentionResult = type.isMentionEnabled()
                ? this.plugin.getMentionService().processMentions(player, sanitizedMessage, baseMessageColors)
                : new MentionResult(sanitizedMessage, java.util.Collections.<java.util.UUID>emptySet());
        String formattedMessage = this.plugin.getChatFormattingService().formatPlayerMessage(player, mentionResult.getMessage());
        String output = formattedTemplate
                .replace("{player}", player.getName())
                .replace("{message}", formattedMessage)
                .replace("{chat_type}", type.getId());

        event.setCancelled(true);
        for (Player recipient : resolveRecipients(player, type)) {
            if (!type.getVisibilityConditions().test(this.plugin, recipient)) {
                continue;
            }
            recipient.sendMessage(this.plugin.getChatFormattingService().formatSystemMessage(output));
            if (mentionResult.getMentionedPlayers().contains(recipient.getUniqueId())) {
                this.plugin.getMentionService().playMentionSound(recipient);
            }
        }
        Bukkit.getConsoleSender().sendMessage(this.plugin.getChatFormattingService().formatSystemMessage(output));
    }

    private boolean isAllowed(Player player, ChatTypeDefinition type) {
        String permission = type.getPermission();
        if (permission == null || permission.isEmpty()) {
            return true;
        }
        if (player.hasPermission(permission)) {
            return true;
        }
        return !type.isDenyIfNoPermission();
    }

    private Set<Player> resolveRecipients(Player sender, ChatTypeDefinition type) {
        Set<Player> recipients = new LinkedHashSet<Player>();
        if (type.getMode() == ChatTypeMode.GLOBAL) {
            recipients.addAll(Bukkit.getOnlinePlayers());
            return recipients;
        }
        if (type.getMode() == ChatTypeMode.WORLD) {
            recipients.addAll(sender.getWorld().getPlayers());
            return recipients;
        }

        double maxDistanceSquared = Math.max(0, type.getRadius()) * (double) Math.max(0, type.getRadius());
        for (Player online : sender.getWorld().getPlayers()) {
            if (online.getLocation().distanceSquared(sender.getLocation()) <= maxDistanceSquared) {
                recipients.add(online);
            }
        }
        return recipients;
    }

    private String applyPlaceholders(Player player, String input) {
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                return PlaceholderAPI.setPlaceholders(player, input);
            } catch (Throwable ignored) {
            }
        }
        return input;
    }

    private boolean canUseMessagePlaceholders(Player player) {
        if (!this.plugin.getConfig().getBoolean("formatting.allow-message-placeholders", true)) {
            return false;
        }
        String permission = this.plugin.getConfig().getString("formatting.permissions.placeholders", "");
        return permission == null || permission.isEmpty() || player.hasPermission(permission);
    }
}
