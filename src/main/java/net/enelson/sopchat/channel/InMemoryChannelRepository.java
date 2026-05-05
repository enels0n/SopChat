package net.enelson.sopchat.channel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InMemoryChannelRepository implements ChannelRepository {

    private final Map<Long, PlayerChannel> channels = new LinkedHashMap<Long, PlayerChannel>();
    private final Map<String, Long> channelIdsByName = new LinkedHashMap<String, Long>();
    private final Map<String, ChannelMemberRole> roles = new LinkedHashMap<String, ChannelMemberRole>();
    private final Map<String, ChannelInvite> invites = new LinkedHashMap<String, ChannelInvite>();
    private final Map<Long, List<ChannelMessage>> messages = new LinkedHashMap<Long, List<ChannelMessage>>();
    private final Map<String, Long> reads = new LinkedHashMap<String, Long>();
    private long nextId = 1L;
    private long nextMessageId = 1L;

    @Override
    public void initialize() {
    }

    @Override
    public PlayerChannel createChannel(String name, String ownerUuid) throws SQLException {
        PlayerChannel existing = findChannelByName(name);
        if (existing != null) {
            throw new SQLException("Channel already exists");
        }
        PlayerChannel channel = new PlayerChannel(this.nextId++, name, ownerUuid, System.currentTimeMillis() / 1000L);
        this.channels.put(Long.valueOf(channel.getId()), channel);
        this.channelIdsByName.put(normalizeName(name), Long.valueOf(channel.getId()));
        ensureMembership(channel.getId(), ownerUuid, ChannelMemberRole.OWNER);
        return channel;
    }

    @Override
    public PlayerChannel findChannelById(long channelId) {
        return this.channels.get(Long.valueOf(channelId));
    }

    @Override
    public PlayerChannel findChannelByName(String name) {
        Long id = this.channelIdsByName.get(normalizeName(name));
        return id == null ? null : this.channels.get(id);
    }

    @Override
    public PlayerChannel updateOwner(long channelId, String newOwnerUuid) throws SQLException {
        PlayerChannel channel = this.channels.get(Long.valueOf(channelId));
        if (channel == null) {
            throw new SQLException("Channel not found");
        }
        PlayerChannel updated = channel.withOwner(newOwnerUuid);
        this.channels.put(Long.valueOf(channelId), updated);
        setRole(channelId, channel.getOwnerUuid(), ChannelMemberRole.MODERATOR);
        setRole(channelId, newOwnerUuid, ChannelMemberRole.OWNER);
        return updated;
    }

    @Override
    public void ensureMembership(long channelId, String playerUuid, ChannelMemberRole role) {
        setRole(channelId, playerUuid, role);
    }

    @Override
    public ChannelMemberRole findRole(long channelId, String playerUuid) {
        return this.roles.get(memberKey(channelId, playerUuid));
    }

    @Override
    public List<PlayerChannel> findOwnedChannels(String ownerUuid) {
        List<PlayerChannel> result = new ArrayList<PlayerChannel>();
        for (PlayerChannel channel : this.channels.values()) {
            if (channel.getOwnerUuid().equalsIgnoreCase(ownerUuid)) {
                result.add(channel);
            }
        }
        return result;
    }

    @Override
    public List<PlayerChannel> findMemberChannels(String playerUuid) {
        List<PlayerChannel> result = new ArrayList<PlayerChannel>();
        for (Map.Entry<Long, PlayerChannel> entry : this.channels.entrySet()) {
            if (this.roles.containsKey(memberKey(entry.getKey().longValue(), playerUuid))) {
                result.add(entry.getValue());
            }
        }
        return result;
    }

    @Override
    public List<String> findMemberUuids(long channelId) {
        List<String> result = new ArrayList<String>();
        String prefix = channelId + ":";
        for (String key : this.roles.keySet()) {
            if (key.startsWith(prefix)) {
                result.add(key.substring(prefix.length()));
            }
        }
        return result;
    }

    @Override
    public void removeMembership(long channelId, String playerUuid) {
        this.roles.remove(memberKey(channelId, playerUuid));
    }

    @Override
    public void deleteChannel(long channelId) {
        PlayerChannel removed = this.channels.remove(Long.valueOf(channelId));
        if (removed != null) {
            this.channelIdsByName.remove(normalizeName(removed.getName()));
        }
        this.messages.remove(Long.valueOf(channelId));
        String prefix = channelId + ":";
        this.roles.keySet().removeIf(key -> key.startsWith(prefix));
        this.invites.keySet().removeIf(key -> key.startsWith(prefix));
        this.reads.keySet().removeIf(key -> key.startsWith(prefix));
    }

    @Override
    public ChannelInvite createInvite(long channelId, String inviterUuid, String invitedUuid, long createdAt, long expiresAt) {
        ChannelInvite invite = new ChannelInvite(channelId, inviterUuid, invitedUuid, createdAt, expiresAt);
        this.invites.put(inviteKey(channelId, invitedUuid), invite);
        return invite;
    }

    @Override
    public List<ChannelInvite> findInvites(String invitedUuid, long nowEpochSeconds) {
        List<ChannelInvite> result = new ArrayList<ChannelInvite>();
        for (ChannelInvite invite : this.invites.values()) {
            if (invite.getInvitedUuid().equalsIgnoreCase(invitedUuid) && invite.getExpiresAt() >= nowEpochSeconds) {
                result.add(invite);
            }
        }
        return result;
    }

    @Override
    public void deleteInvite(long channelId, String invitedUuid) {
        this.invites.remove(inviteKey(channelId, invitedUuid));
    }

    @Override
    public ChannelMessage saveMessage(long channelId, String senderUuid, String senderName, String message, long createdAt) {
        ChannelMessage saved = new ChannelMessage(this.nextMessageId++, channelId, senderUuid, senderName, message, createdAt);
        List<ChannelMessage> list = this.messages.get(Long.valueOf(channelId));
        if (list == null) {
            list = new ArrayList<ChannelMessage>();
            this.messages.put(Long.valueOf(channelId), list);
        }
        list.add(saved);
        return saved;
    }

    @Override
    public List<ChannelMessage> findRecentMessages(long channelId, int limit) {
        List<ChannelMessage> list = this.messages.get(Long.valueOf(channelId));
        if (list == null || list.isEmpty()) {
            return Collections.emptyList();
        }
        int fromIndex = Math.max(0, list.size() - Math.max(1, limit));
        return new ArrayList<ChannelMessage>(list.subList(fromIndex, list.size()));
    }

    @Override
    public void markRead(long channelId, String playerUuid, long lastMessageId, long readAt) {
        this.reads.put(readKey(channelId, playerUuid), Long.valueOf(lastMessageId));
    }

    @Override
    public int countUnread(long channelId, String playerUuid) {
        List<ChannelMessage> list = this.messages.get(Long.valueOf(channelId));
        if (list == null || list.isEmpty()) {
            return 0;
        }
        Long lastReadId = this.reads.get(readKey(channelId, playerUuid));
        long threshold = lastReadId == null ? 0L : lastReadId.longValue();
        int unread = 0;
        for (ChannelMessage message : list) {
            if (message.getId() > threshold) {
                unread++;
            }
        }
        return unread;
    }

    private void setRole(long channelId, String playerUuid, ChannelMemberRole role) {
        this.roles.put(memberKey(channelId, playerUuid), role);
    }

    private String memberKey(long channelId, String playerUuid) {
        return channelId + ":" + playerUuid.toLowerCase(Locale.ROOT);
    }

    private String inviteKey(long channelId, String invitedUuid) {
        return channelId + ":" + invitedUuid.toLowerCase(Locale.ROOT);
    }

    private String readKey(long channelId, String playerUuid) {
        return channelId + ":" + playerUuid.toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }
}
