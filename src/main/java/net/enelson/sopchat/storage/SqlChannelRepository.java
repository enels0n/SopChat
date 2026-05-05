package net.enelson.sopchat.storage;

import net.enelson.sopchat.channel.ChannelInvite;
import net.enelson.sopchat.channel.ChannelMemberRole;
import net.enelson.sopchat.channel.ChannelMessage;
import net.enelson.sopchat.channel.ChannelRepository;
import net.enelson.sopchat.channel.PlayerChannel;
import net.enelson.sopli.lib.database.SopDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SqlChannelRepository implements ChannelRepository {

    private final SopDatabase database;
    private final String tablePrefix;

    public SqlChannelRepository(SopDatabase database, String tablePrefix) {
        this.database = database;
        this.tablePrefix = tablePrefix;
    }

    @Override
    public void initialize() throws SQLException {
        this.database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "channels ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "name VARCHAR(64) NOT NULL UNIQUE,"
                + "owner_uuid VARCHAR(36) NOT NULL,"
                + "created_at BIGINT NOT NULL"
                + ")");
        this.database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "channel_members ("
                + "channel_id BIGINT NOT NULL,"
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "role VARCHAR(16) NOT NULL,"
                + "joined_at BIGINT NOT NULL,"
                + "PRIMARY KEY (channel_id, player_uuid)"
                + ")");
        this.database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "channel_invites ("
                + "channel_id BIGINT NOT NULL,"
                + "inviter_uuid VARCHAR(36) NOT NULL,"
                + "invited_uuid VARCHAR(36) NOT NULL,"
                + "created_at BIGINT NOT NULL,"
                + "expires_at BIGINT NOT NULL,"
                + "PRIMARY KEY (channel_id, invited_uuid)"
                + ")");
        this.database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "channel_messages ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "channel_id BIGINT NOT NULL,"
                + "sender_uuid VARCHAR(36) NOT NULL,"
                + "sender_name VARCHAR(16) NOT NULL,"
                + "message TEXT NOT NULL,"
                + "created_at BIGINT NOT NULL"
                + ")");
        this.database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "channel_reads ("
                + "channel_id BIGINT NOT NULL,"
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "last_message_id BIGINT NOT NULL,"
                + "read_at BIGINT NOT NULL,"
                + "PRIMARY KEY (channel_id, player_uuid)"
                + ")");
    }

    @Override
    public PlayerChannel createChannel(final String name, final String ownerUuid) throws SQLException {
        return this.database.transaction(connection -> {
            PreparedStatement insert = null;
            try {
                insert = connection.prepareStatement(
                        "INSERT INTO " + tablePrefix + "channels (name, owner_uuid, created_at) VALUES (?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS
                );
                long now = System.currentTimeMillis() / 1000L;
                insert.setString(1, name);
                insert.setString(2, ownerUuid);
                insert.setLong(3, now);
                insert.executeUpdate();

                ResultSet generatedKeys = insert.getGeneratedKeys();
                long id = 0L;
                try {
                    if (generatedKeys.next()) {
                        id = generatedKeys.getLong(1);
                    }
                } finally {
                    closeQuietly(generatedKeys);
                }
                ensureMembershipInternal(connection, id, ownerUuid, ChannelMemberRole.OWNER, now);
                return new PlayerChannel(id, name, ownerUuid, now);
            } finally {
                closeQuietly(insert);
            }
        });
    }

    @Override
    public PlayerChannel findChannelById(final long channelId) throws SQLException {
        return this.database.withConnection(connection -> {
            return findChannelById(connection, channelId);
        });
    }

    @Override
    public PlayerChannel findChannelByName(final String name) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement("SELECT id, name, owner_uuid, created_at FROM " + tablePrefix + "channels WHERE LOWER(name) = LOWER(?)");
                statement.setString(1, name);
                resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    return null;
                }
                return mapChannel(resultSet);
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public PlayerChannel updateOwner(final long channelId, final String newOwnerUuid) throws SQLException {
        return this.database.transaction(connection -> {
            PlayerChannel current = findChannelById(connection, channelId);
            if (current == null) {
                throw new SQLException("Channel not found");
            }
            PreparedStatement update = null;
            try {
                update = connection.prepareStatement("UPDATE " + tablePrefix + "channels SET owner_uuid = ? WHERE id = ?");
                update.setString(1, newOwnerUuid);
                update.setLong(2, channelId);
                update.executeUpdate();
            } finally {
                closeQuietly(update);
            }
            ensureMembershipInternal(connection, channelId, current.getOwnerUuid(), ChannelMemberRole.MODERATOR, System.currentTimeMillis() / 1000L);
            ensureMembershipInternal(connection, channelId, newOwnerUuid, ChannelMemberRole.OWNER, System.currentTimeMillis() / 1000L);
            return new PlayerChannel(channelId, current.getName(), newOwnerUuid, current.getCreatedAt());
        });
    }

    @Override
    public void ensureMembership(final long channelId, final String playerUuid, final ChannelMemberRole role) throws SQLException {
        this.database.transaction(connection -> {
            ensureMembershipInternal(connection, channelId, playerUuid, role, System.currentTimeMillis() / 1000L);
            return null;
        });
    }

    @Override
    public ChannelMemberRole findRole(final long channelId, final String playerUuid) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement("SELECT role FROM " + tablePrefix + "channel_members WHERE channel_id = ? AND player_uuid = ?");
                statement.setLong(1, channelId);
                statement.setString(2, playerUuid);
                resultSet = statement.executeQuery();
                if (!resultSet.next()) {
                    return null;
                }
                return ChannelMemberRole.valueOf(resultSet.getString("role"));
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public List<PlayerChannel> findOwnedChannels(final String ownerUuid) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            List<PlayerChannel> result = new ArrayList<PlayerChannel>();
            try {
                statement = connection.prepareStatement("SELECT id, name, owner_uuid, created_at FROM " + tablePrefix + "channels WHERE owner_uuid = ?");
                statement.setString(1, ownerUuid);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    result.add(mapChannel(resultSet));
                }
                return result;
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public List<PlayerChannel> findMemberChannels(final String playerUuid) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            List<PlayerChannel> result = new ArrayList<PlayerChannel>();
            try {
                statement = connection.prepareStatement(
                        "SELECT c.id, c.name, c.owner_uuid, c.created_at "
                                + "FROM " + tablePrefix + "channels c "
                                + "INNER JOIN " + tablePrefix + "channel_members m ON m.channel_id = c.id "
                                + "WHERE m.player_uuid = ? "
                                + "ORDER BY c.name ASC"
                );
                statement.setString(1, playerUuid);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    result.add(mapChannel(resultSet));
                }
                return result;
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public List<String> findMemberUuids(final long channelId) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            List<String> result = new ArrayList<String>();
            try {
                statement = connection.prepareStatement("SELECT player_uuid FROM " + tablePrefix + "channel_members WHERE channel_id = ?");
                statement.setLong(1, channelId);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    result.add(resultSet.getString("player_uuid"));
                }
                return result;
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public void removeMembership(final long channelId, final String playerUuid) throws SQLException {
        this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement("DELETE FROM " + tablePrefix + "channel_members WHERE channel_id = ? AND player_uuid = ?");
                statement.setLong(1, channelId);
                statement.setString(2, playerUuid);
                statement.executeUpdate();
            } finally {
                closeQuietly(statement);
            }
        });
    }

    @Override
    public void deleteChannel(final long channelId) throws SQLException {
        this.database.transaction(connection -> {
            executeDelete(connection, "DELETE FROM " + tablePrefix + "channel_reads WHERE channel_id = ?", channelId);
            executeDelete(connection, "DELETE FROM " + tablePrefix + "channel_messages WHERE channel_id = ?", channelId);
            executeDelete(connection, "DELETE FROM " + tablePrefix + "channel_invites WHERE channel_id = ?", channelId);
            executeDelete(connection, "DELETE FROM " + tablePrefix + "channel_members WHERE channel_id = ?", channelId);
            executeDelete(connection, "DELETE FROM " + tablePrefix + "channels WHERE id = ?", channelId);
            return null;
        });
    }

    @Override
    public ChannelInvite createInvite(final long channelId, final String inviterUuid, final String invitedUuid, final long createdAt, final long expiresAt) throws SQLException {
        this.database.transaction(connection -> {
            PreparedStatement delete = null;
            PreparedStatement insert = null;
            try {
                delete = connection.prepareStatement("DELETE FROM " + tablePrefix + "channel_invites WHERE channel_id = ? AND invited_uuid = ?");
                delete.setLong(1, channelId);
                delete.setString(2, invitedUuid);
                delete.executeUpdate();

                insert = connection.prepareStatement("INSERT INTO " + tablePrefix + "channel_invites (channel_id, inviter_uuid, invited_uuid, created_at, expires_at) VALUES (?, ?, ?, ?, ?)");
                insert.setLong(1, channelId);
                insert.setString(2, inviterUuid);
                insert.setString(3, invitedUuid);
                insert.setLong(4, createdAt);
                insert.setLong(5, expiresAt);
                insert.executeUpdate();
                return null;
            } finally {
                closeQuietly(delete);
                closeQuietly(insert);
            }
        });
        return new ChannelInvite(channelId, inviterUuid, invitedUuid, createdAt, expiresAt);
    }

    @Override
    public List<ChannelInvite> findInvites(final String invitedUuid, final long nowEpochSeconds) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            List<ChannelInvite> result = new ArrayList<ChannelInvite>();
            try {
                statement = connection.prepareStatement("SELECT channel_id, inviter_uuid, invited_uuid, created_at, expires_at FROM " + tablePrefix + "channel_invites WHERE invited_uuid = ? AND expires_at >= ?");
                statement.setString(1, invitedUuid);
                statement.setLong(2, nowEpochSeconds);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    result.add(new ChannelInvite(
                            resultSet.getLong("channel_id"),
                            resultSet.getString("inviter_uuid"),
                            resultSet.getString("invited_uuid"),
                            resultSet.getLong("created_at"),
                            resultSet.getLong("expires_at")
                    ));
                }
                return result;
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public void deleteInvite(final long channelId, final String invitedUuid) throws SQLException {
        this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement("DELETE FROM " + tablePrefix + "channel_invites WHERE channel_id = ? AND invited_uuid = ?");
                statement.setLong(1, channelId);
                statement.setString(2, invitedUuid);
                statement.executeUpdate();
            } finally {
                closeQuietly(statement);
            }
        });
    }

    @Override
    public ChannelMessage saveMessage(final long channelId, final String senderUuid, final String senderName, final String message, final long createdAt) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement insert = null;
            ResultSet keys = null;
            try {
                insert = connection.prepareStatement(
                        "INSERT INTO " + tablePrefix + "channel_messages (channel_id, sender_uuid, sender_name, message, created_at) VALUES (?, ?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS
                );
                insert.setLong(1, channelId);
                insert.setString(2, senderUuid);
                insert.setString(3, senderName);
                insert.setString(4, message);
                insert.setLong(5, createdAt);
                insert.executeUpdate();
                keys = insert.getGeneratedKeys();
                long id = 0L;
                if (keys.next()) {
                    id = keys.getLong(1);
                }
                return new ChannelMessage(id, channelId, senderUuid, senderName, message, createdAt);
            } finally {
                closeQuietly(keys);
                closeQuietly(insert);
            }
        });
    }

    @Override
    public List<ChannelMessage> findRecentMessages(final long channelId, final int limit) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            List<ChannelMessage> reversed = new ArrayList<ChannelMessage>();
            try {
                statement = connection.prepareStatement("SELECT id, channel_id, sender_uuid, sender_name, message, created_at FROM " + tablePrefix + "channel_messages WHERE channel_id = ? ORDER BY id DESC LIMIT ?");
                statement.setLong(1, channelId);
                statement.setInt(2, Math.max(1, limit));
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    reversed.add(mapMessage(resultSet));
                }
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
            Collections.reverse(reversed);
            return reversed;
        });
    }

    @Override
    public void markRead(final long channelId, final String playerUuid, final long lastMessageId, final long readAt) throws SQLException {
        this.database.transaction(connection -> {
            PreparedStatement delete = null;
            PreparedStatement insert = null;
            try {
                delete = connection.prepareStatement("DELETE FROM " + tablePrefix + "channel_reads WHERE channel_id = ? AND player_uuid = ?");
                delete.setLong(1, channelId);
                delete.setString(2, playerUuid);
                delete.executeUpdate();

                insert = connection.prepareStatement("INSERT INTO " + tablePrefix + "channel_reads (channel_id, player_uuid, last_message_id, read_at) VALUES (?, ?, ?, ?)");
                insert.setLong(1, channelId);
                insert.setString(2, playerUuid);
                insert.setLong(3, lastMessageId);
                insert.setLong(4, readAt);
                insert.executeUpdate();
                return null;
            } finally {
                closeQuietly(delete);
                closeQuietly(insert);
            }
        });
    }

    @Override
    public int countUnread(final long channelId, final String playerUuid) throws SQLException {
        return this.database.withConnection(connection -> {
            long lastRead = findLastReadMessageId(connection, channelId, playerUuid);
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tablePrefix + "channel_messages WHERE channel_id = ? AND id > ?");
                statement.setLong(1, channelId);
                statement.setLong(2, lastRead);
                resultSet = statement.executeQuery();
                return resultSet.next() ? resultSet.getInt(1) : 0;
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    private void ensureMembershipInternal(Connection connection, long channelId, String playerUuid, ChannelMemberRole role, long joinedAt) throws SQLException {
        PreparedStatement delete = null;
        PreparedStatement insert = null;
        try {
            delete = connection.prepareStatement("DELETE FROM " + tablePrefix + "channel_members WHERE channel_id = ? AND player_uuid = ?");
            delete.setLong(1, channelId);
            delete.setString(2, playerUuid);
            delete.executeUpdate();

            insert = connection.prepareStatement("INSERT INTO " + tablePrefix + "channel_members (channel_id, player_uuid, role, joined_at) VALUES (?, ?, ?, ?)");
            insert.setLong(1, channelId);
            insert.setString(2, playerUuid);
            insert.setString(3, role.name());
            insert.setLong(4, joinedAt);
            insert.executeUpdate();
        } finally {
            closeQuietly(delete);
            closeQuietly(insert);
        }
    }

    private PlayerChannel findChannelById(Connection connection, long channelId) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("SELECT id, name, owner_uuid, created_at FROM " + tablePrefix + "channels WHERE id = ?");
            statement.setLong(1, channelId);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return null;
            }
            return mapChannel(resultSet);
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
        }
    }

    private long findLastReadMessageId(Connection connection, long channelId, String playerUuid) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        try {
            statement = connection.prepareStatement("SELECT last_message_id FROM " + tablePrefix + "channel_reads WHERE channel_id = ? AND player_uuid = ?");
            statement.setLong(1, channelId);
            statement.setString(2, playerUuid);
            resultSet = statement.executeQuery();
            if (!resultSet.next()) {
                return 0L;
            }
            return resultSet.getLong("last_message_id");
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
        }
    }

    private PlayerChannel mapChannel(ResultSet resultSet) throws SQLException {
        return new PlayerChannel(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("owner_uuid"),
                resultSet.getLong("created_at")
        );
    }

    private ChannelMessage mapMessage(ResultSet resultSet) throws SQLException {
        return new ChannelMessage(
                resultSet.getLong("id"),
                resultSet.getLong("channel_id"),
                resultSet.getString("sender_uuid"),
                resultSet.getString("sender_name"),
                resultSet.getString("message"),
                resultSet.getLong("created_at")
        );
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    private void executeDelete(Connection connection, String sql, long channelId) throws SQLException {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(sql);
            statement.setLong(1, channelId);
            statement.executeUpdate();
        } finally {
            closeQuietly(statement);
        }
    }
}
