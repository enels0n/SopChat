package net.enelson.sopchat;

import net.enelson.sopchat.channel.ChannelService;
import net.enelson.sopchat.chat.ChatTypeService;
import net.enelson.sopchat.command.ChatCommand;
import net.enelson.sopchat.config.MessageConfig;
import net.enelson.sopchat.format.ChatFormattingService;
import net.enelson.sopchat.listener.ChatListener;
import net.enelson.sopli.lib.SopLib;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SopChatPlugin extends JavaPlugin {

    private YamlConfiguration chatTypesConfig;
    private YamlConfiguration messageConfigFile;
    private MessageConfig messageConfig;
    private ChatTypeService chatTypeService;
    private ChatFormattingService chatFormattingService;
    private ChannelService channelService;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("chat-types.yml");
        saveResourceIfMissing("messages.yml");
        reloadLocalConfigs();

        this.chatTypeService = new ChatTypeService(this);
        this.chatFormattingService = new ChatFormattingService(this, SopLib.getInstance().getTextUtils());
        this.channelService = new ChannelService(this);

        ChatCommand chatCommand = new ChatCommand(this);
        getCommand("chat").setExecutor(chatCommand);
        getCommand("chat").setTabCompleter(chatCommand);
        getCommand("channel").setExecutor(chatCommand);
        getCommand("channel").setTabCompleter(chatCommand);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
    }

    public void reloadLocalConfigs() {
        reloadConfig();
        this.chatTypesConfig = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "chat-types.yml"));
        this.messageConfigFile = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));
        this.messageConfig = new MessageConfig(this.messageConfigFile);
        if (this.chatTypeService != null) {
            this.chatTypeService.reload();
        }
    }

    public YamlConfiguration getChatTypesConfig() {
        return this.chatTypesConfig;
    }

    public MessageConfig getMessageConfig() {
        return this.messageConfig;
    }

    public ChatTypeService getChatTypeService() {
        return this.chatTypeService;
    }

    public ChatFormattingService getChatFormattingService() {
        return this.chatFormattingService;
    }

    public ChannelService getChannelService() {
        return this.channelService;
    }

    private void saveResourceIfMissing(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            saveResource(path, false);
        }
    }
}
