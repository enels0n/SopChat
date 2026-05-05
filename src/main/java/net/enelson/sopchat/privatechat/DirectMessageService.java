package net.enelson.sopchat.privatechat;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class DirectMessageService {

    private final DirectMessageRepository repository;

    public DirectMessageService() {
        this(new InMemoryDirectMessageRepository());
    }

    public DirectMessageService(DirectMessageRepository repository) {
        this.repository = repository;
    }

    public DirectMessage saveMessage(Player sender, OfflinePlayer target, String targetName, String message) throws SQLException {
        String resolvedTargetName = targetName != null && !targetName.isEmpty() ? targetName : (target.getName() == null ? "Unknown" : target.getName());
        return this.repository.saveMessage(
                sender.getUniqueId().toString(),
                sender.getName(),
                target.getUniqueId().toString(),
                resolvedTargetName,
                message,
                System.currentTimeMillis() / 1000L
        );
    }

    public List<DirectConversationSummary> findConversationSummaries(Player player, int limit) throws SQLException {
        if (player == null) {
            return Collections.emptyList();
        }
        return this.repository.findConversationSummaries(player.getUniqueId().toString(), limit);
    }

    public List<DirectMessage> findConversationMessages(Player player, UUID partnerUuid, int limit) throws SQLException {
        if (player == null || partnerUuid == null) {
            return Collections.emptyList();
        }
        return this.repository.findConversationMessages(player.getUniqueId().toString(), partnerUuid.toString(), limit);
    }

    public void markConversationRead(Player player, UUID partnerUuid, long lastMessageId) throws SQLException {
        if (player == null || partnerUuid == null || lastMessageId <= 0L) {
            return;
        }
        this.repository.markConversationRead(player.getUniqueId().toString(), partnerUuid.toString(), lastMessageId, System.currentTimeMillis() / 1000L);
    }

    public int countTotalUnread(Player player) throws SQLException {
        if (player == null) {
            return 0;
        }
        return this.repository.countTotalUnread(player.getUniqueId().toString());
    }

    public OfflinePlayer findKnownPlayer(String input) {
        if (input == null || input.trim().isEmpty()) {
            return null;
        }
        Player online = Bukkit.getPlayerExact(input);
        if (online != null) {
            return online;
        }
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(input)) {
                return offlinePlayer;
            }
        }
        return null;
    }
}
