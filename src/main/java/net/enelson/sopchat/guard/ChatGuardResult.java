package net.enelson.sopchat.guard;

public final class ChatGuardResult {

    private final boolean allowed;
    private final String message;

    private ChatGuardResult(boolean allowed, String message) {
        this.allowed = allowed;
        this.message = message;
    }

    public static ChatGuardResult allowed() {
        return new ChatGuardResult(true, null);
    }

    public static ChatGuardResult denied(String message) {
        return new ChatGuardResult(false, message);
    }

    public boolean isAllowed() {
        return allowed;
    }

    public String getMessage() {
        return message;
    }
}
