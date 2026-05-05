package net.enelson.sopchat.channel;

import net.enelson.sopchat.SopChatPlugin;
import net.enelson.sopchat.mention.MentionResult;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ChannelService {

    private final SopChatPlugin plugin;
    private final ChannelRepository repository;
    private final Map<java.util.UUID, String> activeChannels = new LinkedHashMap<java.util.UUID, String>();

    public ChannelService(SopChatPlugin plugin) {
        this(plugin, new InMemoryChannelRepository());
    }

    public ChannelService(SopChatPlugin plugin, ChannelRepository repository) {
        this.plugin = plugin;
        this.repository = repository;
    }

    public PlayerChannel createChannel(Player owner, String name) throws SQLException {
        ensurePlayerCanOwnMore(owner);
        return this.repository.createChannel(name, owner.getUniqueId().toString());
    }

    public PlayerChannel transferOwnership(Player actor, String channelName, Player newOwner) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        if (!channel.getOwnerUuid().equalsIgnoreCase(actor.getUniqueId().toString())) {
            throw new SQLException("You are not the channel owner");
        }
        ensurePlayerCanOwnMore(newOwner);
        this.repository.ensureMembership(channel.getId(), newOwner.getUniqueId().toString(), ChannelMemberRole.MEMBER);
        return this.repository.updateOwner(channel.getId(), newOwner.getUniqueId().toString());
    }

    public boolean isOwner(Player player, String channelName) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        return channel.getOwnerUuid().equalsIgnoreCase(player.getUniqueId().toString());
    }

    public ChannelInvite invite(Player actor, String channelName, Player invited) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        if (!channel.getOwnerUuid().equalsIgnoreCase(actor.getUniqueId().toString())
                && this.repository.findRole(channel.getId(), actor.getUniqueId().toString()) != ChannelMemberRole.MODERATOR) {
            throw new SQLException("You cannot invite players to this channel");
        }
        long now = System.currentTimeMillis() / 1000L;
        long expiresAt = now + Math.max(1L, this.plugin.getConfig().getLong("channels.invites.expire-seconds", 300L));
        return this.repository.createInvite(channel.getId(), actor.getUniqueId().toString(), invited.getUniqueId().toString(), now, expiresAt);
    }

    public List<ChannelInvite> findInvites(Player player) throws SQLException {
        return this.repository.findInvites(player.getUniqueId().toString(), System.currentTimeMillis() / 1000L);
    }

    public PlayerChannel acceptInvite(Player player, long channelId) throws SQLException {
        PlayerChannel channel = this.repository.findChannelById(channelId);
        if (channel == null) {
            throw new SQLException("Channel not found");
        }
        this.repository.ensureMembership(channelId, player.getUniqueId().toString(), ChannelMemberRole.MEMBER);
        this.repository.deleteInvite(channelId, player.getUniqueId().toString());
        return channel;
    }

    public void denyInvite(Player player, long channelId) throws SQLException {
        this.repository.deleteInvite(channelId, player.getUniqueId().toString());
    }

    public void leaveChannel(Player player, String channelName) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        ChannelMemberRole role = this.repository.findRole(channel.getId(), player.getUniqueId().toString());
        if (role == null) {
            throw new SQLException("You are not a member of this channel");
        }
        if (role == ChannelMemberRole.OWNER) {
            throw new SQLException("Transfer ownership or delete the channel before leaving");
        }
        this.repository.removeMembership(channel.getId(), player.getUniqueId().toString());
        clearActiveChannelIfMatches(player, channel.getName());
    }

    public void deleteChannel(Player player, String channelName) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        if (!channel.getOwnerUuid().equalsIgnoreCase(player.getUniqueId().toString())) {
            throw new SQLException("You are not the channel owner");
        }
        this.repository.deleteChannel(channel.getId());
        clearActiveChannelIfMatches(player, channel.getName());
    }

    public void kickMember(Player actor, String channelName, OfflinePlayer target) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        ChannelMemberRole actorRole = this.repository.findRole(channel.getId(), actor.getUniqueId().toString());
        if (actorRole != ChannelMemberRole.OWNER && actorRole != ChannelMemberRole.MODERATOR) {
            throw new SQLException("You cannot kick players from this channel");
        }
        if (target == null) {
            throw new SQLException("Target not found");
        }
        ChannelMemberRole targetRole = this.repository.findRole(channel.getId(), target.getUniqueId().toString());
        if (targetRole == null) {
            throw new SQLException("That player is not in the channel");
        }
        if (targetRole == ChannelMemberRole.OWNER) {
            throw new SQLException("You cannot kick the channel owner");
        }
        if (actorRole == ChannelMemberRole.MODERATOR && targetRole == ChannelMemberRole.MODERATOR) {
            throw new SQLException("Moderators cannot kick other moderators");
        }
        this.repository.removeMembership(channel.getId(), target.getUniqueId().toString());
        clearActiveChannelIfMatches(target, channel.getName());
    }

    public ChannelMessage saveChannelMessage(PlayerChannel channel, Player sender, String message) throws SQLException {
        return this.repository.saveMessage(channel.getId(), sender.getUniqueId().toString(), sender.getName(), message, System.currentTimeMillis() / 1000L);
    }

    public List<ChannelMessage> findRecentMessages(String channelName, int limit) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        return this.repository.findRecentMessages(channel.getId(), limit);
    }

    public void markRead(String channelName, Player player, long lastMessageId) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        this.repository.markRead(channel.getId(), player.getUniqueId().toString(), lastMessageId, System.currentTimeMillis() / 1000L);
    }

    public int countUnread(String channelName, Player player) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        return this.repository.countUnread(channel.getId(), player.getUniqueId().toString());
    }

    public PlayerChannel findChannel(String channelName) throws SQLException {
        return this.repository.findChannelByName(channelName);
    }

    public PlayerChannel findChannel(long channelId) throws SQLException {
        return this.repository.findChannelById(channelId);
    }

    public void sendChannelMessage(Player sender, String channelName, String message) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        ChannelMemberRole role = this.repository.findRole(channel.getId(), sender.getUniqueId().toString());
        if (role == null) {
            throw new SQLException("You are not a member of this channel");
        }
        String sanitizedMessage = this.plugin.getChatFormattingService().sanitizePlayerMessage(sender, message);
        String format = this.plugin.getConfig().getString("channels.format", "&d[{channel}] &f{player}: {message}");
        String formatPrefix = format.replace("{channel}", channel.getName())
                .replace("{player}", sender.getName())
                .replace("{message}", "");
        String baseMessageColors = org.bukkit.ChatColor.getLastColors(
                this.plugin.getChatFormattingService().formatSystemMessage(formatPrefix)
        );
        MentionResult mentionResult = this.plugin.getMentionService().processMentions(sender, sanitizedMessage, baseMessageColors);
        String formattedPlayerMessage = this.plugin.getChatFormattingService().formatPlayerMessage(sender, mentionResult.getMessage());
        ChannelMessage stored = saveChannelMessage(channel, sender, formattedPlayerMessage);
        String renderedMessage = this.plugin.getChatFormattingService().formatSystemMessage(
                format.replace("{channel}", channel.getName())
                        .replace("{player}", sender.getName())
                        .replace("{message}", formattedPlayerMessage)
        );

        Set<java.util.UUID> onlineRecipients = new HashSet<java.util.UUID>();
        for (String memberUuidText : this.repository.findMemberUuids(channel.getId())) {
            try {
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(java.util.UUID.fromString(memberUuidText));
                if (offlinePlayer.isOnline() && offlinePlayer.getPlayer() != null) {
                    Player receiver = offlinePlayer.getPlayer();
                    receiver.sendMessage(renderedMessage);
                    onlineRecipients.add(receiver.getUniqueId());
                }
            } catch (Exception ignored) {
            }
        }
        for (java.util.UUID uniqueId : mentionResult.getMentionedPlayers()) {
            if (onlineRecipients.contains(uniqueId)) {
                Player receiver = Bukkit.getPlayer(uniqueId);
                if (receiver != null) {
                    this.plugin.getMentionService().playMentionSound(receiver);
                }
            }
        }
        Bukkit.getConsoleSender().sendMessage(renderedMessage);
        markRead(channel.getName(), sender, stored.getId());
    }

    public void setActiveChannel(Player player, String channelName) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        ChannelMemberRole role = this.repository.findRole(channel.getId(), player.getUniqueId().toString());
        if (role == null) {
            throw new SQLException("You are not a member of this channel");
        }
        this.activeChannels.put(player.getUniqueId(), channel.getName());
        this.plugin.getPlayerPreferenceService().setActiveChannel(player, channel.getName());
    }

    public void clearActiveChannel(Player player) {
        if (player != null) {
            this.activeChannels.remove(player.getUniqueId());
            try {
                this.plugin.getPlayerPreferenceService().setActiveChannel(player, null);
            } catch (SQLException ignored) {
            }
        }
    }

    public PlayerChannel getActiveChannel(Player player) throws SQLException {
        if (player == null) {
            return null;
        }
        String channelName = this.activeChannels.get(player.getUniqueId());
        if ((channelName == null || channelName.isEmpty()) && this.plugin.getPlayerPreferenceService() != null) {
            channelName = this.plugin.getPlayerPreferenceService().findActiveChannel(player);
            if (channelName != null && !channelName.isEmpty()) {
                this.activeChannels.put(player.getUniqueId(), channelName);
            }
        }
        if (channelName == null || channelName.isEmpty()) {
            return null;
        }
        PlayerChannel channel = this.repository.findChannelByName(channelName);
        if (channel == null) {
            this.activeChannels.remove(player.getUniqueId());
            this.plugin.getPlayerPreferenceService().setActiveChannel(player, null);
            return null;
        }
        ChannelMemberRole role = this.repository.findRole(channel.getId(), player.getUniqueId().toString());
        if (role == null) {
            this.activeChannels.remove(player.getUniqueId());
            this.plugin.getPlayerPreferenceService().setActiveChannel(player, null);
            return null;
        }
        return channel;
    }

    private PlayerChannel requireChannel(String name) throws SQLException {
        PlayerChannel channel = this.repository.findChannelByName(name);
        if (channel == null) {
            throw new SQLException("Channel not found");
        }
        return channel;
    }

    private void ensurePlayerCanOwnMore(Player player) throws SQLException {
        int limit = resolveOwnedChannelLimit(player);
        if (limit <= 0) {
            return;
        }
        List<PlayerChannel> owned = this.repository.findOwnedChannels(player.getUniqueId().toString());
        if (owned.size() >= limit) {
            throw new SQLException("Owned channel limit reached");
        }
    }

    private int resolveOwnedChannelLimit(Player player) {
        int limit = this.plugin.getConfig().getInt("channels.limits.owned-per-player", 1);
        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String permission = info.getPermission().toLowerCase(Locale.ROOT);
            if (!permission.startsWith("sopchat.limit.owned_channels.") || !info.getValue()) {
                continue;
            }
            String suffix = permission.substring("sopchat.limit.owned_channels.".length());
            try {
                limit = Math.max(limit, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
            }
        }
        return limit;
    }

    public List<PlayerChannel> findOwnedChannels(Player player) throws SQLException {
        if (player == null) {
            return Collections.emptyList();
        }
        return this.repository.findOwnedChannels(player.getUniqueId().toString());
    }

    public List<PlayerChannel> findMemberChannels(Player player) throws SQLException {
        if (player == null) {
            return Collections.emptyList();
        }
        return this.repository.findMemberChannels(player.getUniqueId().toString());
    }

    public List<PlayerChannel> findAccessibleChannels(Player player) throws SQLException {
        if (player == null) {
            return Collections.emptyList();
        }
        Map<Long, PlayerChannel> channels = new LinkedHashMap<Long, PlayerChannel>();
        for (PlayerChannel channel : this.repository.findMemberChannels(player.getUniqueId().toString())) {
            channels.put(Long.valueOf(channel.getId()), channel);
        }
        return new ArrayList<PlayerChannel>(channels.values());
    }

    private void clearActiveChannelIfMatches(OfflinePlayer player, String channelName) {
        if (player == null || channelName == null) {
            return;
        }
        String active = this.activeChannels.get(player.getUniqueId());
        if (active != null && active.equalsIgnoreCase(channelName)) {
            this.activeChannels.remove(player.getUniqueId());
            if (player.isOnline() && player.getPlayer() != null) {
                try {
                    this.plugin.getPlayerPreferenceService().setActiveChannel(player.getPlayer(), null);
                } catch (SQLException ignored) {
                }
            }
        }
    }
}
