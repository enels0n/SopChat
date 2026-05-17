package net.enelson.sopchat.listener;

import me.clip.placeholderapi.PlaceholderAPI;
import net.enelson.sopchat.SopChatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;

public final class JoinQuitListener implements Listener {

    private final SopChatPlugin plugin;

    public JoinQuitListener(SopChatPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("join-quit.enabled", true)) {
            return;
        }
        if (!event.getPlayer().hasPlayedBefore()) {
            event.setJoinMessage(resolveMessage(event.getPlayer(), "join-quit.first-join", "&6✦ {player}"));
            return;
        }
        event.setJoinMessage(resolveMessage(event.getPlayer(), "join-quit.join", "&a+ {player}"));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (!plugin.getConfig().getBoolean("join-quit.enabled", true)) {
            return;
        }
        event.setQuitMessage(resolveMessage(event.getPlayer(), "join-quit.quit", "&c- {player}"));
    }

    private String resolveMessage(Player player, String path, String fallback) {
        String format = plugin.getConfig().getString(path + ".default", fallback);
        List<Map<?, ?>> rules = plugin.getConfig().getMapList(path + ".rules");
        if (rules != null) {
            for (Map<?, ?> entry : rules) {
                Object permissionObject = entry.get("permission");
                String permission = permissionObject == null ? "" : permissionObject.toString();
                if (permission == null || permission.isEmpty() || player.isPermissionSet(permission)) {
                    Object formatObject = entry.get("format");
                    format = formatObject == null ? format : formatObject.toString();
                    break;
                }
            }
        }
        String resolved = format.replace("{player}", player.getName());
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                resolved = PlaceholderAPI.setPlaceholders(player, resolved);
            } catch (Throwable ignored) {
                // Keep raw system message if PlaceholderAPI fails unexpectedly.
            }
        }
        return plugin.getChatFormattingService().formatSystemMessage(resolved);
    }
}
