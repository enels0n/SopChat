package net.enelson.sopchat.storage;

import net.enelson.sopchat.privatechat.DirectConversationSummary;
import net.enelson.sopchat.privatechat.DirectMessage;
import net.enelson.sopchat.privatechat.DirectMessageRepository;
import net.enelson.sopli.lib.database.SopDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SqlDirectMessageRepository implements DirectMessageRepository {

    private final SopDatabase database;
    private final String tablePrefix;

    public SqlDirectMessageRepository(SopDatabase database, String tablePrefix) {
        this.database = database;
        this.tablePrefix = tablePrefix;
    }

    @Override
    public void initialize() throws SQLException {
        this.database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "private_messages ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "sender_uuid VARCHAR(36) NOT NULL,"
                + "sender_name VARCHAR(16) NOT NULL,"
                + "receiver_uuid VARCHAR(36) NOT NULL,"
                + "receiver_name VARCHAR(16) NOT NULL,"
                + "message TEXT NOT NULL,"
                + "created_at BIGINT NOT NULL"
                + ")");
        this.database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "private_reads ("
                + "player_uuid VARCHAR(36) NOT NULL,"
                + "partner_uuid VARCHAR(36) NOT NULL,"
                + "last_message_id BIGINT NOT NULL,"
                + "read_at BIGINT NOT NULL,"
                + "PRIMARY KEY (player_uuid, partner_uuid)"
                + ")");
    }

    @Override
    public DirectMessage saveMessage(String senderUuid, String senderName, String receiverUuid, String receiverName, String message, long createdAt) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement insert = null;
            ResultSet keys = null;
            try {
                insert = connection.prepareStatement(
                        "INSERT INTO " + tablePrefix + "private_messages (sender_uuid, sender_name, receiver_uuid, receiver_name, message, created_at) VALUES (?, ?, ?, ?, ?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS
                );
                insert.setString(1, senderUuid);
                insert.setString(2, senderName);
                insert.setString(3, receiverUuid);
                insert.setString(4, receiverName);
                insert.setString(5, message);
                insert.setLong(6, createdAt);
                insert.executeUpdate();
                keys = insert.getGeneratedKeys();
                long id = 0L;
                if (keys.next()) {
                    id = keys.getLong(1);
                }
                return new DirectMessage(id, senderUuid, senderName, receiverUuid, receiverName, message, createdAt);
            } finally {
                closeQuietly(keys);
                closeQuietly(insert);
            }
        });
    }

    @Override
    public List<DirectConversationSummary> findConversationSummaries(String playerUuid, int limit) throws SQLException {
        return this.database.withConnection(connection -> {
            List<DirectMessage> messages = findMessagesForPlayer(connection, playerUuid);
            Map<String, Long> reads = findReadMap(connection, playerUuid);
            Map<String, DirectConversationAccumulator> summaries = new LinkedHashMap<String, DirectConversationAccumulator>();
            for (DirectMessage message : messages) {
                String partnerUuid = partnerUuid(message, playerUuid);
                String partnerName = partnerName(message, playerUuid);
                DirectConversationAccumulator accumulator = summaries.get(partnerUuid.toLowerCase(Locale.ROOT));
                if (accumulator == null) {
                    accumulator = new DirectConversationAccumulator(partnerUuid, partnerName, message.getId(), message.getSenderUuid(), message.getMessage(), message.getCreatedAt());
                    summaries.put(partnerUuid.toLowerCase(Locale.ROOT), accumulator);
                }
                long lastRead = reads.containsKey(partnerUuid.toLowerCase(Locale.ROOT)) ? reads.get(partnerUuid.toLowerCase(Locale.ROOT)).longValue() : 0L;
                if (message.getReceiverUuid().equalsIgnoreCase(playerUuid) && message.getId() > lastRead) {
                    accumulator.unreadCount++;
                }
            }
            List<DirectConversationSummary> result = new ArrayList<DirectConversationSummary>();
            for (DirectConversationAccumulator accumulator : summaries.values()) {
                result.add(accumulator.toSummary());
                if (result.size() >= Math.max(1, limit)) {
                    break;
                }
            }
            return result;
        });
    }

    @Override
    public List<DirectMessage> findConversationMessages(String playerUuid, String partnerUuid, int limit) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            List<DirectMessage> reversed = new ArrayList<DirectMessage>();
            try {
                statement = connection.prepareStatement(
                        "SELECT id, sender_uuid, sender_name, receiver_uuid, receiver_name, message, created_at "
                                + "FROM " + tablePrefix + "private_messages "
                                + "WHERE ((sender_uuid = ? AND receiver_uuid = ?) OR (sender_uuid = ? AND receiver_uuid = ?)) "
                                + "ORDER BY id DESC LIMIT ?"
                );
                statement.setString(1, playerUuid);
                statement.setString(2, partnerUuid);
                statement.setString(3, partnerUuid);
                statement.setString(4, playerUuid);
                statement.setInt(5, Math.max(1, limit));
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    reversed.add(mapMessage(resultSet));
                }
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
            List<DirectMessage> result = new ArrayList<DirectMessage>();
            for (int index = reversed.size() - 1; index >= 0; index--) {
                result.add(reversed.get(index));
            }
            return result;
        });
    }

    @Override
    public void markConversationRead(String playerUuid, String partnerUuid, long lastMessageId, long readAt) throws SQLException {
        this.database.transaction(connection -> {
            PreparedStatement delete = null;
            PreparedStatement insert = null;
            try {
                delete = connection.prepareStatement("DELETE FROM " + tablePrefix + "private_reads WHERE player_uuid = ? AND partner_uuid = ?");
                delete.setString(1, playerUuid);
                delete.setString(2, partnerUuid);
                delete.executeUpdate();

                insert = connection.prepareStatement("INSERT INTO " + tablePrefix + "private_reads (player_uuid, partner_uuid, last_message_id, read_at) VALUES (?, ?, ?, ?)");
                insert.setString(1, playerUuid);
                insert.setString(2, partnerUuid);
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
    public int countTotalUnread(String playerUuid) throws SQLException {
        int unread = 0;
        for (DirectConversationSummary summary : findConversationSummaries(playerUuid, Integer.MAX_VALUE)) {
            unread += summary.getUnreadCount();
        }
        return unread;
    }

    private List<DirectMessage> findMessagesForPlayer(Connection connection, String playerUuid) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<DirectMessage> result = new ArrayList<DirectMessage>();
        try {
            statement = connection.prepareStatement(
                    "SELECT id, sender_uuid, sender_name, receiver_uuid, receiver_name, message, created_at "
                            + "FROM " + tablePrefix + "private_messages "
                            + "WHERE sender_uuid = ? OR receiver_uuid = ? "
                            + "ORDER BY id DESC"
            );
            statement.setString(1, playerUuid);
            statement.setString(2, playerUuid);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                result.add(mapMessage(resultSet));
            }
            return result;
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
        }
    }

    private Map<String, Long> findReadMap(Connection connection, String playerUuid) throws SQLException {
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        Map<String, Long> result = new LinkedHashMap<String, Long>();
        try {
            statement = connection.prepareStatement("SELECT partner_uuid, last_message_id FROM " + tablePrefix + "private_reads WHERE player_uuid = ?");
            statement.setString(1, playerUuid);
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                result.put(resultSet.getString("partner_uuid").toLowerCase(Locale.ROOT), Long.valueOf(resultSet.getLong("last_message_id")));
            }
            return result;
        } finally {
            closeQuietly(resultSet);
            closeQuietly(statement);
        }
    }

    private DirectMessage mapMessage(ResultSet resultSet) throws SQLException {
        return new DirectMessage(
                resultSet.getLong("id"),
                resultSet.getString("sender_uuid"),
                resultSet.getString("sender_name"),
                resultSet.getString("receiver_uuid"),
                resultSet.getString("receiver_name"),
                resultSet.getString("message"),
                resultSet.getLong("created_at")
        );
    }

    private String partnerUuid(DirectMessage message, String playerUuid) {
        return message.getSenderUuid().equalsIgnoreCase(playerUuid) ? message.getReceiverUuid() : message.getSenderUuid();
    }

    private String partnerName(DirectMessage message, String playerUuid) {
        return message.getSenderUuid().equalsIgnoreCase(playerUuid) ? message.getReceiverName() : message.getSenderName();
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

    private static final class DirectConversationAccumulator {
        private final String partnerUuid;
        private final String partnerName;
        private final long lastMessageId;
        private final String lastSenderUuid;
        private final String lastMessage;
        private final long lastMessageAt;
        private int unreadCount;

        private DirectConversationAccumulator(String partnerUuid, String partnerName, long lastMessageId, String lastSenderUuid, String lastMessage, long lastMessageAt) {
            this.partnerUuid = partnerUuid;
            this.partnerName = partnerName;
            this.lastMessageId = lastMessageId;
            this.lastSenderUuid = lastSenderUuid;
            this.lastMessage = lastMessage;
            this.lastMessageAt = lastMessageAt;
        }

        private DirectConversationSummary toSummary() {
            return new DirectConversationSummary(partnerUuid, partnerName, lastMessageId, lastSenderUuid, lastMessage, lastMessageAt, unreadCount);
        }
    }
}
