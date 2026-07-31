package net.enelson.sopchat;

import net.enelson.sopchat.channel.ChannelService;
import net.enelson.sopchat.chat.ChatTypeService;
import net.enelson.sopchat.command.ChatCommand;
import net.enelson.sopchat.config.MessageConfig;
import net.enelson.sopchat.format.ChatFormattingService;
import net.enelson.sopchat.guard.ChatGuardService;
import net.enelson.sopchat.gui.ChatMenuService;
import net.enelson.sopchat.listener.ChatListener;
import net.enelson.sopchat.listener.ChatMenuListener;
import net.enelson.sopchat.listener.JoinQuitListener;
import net.enelson.sopchat.listener.PlayerSessionListener;
import net.enelson.sopchat.mention.MentionService;
import net.enelson.sopchat.moderation.ModerationService;
import net.enelson.sopchat.preference.PlayerPreferenceService;
import net.enelson.sopchat.privatechat.DirectMessageService;
import net.enelson.sopchat.storage.DatabaseFactory;
import net.enelson.sopchat.storage.SqlChannelRepository;
import net.enelson.sopchat.storage.SqlDirectMessageRepository;
import net.enelson.sopchat.storage.SqlPlayerPreferenceRepository;
import net.enelson.sopli.lib.SopLib;
import net.enelson.sopli.lib.database.SopDatabase;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;

public final class SopChatPlugin extends JavaPlugin {

    private YamlConfiguration chatTypesConfig;
    private YamlConfiguration messageConfigFile;
    private MessageConfig messageConfig;
    private ChatTypeService chatTypeService;
    private ChatFormattingService chatFormattingService;
    private ChatGuardService chatGuardService;
    private ChannelService channelService;
    private DirectMessageService directMessageService;
    private PlayerPreferenceService playerPreferenceService;
    private MentionService mentionService;
    private ModerationService moderationService;
    private ChatMenuService chatMenuService;
    private ChatCommand chatCommand;
    private SopDatabase database;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("chat-types.yml");
        saveResourceIfMissing("messages.yml");
        reloadLocalConfigs();

        this.chatTypeService = new ChatTypeService(this);
        this.chatFormattingService = new ChatFormattingService(this, SopLib.getInstance().getTextUtils());
        this.chatGuardService = new ChatGuardService(this);
        this.mentionService = new MentionService(this, SopLib.getInstance().getTextUtils());
        this.moderationService = new ModerationService();
        initializeStorage();
        this.chatMenuService = new ChatMenuService(this);

        this.chatCommand = new ChatCommand(this);
        getCommand("chat").setExecutor(chatCommand);
        getCommand("chat").setTabCompleter(chatCommand);
        getCommand("channel").setExecutor(chatCommand);
        getCommand("channel").setTabCompleter(chatCommand);
        getCommand("msg").setExecutor(chatCommand);
        getCommand("msg").setTabCompleter(chatCommand);
        getCommand("reply").setExecutor(chatCommand);
        getCommand("reply").setTabCompleter(chatCommand);
        getCommand("ignore").setExecutor(chatCommand);
        getCommand("ignore").setTabCompleter(chatCommand);
        getCommand("unignore").setExecutor(chatCommand);
        getCommand("unignore").setTabCompleter(chatCommand);
        getCommand("socialspy").setExecutor(chatCommand);
        getCommand("socialspy").setTabCompleter(chatCommand);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatMenuListener(this.chatMenuService), this);
        getServer().getPluginManager().registerEvents(new PlayerSessionListener(this), this);
        getServer().getPluginManager().registerEvents(new JoinQuitListener(this), this);
    }

    @Override
    public void onDisable() {
        if (this.database != null) {
            this.database.close();
            this.database = null;
        }
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

    public DirectMessageService getDirectMessageService() {
        return this.directMessageService;
    }

    public PlayerPreferenceService getPlayerPreferenceService() {
        return this.playerPreferenceService;
    }

    public ChatGuardService getChatGuardService() {
        return this.chatGuardService;
    }

    public ChatMenuService getChatMenuService() {
        return this.chatMenuService;
    }

    public ChatCommand getChatCommand() {
        return this.chatCommand;
    }

    public MentionService getMentionService() {
        return this.mentionService;
    }

    public ModerationService getModerationService() {
        return this.moderationService;
    }

    public String resolveConditionPlaceholders(Player player, String input) {
        if (input == null) {
            return "";
        }
        String value = input;
        value = value.replace("{player}", player == null ? "" : player.getName());
        value = value.replace("{world}", player == null || player.getWorld() == null ? "" : player.getWorld().getName());
        value = value.replace("{world_name}", player == null || player.getWorld() == null ? "" : player.getWorld().getName());
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                value = PlaceholderAPI.setPlaceholders(player, value);
            } catch (Throwable ignored) {
            }
        }
        return value;
    }

    private void saveResourceIfMissing(String path) {
        File file = new File(getDataFolder(), path);
        if (!file.exists()) {
            saveResource(path, false);
        }
    }

    private void initializeStorage() {
        try {
            this.database = new DatabaseFactory(this).create();
            String tablePrefix = getConfig().getString("database.table-prefix", "sopchat_");
            SqlChannelRepository channelRepository = new SqlChannelRepository(this.database, tablePrefix);
            SqlDirectMessageRepository directMessageRepository = new SqlDirectMessageRepository(this.database, tablePrefix);
            SqlPlayerPreferenceRepository playerPreferenceRepository = new SqlPlayerPreferenceRepository(this.database, tablePrefix);
            channelRepository.initialize();
            directMessageRepository.initialize();
            playerPreferenceRepository.initialize();
            this.channelService = new ChannelService(this, channelRepository);
            this.directMessageService = new DirectMessageService(directMessageRepository);
            this.playerPreferenceService = new PlayerPreferenceService(playerPreferenceRepository);
        } catch (SQLException exception) {
            getLogger().warning("Failed to initialize SopChat SQL storage, falling back to in-memory channels: " + exception.getMessage());
            if (this.database != null) {
                this.database.close();
                this.database = null;
            }
            this.channelService = new ChannelService(this);
            this.directMessageService = new DirectMessageService();
            this.playerPreferenceService = new PlayerPreferenceService();
        }
    }
}
