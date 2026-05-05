package net.enelson.sopchat.moderation;

public final class ModerationService {

    private boolean globalMute;
    private int slowmodeSeconds;

    public boolean toggleGlobalMute() {
        this.globalMute = !this.globalMute;
        return this.globalMute;
    }

    public boolean isGlobalMute() {
        return this.globalMute;
    }

    public void setSlowmodeSeconds(int slowmodeSeconds) {
        this.slowmodeSeconds = Math.max(0, slowmodeSeconds);
    }

    public int getSlowmodeSeconds() {
        return this.slowmodeSeconds;
    }
}
