package net.enelson.sopchat.command;

import net.enelson.sopchat.SopChatPlugin;
import net.enelson.sopchat.channel.PlayerChannel;
import net.enelson.sopchat.format.ChatFormattingService;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class ChatCommand implements CommandExecutor, TabCompleter {

    private final SopChatPlugin plugin;

    public ChatCommand(SopChatPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("chat")) {
            return handleChat(sender, args);
        }
        if (command.getName().equalsIgnoreCase("channel")) {
            return handleChannel(sender, args);
        }
        sender.sendMessage(color(this.plugin.getMessageConfig().get("player-only", "{prefix}&cКоманда пока не реализована.").replace("{prefix}", prefix())));
        return true;
    }

    private boolean handleChat(CommandSender sender, String[] args) {
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            this.plugin.reloadLocalConfigs();
            sender.sendMessage(color(this.plugin.getMessageConfig().get("reload", "{prefix}&aКонфигурация SopChat перезагружена.").replace("{prefix}", prefix())));
            return true;
        }
        sender.sendMessage(color(prefix() + "&e/chat reload"));
        return true;
    }

    private boolean handleChannel(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(color(this.plugin.getMessageConfig().get("player-only", "{prefix}&cКоманда доступна только игроку.").replace("{prefix}", prefix())));
            return true;
        }
        Player player = (Player) sender;
        if (args.length >= 2 && args[0].equalsIgnoreCase("create")) {
            try {
                PlayerChannel channel = this.plugin.getChannelService().createChannel(player, args[1]);
                sender.sendMessage(color(this.plugin.getMessageConfig().get("channel-created", "{prefix}&aКанал &e{channel}&a создан.")
                        .replace("{prefix}", prefix())
                        .replace("{channel}", channel.getName())));
            } catch (SQLException exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        if (args.length >= 3 && args[0].equalsIgnoreCase("transfer")) {
            Player target = Bukkit.getPlayerExact(args[2]);
            if (target == null) {
                sender.sendMessage(color(prefix() + "&cИгрок не найден."));
                return true;
            }
            try {
                PlayerChannel channel = this.plugin.getChannelService().transferOwnership(player, args[1], target);
                sender.sendMessage(color(this.plugin.getMessageConfig().get("channel-owner-transferred", "{prefix}&aВладелец канала &e{channel}&a изменён на &e{player}&a.")
                        .replace("{prefix}", prefix())
                        .replace("{channel}", channel.getName())
                        .replace("{player}", target.getName())));
            } catch (SQLException exception) {
                sender.sendMessage(color(prefix() + "&c" + exception.getMessage()));
            }
            return true;
        }
        sender.sendMessage(color(prefix() + "&e/channel create <name>"));
        sender.sendMessage(color(prefix() + "&e/channel transfer <channel> <player>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("chat")) {
            return args.length == 1 ? filter(Arrays.asList("reload"), args[0]) : Collections.<String>emptyList();
        }
        if (!command.getName().equalsIgnoreCase("channel")) {
            return Collections.emptyList();
        }
        if (args.length == 1) {
            return filter(Arrays.asList("create", "transfer"), args[0]);
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("transfer")) {
            List<String> result = new ArrayList<String>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                result.add(player.getName());
            }
            return filter(result, args[2]);
        }
        return Collections.emptyList();
    }

    private List<String> filter(List<String> values, String input) {
        List<String> result = new ArrayList<String>();
        String lower = input == null ? "" : input.toLowerCase();
        for (String value : values) {
            if (value.toLowerCase().startsWith(lower)) {
                result.add(value);
            }
        }
        return result;
    }

    private String prefix() {
        return this.plugin.getMessageConfig().get("prefix", "&6SopChat &8| ");
    }

    private String color(String input) {
        ChatFormattingService formatting = this.plugin.getChatFormattingService();
        return formatting.formatSystemMessage(input);
    }
}
