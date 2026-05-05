package net.enelson.sopchat.gui;

import net.enelson.sopchat.SopChatPlugin;
import net.enelson.sopchat.channel.ChannelInvite;
import net.enelson.sopchat.channel.ChannelMessage;
import net.enelson.sopchat.channel.PlayerChannel;
import net.enelson.sopchat.privatechat.DirectConversationSummary;
import net.enelson.sopchat.privatechat.DirectMessage;
import net.enelson.sopli.lib.SopLib;
import net.enelson.sopli.lib.item.ItemUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class ChatMenuService {

    private final SopChatPlugin plugin;
    private final ItemUtils itemUtils;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("dd.MM HH:mm");

    public ChatMenuService(SopChatPlugin plugin) {
        this.plugin = plugin;
        this.itemUtils = SopLib.getInstance().getItemUtils();
    }

    public void openMain(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 27, color(config().getString("gui.main.title", "&8SopChat")));
        List<String> channelLore = new ArrayList<String>(defaultList("&7Open your channels"));
        try {
            PlayerChannel activeChannel = plugin.getChannelService().getActiveChannel(player);
            channelLore.add("");
            channelLore.add("&7Active: &f" + (activeChannel == null ? "-" : activeChannel.getName()));
        } catch (SQLException ignored) {
        }
        inventory.setItem(10, createConfiguredItem(player, "gui.main.items.channels", Material.BOOK, "&dChannels", channelLore, "menu:view:channels"));
        inventory.setItem(12, createConfiguredItem(player, "gui.main.items.invites", Material.WRITABLE_BOOK, "&eInvites", defaultList("&7Open channel invites"), "menu:view:invites"));
        List<String> privateLore = new ArrayList<String>(defaultList("&7Private message hub"));
        try {
            int unread = plugin.getDirectMessageService().countTotalUnread(player);
            privateLore.add("");
            privateLore.add("&7Unread: &f" + unread);
        } catch (SQLException ignored) {
        }
        inventory.setItem(14, createConfiguredItem(player, "gui.main.items.private-messages", Material.PAPER, "&bPrivate messages", privateLore, "menu:view:private_messages"));
        inventory.setItem(16, createConfiguredItem(player, "gui.main.items.ignore-list", Material.BARRIER, "&cIgnore list", defaultList("&7Manage ignored players"), "menu:view:ignore_list"));
        player.openInventory(inventory);
    }

    public void openChannels(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, color(config().getString("gui.channels.title", "&8Your channels")));
        try {
            List<PlayerChannel> channels = plugin.getChannelService().findAccessibleChannels(player);
            fillChannels(player, inventory, channels);
        } catch (SQLException exception) {
            inventory.setItem(22, createSimpleItem(Material.BARRIER, "&cFailed to load channels", "menu:none", defaultList("&7" + exception.getMessage())));
        }
        inventory.setItem(49, createConfiguredItem(player, "gui.shared.back", Material.ARROW, "&aBack", defaultList("&7Return to the main menu"), "menu:view:main"));
        player.openInventory(inventory);
    }

    public void openInvites(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, color(config().getString("gui.invites.title", "&8Channel invites")));
        try {
            List<ChannelInvite> invites = plugin.getChannelService().findInvites(player);
            fillInvites(player, inventory, invites);
        } catch (SQLException exception) {
            inventory.setItem(22, createSimpleItem(Material.BARRIER, "&cFailed to load invites", "menu:none", defaultList("&7" + exception.getMessage())));
        }
        inventory.setItem(49, createConfiguredItem(player, "gui.shared.back", Material.ARROW, "&aBack", defaultList("&7Return to the main menu"), "menu:view:main"));
        player.openInventory(inventory);
    }

    public void openPrivateMessages(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, color(config().getString("gui.private-messages.title", "&8Private messages")));
        try {
            List<DirectConversationSummary> summaries = plugin.getDirectMessageService().findConversationSummaries(player, 28);
            fillPrivateMessages(inventory, summaries);
        } catch (SQLException exception) {
            inventory.setItem(22, createSimpleItem(Material.BARRIER, "&cFailed to load conversations", "menu:none", defaultList("&7" + exception.getMessage())));
        }
        inventory.setItem(49, createConfiguredItem(player, "gui.shared.back", Material.ARROW, "&aBack", defaultList("&7Return to the main menu"), "menu:view:main"));
        player.openInventory(inventory);
    }

    public void openIgnoreList(Player player) {
        Inventory inventory = Bukkit.createInventory(null, 54, color(config().getString("gui.ignore-list.title", "&8Ignore list")));
        fillIgnoreList(inventory, player);
        inventory.setItem(49, createConfiguredItem(player, "gui.shared.back", Material.ARROW, "&aBack", defaultList("&7Return to the main menu"), "menu:view:main"));
        player.openInventory(inventory);
    }

    public void openChannelHistory(Player player, String channelName) {
        Inventory inventory = Bukkit.createInventory(null, 54, color(config().getString("gui.channel-history.title", "&8Channel history").replace("{channel}", channelName)));
        try {
            List<ChannelMessage> messages = plugin.getChannelService().findRecentMessages(channelName, Math.max(1, config().getInt("channels.history.preview-on-open", 10)));
            fillChannelHistory(inventory, messages);
        } catch (SQLException exception) {
            inventory.setItem(22, createSimpleItem(Material.BARRIER, "&cFailed to load history", "menu:none", defaultList("&7" + exception.getMessage())));
        }
        inventory.setItem(49, createConfiguredItem(player, "gui.shared.back-to-channels", Material.ARROW, "&aBack", defaultList("&7Return to channels"), "menu:view:channels"));
        player.openInventory(inventory);
    }

    public void openPrivateConversation(Player player, String partnerUuidText) {
        OfflinePlayer partner = Bukkit.getOfflinePlayer(java.util.UUID.fromString(partnerUuidText));
        String partnerName = partner.getName() == null ? partnerUuidText : partner.getName();
        Inventory inventory = Bukkit.createInventory(null, 54, color(config().getString("gui.private-messages.conversation-title", "&8Conversation: {player}").replace("{player}", partnerName)));
        try {
            List<DirectMessage> messages = plugin.getDirectMessageService().findConversationMessages(player, partner.getUniqueId(), 28);
            fillPrivateConversation(inventory, player, messages);
            if (!messages.isEmpty()) {
                plugin.getDirectMessageService().markConversationRead(player, partner.getUniqueId(), messages.get(messages.size() - 1).getId());
            }
        } catch (Exception exception) {
            inventory.setItem(22, createSimpleItem(Material.BARRIER, "&cFailed to load conversation", "menu:none", defaultList("&7" + exception.getMessage())));
        }
        inventory.setItem(49, createConfiguredItem(player, "gui.shared.back-to-private-messages", Material.ARROW, "&aBack", defaultList("&7Return to private messages"), "menu:view:private_messages"));
        player.openInventory(inventory);
    }

    public boolean isManagedInventory(String title) {
        if (title == null) {
            return false;
        }
        String stripped = org.bukkit.ChatColor.stripColor(title);
        if (stripped == null) {
            return false;
        }
        String lower = stripped.toLowerCase(Locale.ROOT);
        return lower.contains("sopchat")
                || lower.contains("your channels")
                || lower.contains("channel invites")
                || lower.contains("private messages")
                || lower.contains("ignore list")
                || lower.contains("channel history")
                || lower.contains("conversation");
    }

    private void fillChannels(Player player, Inventory inventory, List<PlayerChannel> channels) throws SQLException {
        if (channels.isEmpty()) {
            inventory.setItem(22, createConfiguredItem(player, "gui.channels.empty", Material.GRAY_STAINED_GLASS_PANE, "&7No channels yet", defaultList("&7Create one with &e/channel create <name>"), "menu:none"));
            return;
        }
        PlayerChannel activeChannel = plugin.getChannelService().getActiveChannel(player);
        int slot = 10;
        for (PlayerChannel channel : channels) {
            if (slot >= 44) {
                break;
            }
            int unread = plugin.getChannelService().countUnread(channel.getName(), player);
            boolean active = activeChannel != null && activeChannel.getName().equalsIgnoreCase(channel.getName());
            List<String> lore = defaultList(
                    "&7Owner: &f" + resolveOwnerName(channel),
                    "&7Unread: &f" + unread,
                    "&7Active: " + (active ? "&ayes" : "&cno"),
                    "&7Created at: &f" + formatTime(channel.getCreatedAt()),
                    "",
                    "&eLeft click to open history",
                    "&bRight click to set active channel",
                    "&7Shift-right click to disable active channel"
            );
            inventory.setItem(slot, createSimpleItem(active ? Material.ENCHANTED_BOOK : Material.BOOK, "&d" + channel.getName(), "menu:channel:" + channel.getName(), lore));
            slot = nextContentSlot(slot);
        }
    }

    private void fillInvites(Player player, Inventory inventory, List<ChannelInvite> invites) throws SQLException {
        if (invites.isEmpty()) {
            inventory.setItem(22, createSimpleItem(Material.GRAY_STAINED_GLASS_PANE, "&7No invites", "menu:none", defaultList("&7You have no active channel invites.")));
            return;
        }
        int slot = 10;
        for (ChannelInvite invite : invites) {
            if (slot >= 44) {
                break;
            }
            PlayerChannel channel = plugin.getChannelService().findChannel(invite.getChannelId());
            String channelName = channel == null ? ("#" + invite.getChannelId()) : channel.getName();
            List<String> lore = defaultList(
                    "&7Channel: &f" + channelName,
                    "&7Inviter UUID: &f" + invite.getInviterUuid(),
                    "&7Expires: &f" + formatTime(invite.getExpiresAt()),
                    "",
                    "&aLeft click to accept",
                    "&cRight click to deny"
            );
            inventory.setItem(slot, createSimpleItem(Material.WRITABLE_BOOK, "&eInvite: " + channelName, "menu:invite:" + invite.getChannelId(), lore));
            slot = nextContentSlot(slot);
        }
    }

    private void fillChannelHistory(Inventory inventory, List<ChannelMessage> messages) {
        if (messages.isEmpty()) {
            inventory.setItem(22, createSimpleItem(Material.GRAY_STAINED_GLASS_PANE, "&7No messages yet", "menu:none", defaultList("&7This channel has no stored messages.")));
            return;
        }
        int slot = 10;
        List<ChannelMessage> reversed = new ArrayList<ChannelMessage>(messages);
        Collections.reverse(reversed);
        for (ChannelMessage message : reversed) {
            if (slot >= 44) {
                break;
            }
            List<String> lore = defaultList(
                    "&7At: &f" + formatTime(message.getCreatedAt()),
                    "",
                    "&f" + message.getMessage()
            );
            inventory.setItem(slot, createSimpleItem(Material.PAPER, "&b" + message.getSenderName(), "menu:none", lore));
            slot = nextContentSlot(slot);
        }
    }

    private void fillPrivateMessages(Inventory inventory, List<DirectConversationSummary> summaries) {
        if (summaries.isEmpty()) {
            inventory.setItem(22, createConfiguredItem(null, "gui.private-messages.empty", Material.GRAY_STAINED_GLASS_PANE, "&7No conversations yet", defaultList("&7Use &e/m <player> <message>"), "menu:none"));
            return;
        }
        int slot = 10;
        for (DirectConversationSummary summary : summaries) {
            if (slot >= 44) {
                break;
            }
            List<String> lore = defaultList(
                    "&7Last at: &f" + formatTime(summary.getLastMessageAt()),
                    "&7Unread: &f" + summary.getUnreadCount(),
                    "",
                    "&f" + trim(summary.getLastMessage(), 60),
                    "",
                    "&eClick to open conversation"
            );
            inventory.setItem(slot, createSimpleItem(Material.PAPER, "&b" + summary.getPartnerName(), "menu:pm:" + summary.getPartnerUuid(), lore));
            slot = nextContentSlot(slot);
        }
    }

    private void fillPrivateConversation(Inventory inventory, Player viewer, List<DirectMessage> messages) {
        if (messages.isEmpty()) {
            inventory.setItem(22, createSimpleItem(Material.GRAY_STAINED_GLASS_PANE, "&7No messages yet", "menu:none", defaultList("&7This conversation has no messages.")));
            return;
        }
        int slot = 10;
        for (DirectMessage message : messages) {
            if (slot >= 44) {
                break;
            }
            boolean outgoing = message.getSenderUuid().equalsIgnoreCase(viewer.getUniqueId().toString());
            String title = outgoing ? "&aYou" : "&b" + message.getSenderName();
            Material material = outgoing ? Material.LIME_DYE : Material.LIGHT_BLUE_DYE;
            List<String> lore = defaultList(
                    "&7At: &f" + formatTime(message.getCreatedAt()),
                    "",
                    "&f" + message.getMessage()
            );
            inventory.setItem(slot, createSimpleItem(material, title, "menu:none", lore));
            slot = nextContentSlot(slot);
        }
    }

    private void fillIgnoreList(Inventory inventory, Player player) {
        List<java.util.UUID> ignored = new ArrayList<java.util.UUID>(plugin.getChatCommand().getIgnoredPlayerIds(player));
        if (ignored.isEmpty()) {
            inventory.setItem(22, createConfiguredItem(player, "gui.ignore-list.empty", Material.GRAY_STAINED_GLASS_PANE, "&7Nobody ignored", defaultList("&7Your ignore list is empty."), "menu:none"));
            return;
        }
        int slot = 10;
        for (java.util.UUID uuid : ignored) {
            if (slot >= 44) {
                break;
            }
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String name = offlinePlayer.getName() == null ? uuid.toString() : offlinePlayer.getName();
            inventory.setItem(slot, createSimpleItem(Material.BARRIER, "&c" + name, "menu:ignore:" + uuid.toString(), defaultList("&7Ignored player", "", "&eClick to unignore")));
            slot = nextContentSlot(slot);
        }
    }

    private String resolveOwnerName(PlayerChannel channel) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(java.util.UUID.fromString(channel.getOwnerUuid()));
        return owner.getName() == null ? channel.getOwnerUuid() : owner.getName();
    }

    private void openPlaceholder(Player player, String title, String path, Material fallbackMaterial, String fallbackName, List<String> fallbackLore) {
        Inventory inventory = Bukkit.createInventory(null, 27, color(title));
        inventory.setItem(13, createConfiguredItem(player, path, fallbackMaterial, fallbackName, fallbackLore, "menu:none"));
        inventory.setItem(22, createConfiguredItem(player, "gui.shared.back", Material.ARROW, "&aBack", defaultList("&7Return to the main menu"), "menu:view:main"));
        player.openInventory(inventory);
    }

    private ItemStack createConfiguredItem(Player player, String path, Material fallbackMaterial, String fallbackName, List<String> fallbackLore, String customKey) {
        return createItem(MenuItemSpec.fromSection(config().getConfigurationSection(path), fallbackMaterial, fallbackName, fallbackLore), customKey);
    }

    private ItemStack createSimpleItem(Material material, String name, String customKey, List<String> lore) {
        return createItem(new MenuItemSpec(material.name(), material, name, null, lore), customKey);
    }

    private ItemStack createItem(MenuItemSpec spec, String customKey) {
        ItemStack item = createBaseItem(spec.getMaterialSpec(), spec.getFallbackMaterial(), spec.getCustomModelData());
        itemUtils.setCustomItemKey(item, customKey, spec.getName());
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(spec.getName()));
            meta.setLore(color(spec.getLore()));
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBaseItem(String materialSpec, Material fallbackMaterial, Integer customModelData) {
        ItemStack item;
        if (materialSpec != null && materialSpec.regionMatches(true, 0, "HEAD:", 0, 5)) {
            String texture = materialSpec.substring(5);
            item = itemUtils.getHeadTexture(texture, null);
            if (item == null) {
                item = new ItemStack(Material.PLAYER_HEAD);
            }
        } else {
            Material material = Material.matchMaterial(materialSpec == null ? fallbackMaterial.name() : materialSpec);
            item = new ItemStack(material == null ? fallbackMaterial : material);
        }
        if (customModelData != null) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setCustomModelData(customModelData);
                item.setItemMeta(meta);
            }
        }
        return item;
    }

    private int nextContentSlot(int current) {
        if (current % 9 == 7) {
            return current + 3;
        }
        return current + 1;
    }

    private String formatTime(long epochSeconds) {
        return timeFormatter.format(Instant.ofEpochSecond(epochSeconds).atZone(ZoneId.systemDefault()));
    }

    private List<String> defaultList(String... lines) {
        List<String> result = new ArrayList<String>();
        if (lines != null) {
            Collections.addAll(result, lines);
        }
        return result;
    }

    private org.bukkit.configuration.file.FileConfiguration config() {
        return plugin.getConfig();
    }

    public SopChatPlugin getPlugin() {
        return plugin;
    }

    public String color(String input) {
        return plugin.getChatFormattingService().formatSystemMessage(input);
    }

    private List<String> color(List<String> input) {
        List<String> result = new ArrayList<String>();
        for (String line : input) {
            result.add(color(line));
        }
        return result;
    }

    private String trim(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
