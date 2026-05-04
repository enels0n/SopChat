package net.enelson.sopchat.channel;

import java.sql.SQLException;
import java.util.List;

public interface ChannelRepository {

    PlayerChannel createChannel(String name, String ownerUuid) throws SQLException;

    PlayerChannel findChannelByName(String name) throws SQLException;

    PlayerChannel updateOwner(long channelId, String newOwnerUuid) throws SQLException;

    void ensureMembership(long channelId, String playerUuid, ChannelMemberRole role) throws SQLException;

    ChannelMemberRole findRole(long channelId, String playerUuid) throws SQLException;

    List<PlayerChannel> findOwnedChannels(String ownerUuid) throws SQLException;
}
