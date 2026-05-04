package net.enelson.sopchat.channel;

public final class ChannelMessage {

    private final long id;
    private final long channelId;
    private final String senderUuid;
    private final String senderName;
    private final String message;
    private final long createdAt;

    public ChannelMessage(long id, long channelId, String senderUuid, String senderName, String message, long createdAt) {
        this.id = id;
        this.channelId = channelId;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.message = message;
        this.createdAt = createdAt;
    }

    public long getId() {
        return this.id;
    }

    public long getChannelId() {
        return this.channelId;
    }

    public String getSenderUuid() {
        return this.senderUuid;
    }

    public String getSenderName() {
        return this.senderName;
    }

    public String getMessage() {
        return this.message;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }
}
