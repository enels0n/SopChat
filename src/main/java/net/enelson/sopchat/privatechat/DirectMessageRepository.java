package net.enelson.sopchat.privatechat;

import java.sql.SQLException;
import java.util.List;

public interface DirectMessageRepository {

    void initialize() throws SQLException;

    DirectMessage saveMessage(String senderUuid, String senderName, String receiverUuid, String receiverName, String message, long createdAt) throws SQLException;

    List<DirectConversationSummary> findConversationSummaries(String playerUuid, int limit) throws SQLException;

    List<DirectMessage> findConversationMessages(String playerUuid, String partnerUuid, int limit) throws SQLException;

    void markConversationRead(String playerUuid, String partnerUuid, long lastMessageId, long readAt) throws SQLException;

    int countTotalUnread(String playerUuid) throws SQLException;
}
