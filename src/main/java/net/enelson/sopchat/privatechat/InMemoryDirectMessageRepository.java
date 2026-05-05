package net.enelson.sopchat.privatechat;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InMemoryDirectMessageRepository implements DirectMessageRepository {

    private final List<DirectMessage> messages = new ArrayList<DirectMessage>();
    private final Map<String, Long> reads = new LinkedHashMap<String, Long>();
    private long nextId = 1L;

    @Override
    public void initialize() {
    }

    @Override
    public DirectMessage saveMessage(String senderUuid, String senderName, String receiverUuid, String receiverName, String message, long createdAt) {
        DirectMessage directMessage = new DirectMessage(nextId++, senderUuid, senderName, receiverUuid, receiverName, message, createdAt);
        this.messages.add(directMessage);
        return directMessage;
    }

    @Override
    public List<DirectConversationSummary> findConversationSummaries(String playerUuid, int limit) {
        Map<String, DirectConversationAccumulator> summaries = new LinkedHashMap<String, DirectConversationAccumulator>();
        for (int index = this.messages.size() - 1; index >= 0; index--) {
            DirectMessage message = this.messages.get(index);
            if (!isParticipant(message, playerUuid)) {
                continue;
            }
            String partnerUuid = partnerUuid(message, playerUuid);
            String partnerName = partnerName(message, playerUuid);
            DirectConversationAccumulator accumulator = summaries.get(partnerUuid);
            if (accumulator == null) {
                accumulator = new DirectConversationAccumulator(partnerUuid, partnerName, message.getId(), message.getSenderUuid(), message.getMessage(), message.getCreatedAt());
                summaries.put(partnerUuid, accumulator);
            }
            long lastRead = lastRead(playerUuid, partnerUuid);
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
    }

    @Override
    public List<DirectMessage> findConversationMessages(String playerUuid, String partnerUuid, int limit) {
        List<DirectMessage> result = new ArrayList<DirectMessage>();
        for (int index = this.messages.size() - 1; index >= 0; index--) {
            DirectMessage message = this.messages.get(index);
            if (isConversationMessage(message, playerUuid, partnerUuid)) {
                result.add(message);
                if (result.size() >= Math.max(1, limit)) {
                    break;
                }
            }
        }
        Collections.reverse(result);
        return result;
    }

    @Override
    public void markConversationRead(String playerUuid, String partnerUuid, long lastMessageId, long readAt) {
        this.reads.put(readKey(playerUuid, partnerUuid), Long.valueOf(lastMessageId));
    }

    @Override
    public int countTotalUnread(String playerUuid) {
        int unread = 0;
        for (DirectConversationSummary summary : findConversationSummaries(playerUuid, Integer.MAX_VALUE)) {
            unread += summary.getUnreadCount();
        }
        return unread;
    }

    private boolean isParticipant(DirectMessage message, String playerUuid) {
        return message.getSenderUuid().equalsIgnoreCase(playerUuid) || message.getReceiverUuid().equalsIgnoreCase(playerUuid);
    }

    private boolean isConversationMessage(DirectMessage message, String playerUuid, String partnerUuid) {
        return (message.getSenderUuid().equalsIgnoreCase(playerUuid) && message.getReceiverUuid().equalsIgnoreCase(partnerUuid))
                || (message.getSenderUuid().equalsIgnoreCase(partnerUuid) && message.getReceiverUuid().equalsIgnoreCase(playerUuid));
    }

    private String partnerUuid(DirectMessage message, String playerUuid) {
        return message.getSenderUuid().equalsIgnoreCase(playerUuid) ? message.getReceiverUuid() : message.getSenderUuid();
    }

    private String partnerName(DirectMessage message, String playerUuid) {
        return message.getSenderUuid().equalsIgnoreCase(playerUuid) ? message.getReceiverName() : message.getSenderName();
    }

    private long lastRead(String playerUuid, String partnerUuid) {
        Long value = this.reads.get(readKey(playerUuid, partnerUuid));
        return value == null ? 0L : value.longValue();
    }

    private String readKey(String playerUuid, String partnerUuid) {
        return playerUuid.toLowerCase(Locale.ROOT) + ":" + partnerUuid.toLowerCase(Locale.ROOT);
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
