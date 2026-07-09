package sashwind.mc.plugin.ffcraft.model;

public enum PlaybackStatus {
    PLAYING,
    PAUSED,
    STOPPED;

    public static PlaybackStatus fromOrdinal(int ordinal) {
        return values()[ordinal];
    }
}
