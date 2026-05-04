package net.enelson.sopchat.channel;

import net.enelson.sopchat.SopChatPlugin;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

public final class ChannelService {

    private final SopChatPlugin plugin;
    private final ChannelRepository repository;

    public ChannelService(SopChatPlugin plugin) {
        this.plugin = plugin;
        this.repository = new InMemoryChannelRepository();
    }

    public PlayerChannel createChannel(Player owner, String name) throws SQLException {
        ensurePlayerCanOwnMore(owner);
        return this.repository.createChannel(name, owner.getUniqueId().toString());
    }

    public PlayerChannel transferOwnership(Player actor, String channelName, Player newOwner) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        if (!channel.getOwnerUuid().equalsIgnoreCase(actor.getUniqueId().toString())) {
            throw new SQLException("You are not the channel owner");
        }
        ensurePlayerCanOwnMore(newOwner);
        this.repository.ensureMembership(channel.getId(), newOwner.getUniqueId().toString(), ChannelMemberRole.MEMBER);
        return this.repository.updateOwner(channel.getId(), newOwner.getUniqueId().toString());
    }

    public boolean isOwner(Player player, String channelName) throws SQLException {
        PlayerChannel channel = requireChannel(channelName);
        return channel.getOwnerUuid().equalsIgnoreCase(player.getUniqueId().toString());
    }

    private PlayerChannel requireChannel(String name) throws SQLException {
        PlayerChannel channel = this.repository.findChannelByName(name);
        if (channel == null) {
            throw new SQLException("Channel not found");
        }
        return channel;
    }

    private void ensurePlayerCanOwnMore(Player player) throws SQLException {
        int limit = resolveOwnedChannelLimit(player);
        if (limit <= 0) {
            return;
        }
        List<PlayerChannel> owned = this.repository.findOwnedChannels(player.getUniqueId().toString());
        if (owned.size() >= limit) {
            throw new SQLException("Owned channel limit reached");
        }
    }

    private int resolveOwnedChannelLimit(Player player) {
        int limit = this.plugin.getConfig().getInt("channels.limits.owned-per-player", 1);
        for (org.bukkit.permissions.PermissionAttachmentInfo info : player.getEffectivePermissions()) {
            String permission = info.getPermission().toLowerCase(Locale.ROOT);
            if (!permission.startsWith("sopchat.limit.owned_channels.") || !info.getValue()) {
                continue;
            }
            String suffix = permission.substring("sopchat.limit.owned_channels.".length());
            try {
                limit = Math.max(limit, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
            }
        }
        return limit;
    }
}
