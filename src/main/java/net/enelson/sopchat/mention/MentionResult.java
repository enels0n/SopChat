package net.enelson.sopchat.mention;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class MentionResult {

    private final String message;
    private final Set<UUID> mentionedPlayers;

    public MentionResult(String message, Set<UUID> mentionedPlayers) {
        this.message = message;
        this.mentionedPlayers = mentionedPlayers == null ? Collections.<UUID>emptySet() : new HashSet<UUID>(mentionedPlayers);
    }

    public String getMessage() {
        return this.message;
    }

    public Set<UUID> getMentionedPlayers() {
        return Collections.unmodifiableSet(this.mentionedPlayers);
    }
}
