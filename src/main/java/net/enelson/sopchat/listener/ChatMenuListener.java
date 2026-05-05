package net.enelson.sopchat.listener;

import net.enelson.sopchat.channel.PlayerChannel;
import net.enelson.sopchat.gui.ChatMenuService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public final class ChatMenuListener implements Listener {

    private final ChatMenuService menuService;

    public ChatMenuListener(ChatMenuService menuService) {
        this.menuService = menuService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        if (!menuService.isManagedInventory(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
        ItemStack current = event.getCurrentItem();
        if (current == null) {
            return;
        }
        String customKey = net.enelson.sopli.lib.SopLib.getInstance().getItemUtils().getCustomItemKey(current);
        if (customKey == null || customKey.isEmpty()) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if ("menu:view:main".equalsIgnoreCase(customKey)) {
            menuService.openMain(player);
            return;
        }
        if ("menu:view:channels".equalsIgnoreCase(customKey)) {
            menuService.openChannels(player);
            return;
        }
        if ("menu:view:invites".equalsIgnoreCase(customKey)) {
            menuService.openInvites(player);
            return;
        }
        if ("menu:view:private_messages".equalsIgnoreCase(customKey)) {
            menuService.openPrivateMessages(player);
            return;
        }
        if ("menu:view:ignore_list".equalsIgnoreCase(customKey)) {
            menuService.openIgnoreList(player);
            return;
        }
        if (customKey.startsWith("menu:channel:")) {
            String channelName = customKey.substring("menu:channel:".length());
            try {
                if (event.getClick() == ClickType.RIGHT) {
                    if (event.isShiftClick()) {
                        menuService.getPlugin().getChannelService().clearActiveChannel(player);
                        player.sendMessage(menuService.color(menuService.getPlugin().getMessageConfig().get("channel-active-cleared", "{prefix}&eАктивный канал отключён.")
                                .replace("{prefix}", menuService.getPlugin().getMessageConfig().get("prefix", "&6SopChat &8| "))));
                    } else {
                        menuService.getPlugin().getChannelService().setActiveChannel(player, channelName);
                        player.sendMessage(menuService.color(menuService.getPlugin().getMessageConfig().get("channel-active-set", "{prefix}&aАктивный канал переключён на &e{channel}&a.")
                                .replace("{prefix}", menuService.getPlugin().getMessageConfig().get("prefix", "&6SopChat &8| "))
                                .replace("{channel}", channelName)));
                    }
                    menuService.openChannels(player);
                } else {
                    menuService.openChannelHistory(player, channelName);
                }
            } catch (Exception exception) {
                player.sendMessage(menuService.color("&6SopChat &8| &c" + exception.getMessage()));
            }
            return;
        }
        if (customKey.startsWith("menu:pm:")) {
            menuService.openPrivateConversation(player, customKey.substring("menu:pm:".length()));
            return;
        }
        if (customKey.startsWith("menu:ignore:")) {
            String uuidText = customKey.substring("menu:ignore:".length());
            try {
                java.util.UUID targetId = java.util.UUID.fromString(uuidText);
                if (menuService.getPlugin().getChatCommand().unignore(player, targetId)) {
                    org.bukkit.OfflinePlayer target = org.bukkit.Bukkit.getOfflinePlayer(targetId);
                    String targetName = target.getName() == null ? uuidText : target.getName();
                    player.sendMessage(menuService.color(menuService.getPlugin().getMessageConfig().get("ignore-removed", "{prefix}&aВы больше не игнорируете &e{player}&a.")
                            .replace("{prefix}", menuService.getPlugin().getMessageConfig().get("prefix", "&6SopChat &8| "))
                            .replace("{player}", targetName)));
                }
                menuService.openIgnoreList(player);
            } catch (Exception exception) {
                player.sendMessage(menuService.color("&6SopChat &8| &c" + exception.getMessage()));
            }
            return;
        }
        if (customKey.startsWith("menu:invite:")) {
            String idText = customKey.substring("menu:invite:".length());
            try {
                long channelId = Long.parseLong(idText);
                if (event.getClick() == ClickType.RIGHT) {
                    menuService.getPlugin().getChannelService().denyInvite(player, channelId);
                    player.sendMessage(menuService.color(menuService.getPlugin().getMessageConfig().get("channel-invite-denied", "{prefix}&eПриглашение отклонено.").replace("{prefix}", menuService.getPlugin().getMessageConfig().get("prefix", "&6SopChat &8| "))));
                } else {
                    PlayerChannel channel = menuService.getPlugin().getChannelService().acceptInvite(player, channelId);
                    player.sendMessage(menuService.color(menuService.getPlugin().getMessageConfig().get("channel-invite-accepted", "{prefix}&aВы вступили в канал &e{channel}&a.")
                            .replace("{prefix}", menuService.getPlugin().getMessageConfig().get("prefix", "&6SopChat &8| "))
                            .replace("{channel}", channel.getName())));
                }
                menuService.openInvites(player);
            } catch (Exception exception) {
                player.sendMessage(menuService.color("&6SopChat &8| &c" + exception.getMessage()));
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!menuService.isManagedInventory(event.getView().getTitle())) {
            return;
        }
        event.setCancelled(true);
    }
}
