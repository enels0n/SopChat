package net.enelson.sopchat.privatechat;

public final class DirectConversationSummary {

    private final String partnerUuid;
    private final String partnerName;
    private final long lastMessageId;
    private final String lastSenderUuid;
    private final String lastMessage;
    private final long lastMessageAt;
    private final int unreadCount;

    public DirectConversationSummary(String partnerUuid, String partnerName, long lastMessageId, String lastSenderUuid, String lastMessage, long lastMessageAt, int unreadCount) {
        this.partnerUuid = partnerUuid;
        this.partnerName = partnerName;
        this.lastMessageId = lastMessageId;
        this.lastSenderUuid = lastSenderUuid;
        this.lastMessage = lastMessage;
        this.lastMessageAt = lastMessageAt;
        this.unreadCount = unreadCount;
    }

    public String getPartnerUuid() {
        return partnerUuid;
    }

    public String getPartnerName() {
        return partnerName;
    }

    public long getLastMessageId() {
        return lastMessageId;
    }

    public String getLastSenderUuid() {
        return lastSenderUuid;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public long getLastMessageAt() {
        return lastMessageAt;
    }

    public int getUnreadCount() {
        return unreadCount;
    }
}
