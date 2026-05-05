package net.enelson.sopchat.preference;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class PlayerPreferenceService {

    private final PlayerPreferenceRepository repository;

    public PlayerPreferenceService() {
        this(new InMemoryPlayerPreferenceRepository());
    }

    public PlayerPreferenceService(PlayerPreferenceRepository repository) {
        this.repository = repository;
    }

    public boolean toggleSocialSpy(Player player) throws SQLException {
        boolean enabled = !this.repository.isSocialSpyEnabled(player.getUniqueId().toString());
        this.repository.setSocialSpy(player.getUniqueId().toString(), enabled);
        return enabled;
    }

    public boolean isSocialSpyEnabled(Player player) throws SQLException {
        return player != null && this.repository.isSocialSpyEnabled(player.getUniqueId().toString());
    }

    public List<UUID> findSocialSpyEnabled() throws SQLException {
        List<UUID> result = new ArrayList<UUID>();
        for (String uuidText : this.repository.findSocialSpyEnabledPlayers()) {
            try {
                result.add(UUID.fromString(uuidText));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public void addIgnore(Player owner, OfflinePlayer target) throws SQLException {
        this.repository.addIgnore(owner.getUniqueId().toString(), target.getUniqueId().toString());
    }

    public void removeIgnore(Player owner, UUID targetId) throws SQLException {
        this.repository.removeIgnore(owner.getUniqueId().toString(), targetId.toString());
    }

    public boolean isIgnoring(OfflinePlayer owner, Player target) throws SQLException {
        return owner != null && target != null && this.repository.isIgnoring(owner.getUniqueId().toString(), target.getUniqueId().toString());
    }

    public List<UUID> findIgnoredPlayerIds(Player owner) throws SQLException {
        if (owner == null) {
            return Collections.emptyList();
        }
        List<UUID> result = new ArrayList<UUID>();
        for (String uuidText : this.repository.findIgnoredPlayers(owner.getUniqueId().toString())) {
            try {
                result.add(UUID.fromString(uuidText));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    public List<OfflinePlayer> findIgnoredPlayers(Player owner) throws SQLException {
        List<OfflinePlayer> result = new ArrayList<OfflinePlayer>();
        for (UUID uuid : findIgnoredPlayerIds(owner)) {
            result.add(Bukkit.getOfflinePlayer(uuid));
        }
        return result;
    }

    public void setActiveChannel(Player player, String channelName) throws SQLException {
        if (player != null) {
            this.repository.setActiveChannel(player.getUniqueId().toString(), channelName);
        }
    }

    public String findActiveChannel(Player player) throws SQLException {
        return player == null ? null : this.repository.findActiveChannel(player.getUniqueId().toString());
    }
}
