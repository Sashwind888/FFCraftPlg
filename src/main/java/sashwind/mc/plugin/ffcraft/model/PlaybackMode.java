package sashwind.mc.plugin.ffcraft.model;

public enum PlaybackMode {
    SEQUENTIAL,
    LOOP_LIST,
    SINGLE_LOOP,
    RANDOM;

    public static PlaybackMode fromOrdinal(int ordinal) {
        return values()[ordinal];
    }
}
