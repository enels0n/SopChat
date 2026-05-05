package net.enelson.sopchat.channel;

import java.sql.SQLException;
import java.util.List;

public interface ChannelRepository {

    void initialize() throws SQLException;

    PlayerChannel createChannel(String name, String ownerUuid) throws SQLException;

    PlayerChannel findChannelById(long channelId) throws SQLException;

    PlayerChannel findChannelByName(String name) throws SQLException;

    PlayerChannel updateOwner(long channelId, String newOwnerUuid) throws SQLException;

    void ensureMembership(long channelId, String playerUuid, ChannelMemberRole role) throws SQLException;

    ChannelMemberRole findRole(long channelId, String playerUuid) throws SQLException;

    List<PlayerChannel> findOwnedChannels(String ownerUuid) throws SQLException;

    List<PlayerChannel> findMemberChannels(String playerUuid) throws SQLException;

    List<String> findMemberUuids(long channelId) throws SQLException;

    void removeMembership(long channelId, String playerUuid) throws SQLException;

    void deleteChannel(long channelId) throws SQLException;

    ChannelInvite createInvite(long channelId, String inviterUuid, String invitedUuid, long createdAt, long expiresAt) throws SQLException;

    List<ChannelInvite> findInvites(String invitedUuid, long nowEpochSeconds) throws SQLException;

    void deleteInvite(long channelId, String invitedUuid) throws SQLException;

    ChannelMessage saveMessage(long channelId, String senderUuid, String senderName, String message, long createdAt) throws SQLException;

    List<ChannelMessage> findRecentMessages(long channelId, int limit) throws SQLException;

    void markRead(long channelId, String playerUuid, long lastMessageId, long readAt) throws SQLException;

    int countUnread(long channelId, String playerUuid) throws SQLException;
}
