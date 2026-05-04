package net.enelson.sopchat.channel;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InMemoryChannelRepository implements ChannelRepository {

    private final Map<Long, PlayerChannel> channels = new LinkedHashMap<Long, PlayerChannel>();
    private final Map<String, Long> channelIdsByName = new LinkedHashMap<String, Long>();
    private final Map<String, ChannelMemberRole> roles = new LinkedHashMap<String, ChannelMemberRole>();
    private long nextId = 1L;

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

    private void setRole(long channelId, String playerUuid, ChannelMemberRole role) {
        this.roles.put(memberKey(channelId, playerUuid), role);
    }

    private String memberKey(long channelId, String playerUuid) {
        return channelId + ":" + playerUuid.toLowerCase(Locale.ROOT);
    }

    private String normalizeName(String input) {
        return input == null ? "" : input.trim().toLowerCase(Locale.ROOT);
    }
}
