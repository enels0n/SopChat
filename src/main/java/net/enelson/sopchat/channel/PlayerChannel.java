package net.enelson.sopchat.channel;

public final class PlayerChannel {

    private final long id;
    private final String name;
    private final String ownerUuid;
    private final long createdAt;

    public PlayerChannel(long id, String name, String ownerUuid, long createdAt) {
        this.id = id;
        this.name = name;
        this.ownerUuid = ownerUuid;
        this.createdAt = createdAt;
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getOwnerUuid() {
        return this.ownerUuid;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public PlayerChannel withOwner(String ownerUuid) {
        return new PlayerChannel(this.id, this.name, ownerUuid, this.createdAt);
    }
}
