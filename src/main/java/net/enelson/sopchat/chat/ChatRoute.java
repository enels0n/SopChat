package net.enelson.sopchat.chat;

public final class ChatRoute {

    private final ChatTypeDefinition type;
    private final String content;

    public ChatRoute(ChatTypeDefinition type, String content) {
        this.type = type;
        this.content = content;
    }

    public ChatTypeDefinition getType() {
        return this.type;
    }

    public String getContent() {
        return this.content;
    }
}
