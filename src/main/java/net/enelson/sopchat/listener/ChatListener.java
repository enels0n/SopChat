package net.enelson.sopchat.listener;

import me.clip.placeholderapi.PlaceholderAPI;
import net.enelson.sopchat.SopChatPlugin;
import net.enelson.sopchat.chat.ChatRoute;
import net.enelson.sopchat.chat.ChatTypeDefinition;
import net.enelson.sopchat.chat.ChatTypeMode;
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
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

        String content = route.getContent();
        if (content.isEmpty()) {
            event.setCancelled(true);
            return;
        }

        String messageContent = canUseMessagePlaceholders(player) ? applyPlaceholders(player, content) : content;
        String formattedMessage = this.plugin.getChatFormattingService().formatPlayerMessage(player, messageContent);
        String formattedTemplate = applyPlaceholders(player, type.getFormat());
        String output = formattedTemplate
                .replace("{player}", player.getName())
                .replace("{message}", formattedMessage)
                .replace("{chat_type}", type.getId());

        event.setCancelled(true);
        for (Player recipient : resolveRecipients(player, type)) {
            recipient.sendMessage(this.plugin.getChatFormattingService().formatSystemMessage(output));
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
