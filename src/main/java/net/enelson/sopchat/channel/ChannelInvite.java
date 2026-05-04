package net.enelson.sopchat.channel;

public final class ChannelInvite {

    private final long channelId;
    private final String inviterUuid;
    private final String invitedUuid;
    private final long createdAt;
    private final long expiresAt;

    public ChannelInvite(long channelId, String inviterUuid, String invitedUuid, long createdAt, long expiresAt) {
        this.channelId = channelId;
        this.inviterUuid = inviterUuid;
        this.invitedUuid = invitedUuid;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public long getChannelId() {
        return this.channelId;
    }

    public String getInviterUuid() {
        return this.inviterUuid;
    }

    public String getInvitedUuid() {
        return this.invitedUuid;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public long getExpiresAt() {
        return this.expiresAt;
    }
}
