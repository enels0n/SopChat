package net.enelson.sopchat.preference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class InMemoryPlayerPreferenceRepository implements PlayerPreferenceRepository {

    private final Map<String, Boolean> socialSpy = new LinkedHashMap<String, Boolean>();
    private final Map<String, Map<String, Boolean>> ignores = new LinkedHashMap<String, Map<String, Boolean>>();
    private final Map<String, String> activeChannels = new LinkedHashMap<String, String>();

    @Override
    public void initialize() {
    }

    @Override
    public void setSocialSpy(String playerUuid, boolean enabled) {
        this.socialSpy.put(normalize(playerUuid), Boolean.valueOf(enabled));
    }

    @Override
    public boolean isSocialSpyEnabled(String playerUuid) {
        Boolean value = this.socialSpy.get(normalize(playerUuid));
        return value != null && value.booleanValue();
    }

    @Override
    public List<String> findSocialSpyEnabledPlayers() {
        List<String> result = new ArrayList<String>();
        for (Map.Entry<String, Boolean> entry : this.socialSpy.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    @Override
    public void addIgnore(String ownerUuid, String targetUuid) {
        String ownerKey = normalize(ownerUuid);
        Map<String, Boolean> ignored = this.ignores.get(ownerKey);
        if (ignored == null) {
            ignored = new LinkedHashMap<String, Boolean>();
            this.ignores.put(ownerKey, ignored);
        }
        ignored.put(normalize(targetUuid), Boolean.TRUE);
    }

    @Override
    public void removeIgnore(String ownerUuid, String targetUuid) {
        Map<String, Boolean> ignored = this.ignores.get(normalize(ownerUuid));
        if (ignored != null) {
            ignored.remove(normalize(targetUuid));
        }
    }

    @Override
    public boolean isIgnoring(String ownerUuid, String targetUuid) {
        Map<String, Boolean> ignored = this.ignores.get(normalize(ownerUuid));
        return ignored != null && Boolean.TRUE.equals(ignored.get(normalize(targetUuid)));
    }

    @Override
    public List<String> findIgnoredPlayers(String ownerUuid) {
        Map<String, Boolean> ignored = this.ignores.get(normalize(ownerUuid));
        if (ignored == null) {
            return Collections.emptyList();
        }
        return new ArrayList<String>(ignored.keySet());
    }

    @Override
    public void setActiveChannel(String playerUuid, String channelName) {
        String key = normalize(playerUuid);
        if (channelName == null || channelName.trim().isEmpty()) {
            this.activeChannels.remove(key);
            return;
        }
        this.activeChannels.put(key, channelName);
    }

    @Override
    public String findActiveChannel(String playerUuid) {
        return this.activeChannels.get(normalize(playerUuid));
    }

    private String normalize(String uuid) {
        return uuid == null ? "" : uuid.toLowerCase(Locale.ROOT);
    }
}
