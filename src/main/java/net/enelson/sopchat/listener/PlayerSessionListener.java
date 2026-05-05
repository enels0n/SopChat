package net.enelson.sopchat.listener;

import net.enelson.sopchat.SopChatPlugin;
import net.enelson.sopchat.channel.PlayerChannel;
import net.enelson.sopchat.privatechat.DirectConversationSummary;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.SQLException;
import java.util.List;

public final class PlayerSessionListener implements Listener {

    private final SopChatPlugin plugin;

    public PlayerSessionListener(SopChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        notifyUnreadChannels(player);
        notifyUnreadPrivateMessages(player);
    }

    private void notifyUnreadChannels(Player player) {
        if (!this.plugin.getConfig().getBoolean("channels.unread.notify-on-join", true)) {
            return;
        }
        try {
            List<PlayerChannel> channels = this.plugin.getChannelService().findAccessibleChannels(player);
            int unreadMessages = 0;
            int unreadChannels = 0;
            for (PlayerChannel channel : channels) {
                int unread = this.plugin.getChannelService().countUnread(channel.getName(), player);
                if (unread > 0) {
                    unreadChannels++;
                    unreadMessages += unread;
                }
            }
            if (unreadMessages <= 0) {
                return;
            }
            player.sendMessage(formatMessage(
                    "unread-summary",
                    "{prefix}&eYou have &6{messages}&e unread messages in &6{channels}&e channels.",
                    "{messages}", Integer.toString(unreadMessages),
                    "{channels}", Integer.toString(unreadChannels)
            ));
        } catch (SQLException exception) {
            this.plugin.getLogger().warning("Failed to load channel unread summary for " + player.getName() + ": " + exception.getMessage());
        }
    }

    private void notifyUnreadPrivateMessages(Player player) {
        if (!this.plugin.getConfig().getBoolean("private-messages.notify-unread-on-join", true)) {
            return;
        }
        try {
            List<DirectConversationSummary> conversations = this.plugin.getDirectMessageService().findConversationSummaries(player, Integer.MAX_VALUE);
            int unreadMessages = 0;
            int unreadConversations = 0;
            for (DirectConversationSummary summary : conversations) {
                if (summary.getUnreadCount() > 0) {
                    unreadConversations++;
                    unreadMessages += summary.getUnreadCount();
                }
            }
            if (unreadMessages <= 0) {
                return;
            }
            player.sendMessage(formatMessage(
                    "private-message-unread-summary",
                    "{prefix}&bYou have &f{messages}&b unread private messages in &f{conversations}&b conversations.",
                    "{messages}", Integer.toString(unreadMessages),
                    "{conversations}", Integer.toString(unreadConversations)
            ));
        } catch (SQLException exception) {
            this.plugin.getLogger().warning("Failed to load private message unread summary for " + player.getName() + ": " + exception.getMessage());
        }
    }

    private String formatMessage(String key, String fallback, String... replacements) {
        String text = this.plugin.getMessageConfig().get(key, fallback)
                .replace("{prefix}", this.plugin.getMessageConfig().get("prefix", "&6SopChat &8| "));
        for (int index = 0; index + 1 < replacements.length; index += 2) {
            text = text.replace(replacements[index], replacements[index + 1]);
        }
        return this.plugin.getChatFormattingService().formatSystemMessage(text);
    }
}
