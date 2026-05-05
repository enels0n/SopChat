package net.enelson.sopchat.privatechat;

public final class DirectMessage {

    private final long id;
    private final String senderUuid;
    private final String senderName;
    private final String receiverUuid;
    private final String receiverName;
    private final String message;
    private final long createdAt;

    public DirectMessage(long id, String senderUuid, String senderName, String receiverUuid, String receiverName, String message, long createdAt) {
        this.id = id;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.receiverUuid = receiverUuid;
        this.receiverName = receiverName;
        this.message = message;
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public String getSenderUuid() {
        return senderUuid;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getReceiverUuid() {
        return receiverUuid;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getMessage() {
        return message;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
