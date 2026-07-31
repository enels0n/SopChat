package net.enelson.sopchat.chat;

public final class ChatTypeDefinition {

    private final String id;
    private final String trigger;
    private final ChatTypeMode mode;
    private final int radius;
    private final String permission;
    private final boolean denyIfNoPermission;
    private final boolean mentionEnabled;
    private final String format;
    private final ChatConditions conditions;
    private final ChatConditions visibilityConditions;

    public ChatTypeDefinition(String id, String trigger, ChatTypeMode mode, int radius, String permission, boolean denyIfNoPermission, boolean mentionEnabled, String format, ChatConditions conditions, ChatConditions visibilityConditions) {
        this.id = id;
        this.trigger = trigger;
        this.mode = mode;
        this.radius = radius;
        this.permission = permission;
        this.denyIfNoPermission = denyIfNoPermission;
        this.mentionEnabled = mentionEnabled;
        this.format = format;
        this.conditions = conditions == null ? ChatConditions.alwaysAllowed() : conditions;
        this.visibilityConditions = visibilityConditions == null ? ChatConditions.alwaysAllowed() : visibilityConditions;
    }

    public String getId() {
        return this.id;
    }

    public String getTrigger() {
        return this.trigger;
    }

    public ChatTypeMode getMode() {
        return this.mode;
    }

    public int getRadius() {
        return this.radius;
    }

    public String getPermission() {
        return this.permission;
    }

    public boolean isDenyIfNoPermission() {
        return this.denyIfNoPermission;
    }

    public boolean isMentionEnabled() {
        return this.mentionEnabled;
    }

    public String getFormat() {
        return this.format;
    }

    public ChatConditions getConditions() {
        return this.conditions;
    }

    public ChatConditions getVisibilityConditions() {
        return this.visibilityConditions;
    }
}
