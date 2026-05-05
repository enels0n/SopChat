package net.enelson.sopchat.preference;

import java.sql.SQLException;
import java.util.List;

public interface PlayerPreferenceRepository {

    void initialize() throws SQLException;

    void setSocialSpy(String playerUuid, boolean enabled) throws SQLException;

    boolean isSocialSpyEnabled(String playerUuid) throws SQLException;

    List<String> findSocialSpyEnabledPlayers() throws SQLException;

    void addIgnore(String ownerUuid, String targetUuid) throws SQLException;

    void removeIgnore(String ownerUuid, String targetUuid) throws SQLException;

    boolean isIgnoring(String ownerUuid, String targetUuid) throws SQLException;

    List<String> findIgnoredPlayers(String ownerUuid) throws SQLException;

    void setActiveChannel(String playerUuid, String channelName) throws SQLException;

    String findActiveChannel(String playerUuid) throws SQLException;
}
