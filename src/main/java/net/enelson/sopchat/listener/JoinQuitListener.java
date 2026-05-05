package net.enelson.sopchat.listener;

import net.enelson.sopchat.SopChatPlugin;
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
                if (permission == null || permission.isEmpty() || player.hasPermission(permission)) {
                    Object formatObject = entry.get("format");
                    format = formatObject == null ? format : formatObject.toString();
                    break;
                }
            }
        }
        return plugin.getChatFormattingService().formatSystemMessage(format.replace("{player}", player.getName()));
    }
}
