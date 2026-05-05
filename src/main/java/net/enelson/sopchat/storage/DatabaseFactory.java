package net.enelson.sopchat.storage;

import net.enelson.sopchat.SopChatPlugin;
import net.enelson.sopli.lib.SopLib;
import net.enelson.sopli.lib.database.DatabaseConfig;
import net.enelson.sopli.lib.database.SopDatabase;

public final class DatabaseFactory {

    private final SopChatPlugin plugin;

    public DatabaseFactory(SopChatPlugin plugin) {
        this.plugin = plugin;
    }

    public SopDatabase create() {
        String type = this.plugin.getConfig().getString("database.type", "sqlite");
        DatabaseConfig config;
        if ("mysql".equalsIgnoreCase(type)) {
            config = DatabaseConfig.mysql(
                            this.plugin.getConfig().getString("database.host", "127.0.0.1"),
                            this.plugin.getConfig().getInt("database.port", 3306),
                            this.plugin.getConfig().getString("database.database", "minecraft")
                    )
                    .credentials(
                            this.plugin.getConfig().getString("database.username", "root"),
                            this.plugin.getConfig().getString("database.password", "")
                    )
                    .poolName(this.plugin.getConfig().getString("database.pool-name", "SopChat") + "-mysql")
                    .maximumPoolSize(this.plugin.getConfig().getInt("database.maximum-pool-size", 10))
                    .minimumIdle(this.plugin.getConfig().getInt("database.minimum-idle", 2))
                    .connectionTimeout(this.plugin.getConfig().getLong("database.connection-timeout", 30000L))
                    .idleTimeout(this.plugin.getConfig().getLong("database.idle-timeout", 600000L))
                    .maxLifetime(this.plugin.getConfig().getLong("database.max-lifetime", 1800000L))
                    .build();
        } else {
            String path = new java.io.File(this.plugin.getDataFolder(), this.plugin.getConfig().getString("database.sqlite-file", "chat.db"))
                    .getAbsolutePath()
                    .replace("\\", "/");
            config = DatabaseConfig.builder("jdbc:sqlite:" + path)
                    .poolName(this.plugin.getConfig().getString("database.pool-name", "SopChat") + "-sqlite")
                    .maximumPoolSize(1)
                    .minimumIdle(1)
                    .connectionTimeout(this.plugin.getConfig().getLong("database.connection-timeout", 30000L))
                    .idleTimeout(this.plugin.getConfig().getLong("database.idle-timeout", 600000L))
                    .maxLifetime(this.plugin.getConfig().getLong("database.max-lifetime", 1800000L))
                    .build();
        }
        return SopLib.getInstance().getDatabaseService().createDatabase(config);
    }
}
