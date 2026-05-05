package net.enelson.sopchat.storage;

import net.enelson.sopchat.preference.PlayerPreferenceRepository;
import net.enelson.sopli.lib.database.SopDatabase;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class SqlPlayerPreferenceRepository implements PlayerPreferenceRepository {

    private final SopDatabase database;
    private final String tablePrefix;

    public SqlPlayerPreferenceRepository(SopDatabase database, String tablePrefix) {
        this.database = database;
        this.tablePrefix = tablePrefix;
    }

    @Override
    public void initialize() throws SQLException {
        this.database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "social_spy ("
                + "player_uuid VARCHAR(36) PRIMARY KEY,"
                + "enabled BOOLEAN NOT NULL"
                + ")");
        this.database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "ignored_players ("
                + "owner_uuid VARCHAR(36) NOT NULL,"
                + "target_uuid VARCHAR(36) NOT NULL,"
                + "PRIMARY KEY (owner_uuid, target_uuid)"
                + ")");
        this.database.execute("CREATE TABLE IF NOT EXISTS " + tablePrefix + "active_channels ("
                + "player_uuid VARCHAR(36) PRIMARY KEY,"
                + "channel_name VARCHAR(64) NOT NULL"
                + ")");
    }

    @Override
    public void setSocialSpy(String playerUuid, boolean enabled) throws SQLException {
        this.database.transaction(connection -> {
            PreparedStatement delete = null;
            PreparedStatement insert = null;
            try {
                delete = connection.prepareStatement("DELETE FROM " + tablePrefix + "social_spy WHERE player_uuid = ?");
                delete.setString(1, playerUuid);
                delete.executeUpdate();
                insert = connection.prepareStatement("INSERT INTO " + tablePrefix + "social_spy (player_uuid, enabled) VALUES (?, ?)");
                insert.setString(1, playerUuid);
                insert.setBoolean(2, enabled);
                insert.executeUpdate();
                return null;
            } finally {
                closeQuietly(delete);
                closeQuietly(insert);
            }
        });
    }

    @Override
    public boolean isSocialSpyEnabled(String playerUuid) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement("SELECT enabled FROM " + tablePrefix + "social_spy WHERE player_uuid = ?");
                statement.setString(1, playerUuid);
                resultSet = statement.executeQuery();
                return resultSet.next() && resultSet.getBoolean("enabled");
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public List<String> findSocialSpyEnabledPlayers() throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            List<String> result = new ArrayList<String>();
            try {
                statement = connection.prepareStatement("SELECT player_uuid FROM " + tablePrefix + "social_spy WHERE enabled = ?");
                statement.setBoolean(1, true);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    result.add(resultSet.getString("player_uuid"));
                }
                return result;
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public void addIgnore(String ownerUuid, String targetUuid) throws SQLException {
        this.database.transaction(connection -> {
            PreparedStatement delete = null;
            PreparedStatement insert = null;
            try {
                delete = connection.prepareStatement("DELETE FROM " + tablePrefix + "ignored_players WHERE owner_uuid = ? AND target_uuid = ?");
                delete.setString(1, ownerUuid);
                delete.setString(2, targetUuid);
                delete.executeUpdate();
                insert = connection.prepareStatement("INSERT INTO " + tablePrefix + "ignored_players (owner_uuid, target_uuid) VALUES (?, ?)");
                insert.setString(1, ownerUuid);
                insert.setString(2, targetUuid);
                insert.executeUpdate();
                return null;
            } finally {
                closeQuietly(delete);
                closeQuietly(insert);
            }
        });
    }

    @Override
    public void removeIgnore(String ownerUuid, String targetUuid) throws SQLException {
        this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement("DELETE FROM " + tablePrefix + "ignored_players WHERE owner_uuid = ? AND target_uuid = ?");
                statement.setString(1, ownerUuid);
                statement.setString(2, targetUuid);
                statement.executeUpdate();
            } finally {
                closeQuietly(statement);
            }
        });
    }

    @Override
    public boolean isIgnoring(String ownerUuid, String targetUuid) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement("SELECT 1 FROM " + tablePrefix + "ignored_players WHERE owner_uuid = ? AND target_uuid = ?");
                statement.setString(1, ownerUuid);
                statement.setString(2, targetUuid);
                resultSet = statement.executeQuery();
                return resultSet.next();
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public List<String> findIgnoredPlayers(String ownerUuid) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            List<String> result = new ArrayList<String>();
            try {
                statement = connection.prepareStatement("SELECT target_uuid FROM " + tablePrefix + "ignored_players WHERE owner_uuid = ?");
                statement.setString(1, ownerUuid);
                resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    result.add(resultSet.getString("target_uuid"));
                }
                return result;
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    @Override
    public void setActiveChannel(String playerUuid, String channelName) throws SQLException {
        this.database.transaction(connection -> {
            PreparedStatement delete = null;
            PreparedStatement insert = null;
            try {
                delete = connection.prepareStatement("DELETE FROM " + tablePrefix + "active_channels WHERE player_uuid = ?");
                delete.setString(1, playerUuid);
                delete.executeUpdate();
                if (channelName != null && !channelName.trim().isEmpty()) {
                    insert = connection.prepareStatement("INSERT INTO " + tablePrefix + "active_channels (player_uuid, channel_name) VALUES (?, ?)");
                    insert.setString(1, playerUuid);
                    insert.setString(2, channelName);
                    insert.executeUpdate();
                }
                return null;
            } finally {
                closeQuietly(delete);
                closeQuietly(insert);
            }
        });
    }

    @Override
    public String findActiveChannel(String playerUuid) throws SQLException {
        return this.database.withConnection(connection -> {
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                statement = connection.prepareStatement("SELECT channel_name FROM " + tablePrefix + "active_channels WHERE player_uuid = ?");
                statement.setString(1, playerUuid);
                resultSet = statement.executeQuery();
                return resultSet.next() ? resultSet.getString("channel_name") : null;
            } finally {
                closeQuietly(resultSet);
                closeQuietly(statement);
            }
        });
    }

    private void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }
}
